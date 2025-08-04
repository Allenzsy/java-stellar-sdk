package com.safeheron.stellar.util;

import com.safeheron.stellar.entity.*;
import org.stellar.sdk.AbstractTransaction;
import org.stellar.sdk.Network;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.Util;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionsResponse;
import org.stellar.sdk.xdr.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Allenzsy
 * @Date 2025/7/23 1:52
 * @Description: 交易相关工具类
 */
public class TransactionUtil {

    /**
     * 获取未签名交易
     * @param publicKeys 签名signer对应的公钥，Hex格式，若涉及多签则list中存入多个
     * @return 未签名交易的序列化字符串, 待签名的txHash字符串, 签名signer对应的公钥
     * @throws IOException
     */
    public static NeedSignTransactionDTO getUnsignedTransaction(Transaction transaction, List<String> publicKeys) throws IOException {
        byte[] xdrByteArray = transaction.toEnvelopeXdr().toXdrByteArray();
        List<NeedSignSignatureDTO> unsignedSigs = publicKeys.stream()
                .map(e -> new NeedSignSignatureDTO(transaction.hashHex(), e))
                .collect(Collectors.toList());
        return new NeedSignTransactionDTO(org.stellar.sdk.Util.bytesToHex(xdrByteArray), unsignedSigs);
    }

    /**
     *
     * @param unsignedTransaction 未签名交易序列化, Hex格式
     * @param signedSigDTOs 已签名的txHash和对应的公钥
     * @param network 测试网或公网
     * @return SignedTransactionDTO 包含离线计算的txHash和可提交上链的已签名序列化交易
     * @throws IOException
     */
    public static SignedTransactionDTO getSignedTransaction(String unsignedTransaction, List<SignedSignatureDTO> signedSigDTOs, Network network) throws IOException {
        TransactionEnvelope transactionEnvelope = null;
        try {
            transactionEnvelope = TransactionEnvelope.fromXdrByteArray(org.stellar.sdk.Util.hexToBytes(unsignedTransaction));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        // 还原Transaction对象
        Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(transactionEnvelope, network);
        // 添加已签名txHash
        for (SignedSignatureDTO signedSig : signedSigDTOs) {
            byte[] signatureBytes = org.stellar.sdk.Util.hexToBytes(signedSig.getSignedTxHash()); // 外部签名结果
            byte[] signerPublicKey = Util.hexToBytes(signedSig.getPublickKey()); // 32字节的 Ed25519 公钥

            // 创建 Signature 对象
            Signature signature = new Signature();
            signature.setSignature(signatureBytes);

            // 生成 SignatureHint（signer 公钥的后 4 个字节）
            byte[] hintBytes = Arrays.copyOfRange(signerPublicKey, signerPublicKey.length - 4, signerPublicKey.length);
            SignatureHint signatureHint = new SignatureHint();
            signatureHint.setSignatureHint(hintBytes);

            // 构造 DecoratedSignature
            DecoratedSignature decoratedSignature = new DecoratedSignature();
            decoratedSignature.setHint(signatureHint);
            decoratedSignature.setSignature(signature);

            // 添加到交易
            transaction.addSignature(decoratedSignature);
        }
        SignedTransactionDTO signedTransactionDTO = new SignedTransactionDTO();
        signedTransactionDTO.setTxHash(transaction.hashHex());
        signedTransactionDTO.setSignedTransaction(transaction.toEnvelopeXdrBase64());
        return signedTransactionDTO;
    }


    public static TransactionVO parseFromResponse(GetTransactionResponse response, Network network) {
        final TransactionVO txVO = new TransactionVO();
        txVO.setStatus(TransactionVO.TransactionStatus.valueOf(response.getStatus().name()));
        txVO.setTxHash(response.getTxHash());
        txVO.setLatestLedger(response.getLatestLedger());
        txVO.setLatestLedgerCloseTime(response.getLatestLedgerCloseTime());
        txVO.setOldestLedger(response.getOldestLedger());
        txVO.setOldestLedgerCloseTime(response.getOldestLedgerCloseTime());
        txVO.setApplicationOrder(response.getApplicationOrder());
        txVO.setFeeBump(response.getFeeBump());
        txVO.setEnvelopeXdr(response.getEnvelopeXdr());
        txVO.setResultXdr(response.getResultXdr());
        txVO.setResultMetaXdr(response.getResultMetaXdr());
        txVO.setLedger(response.getLedger());
        txVO.setCreatedAt(response.getCreatedAt());
        // 如果不存在交易, 无需继续解析交易相关信息
        if (txVO.getStatus() == TransactionVO.TransactionStatus.NOT_FOUND) {
            return txVO;
        }

        try {
            TransactionResult transactionResult = TransactionResult.fromXdrBase64(txVO.getResultXdr());
            txVO.setGasFee(transactionResult.getFeeCharged().getInt64());
            txVO.setChainMsg(transactionResult.getResult().toString());

            Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(response.getEnvelopeXdr(), network);
            txVO.setFromAddress(transaction.getSourceAccount());
            List<PaymentOperation> paymentOperations = Stream.of(transaction.getOperations())
                    .filter(PaymentOperation.class::isInstance)
                    .map(PaymentOperation.class::cast)
                    .collect(Collectors.toList());
            txVO.setPayments(paymentOperations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return txVO;
    }

    public static List<TransactionVO> parseFromResponse(GetTransactionsResponse response, Network network) {
        List<TransactionVO> transactionVOs = new ArrayList<>();
        final List<GetTransactionsResponse.Transaction> transactionList = response.getTransactions();
        for (GetTransactionsResponse.Transaction tx : transactionList) {
            final TransactionVO txVO = new TransactionVO();
            txVO.setStatus(TransactionVO.TransactionStatus.valueOf(tx.getStatus().name()));
            txVO.setTxHash(tx.getTxHash());
            txVO.setLatestLedger(response.getLatestLedger());
            txVO.setLatestLedgerCloseTime(response.getLatestLedgerCloseTimestamp());
            txVO.setOldestLedger(response.getOldestLedger());
            txVO.setOldestLedgerCloseTime(response.getOldestLedgerCloseTimestamp());
            txVO.setApplicationOrder(tx.getApplicationOrder());
            txVO.setFeeBump(tx.getFeeBump());
            txVO.setEnvelopeXdr(tx.getEnvelopeXdr());
            txVO.setResultXdr(tx.getResultXdr());
            txVO.setResultMetaXdr(tx.getResultMetaXdr());
            txVO.setLedger(tx.getLedger());
            txVO.setCreatedAt(tx.getCreatedAt());
            try {
                TransactionResult transactionResult = TransactionResult.fromXdrBase64(txVO.getResultXdr());
                txVO.setGasFee(transactionResult.getFeeCharged().getInt64());
                txVO.setChainMsg(transactionResult.getResult().toString());
                Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(tx.getEnvelopeXdr(), network);
                txVO.setFromAddress(transaction.getSourceAccount());
                List<PaymentOperation> paymentOperations = Stream.of(transaction.getOperations())
                        .filter(PaymentOperation.class::isInstance)
                        .map(PaymentOperation.class::cast)
                        .collect(Collectors.toList());
                txVO.setPayments(paymentOperations);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            transactionVOs.add(txVO);
        }

        return transactionVOs;
    }

}
