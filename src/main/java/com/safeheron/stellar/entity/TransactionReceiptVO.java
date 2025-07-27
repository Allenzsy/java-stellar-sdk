package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.stellar.sdk.AbstractTransaction;
import org.stellar.sdk.Network;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.operations.Operation;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionResponse;
import org.stellar.sdk.responses.sorobanrpc.GetTransactionsResponse;
import org.stellar.sdk.xdr.TransactionResult;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Allenzsy
 * @Date 2025/7/26 17:27
 * @Description:
 */
@Getter
@Setter
public class TransactionReceiptVO {


    /**
     * 交易状态
     * ref: @see <a href="https://developers.stellar.org/docs/data/rpc/api-reference/methods/getTransaction" target="_blank">getTransaction documentation</a>
     */
    TransactionStatus status;

    /** 交易 Hash */
    String txHash;

    /** Stellar RPC 处理本次请求时的最新账本序号 */
    Long latestLedger;

    /** Stellar RPC 处理本次请求时的最新账本的关闭时间 unix timestamp */
    Long latestLedgerCloseTime;

    /** Stellar RPC 处理本次请求时的最早账本序号 */
    Long oldestLedger;

    /** Stellar RPC 处理本次请求时的最早账本的关闭时间 unix timestamp */
    Long oldestLedgerCloseTime;

    /**
     * 只有 status 为 SUCCESS 或 FAILED 时才有值
     * 该交易在账本所有交易中的索引
     */
    Integer applicationOrder;

    /** 交易手续费是否 fee bumped */
    Boolean feeBump;

    /**
     * base64 编码的已序列化交易
     * The field can be parsed as {@link org.stellar.sdk.xdr.TransactionEnvelope} object.
     */
    String envelopeXdr;

    /**
     * 只有 status 为 SUCCESS 或 FAILED 时才有值
     * The field can be parsed as {@link org.stellar.sdk.xdr.TransactionResult} object.
     */
    String resultXdr;

    /** The field can be parsed as {@link org.stellar.sdk.xdr.TransactionMeta} object. */
    String resultMetaXdr;

    /** 此交易所在的账本序号 */
    Long ledger;

    /** 此交易被写入账本的时间 unix timestamp */
    Long createdAt;

    /** 手续费 */
    Long gasFee;

    /** 链上执行失败的错误信息, 从 resultXdr 解析得到*/
    String chainMsg;

    /** 解析出 from 地址 */
    String fromAddress;

    /** 如果包含 payment 操作, 解析出 PaymentOperation 包含目的地址, 交易币种和金额 */
    List<PaymentOperation> payments;

    ///** 解析出 to 地址 */
    //String toAddress;
    ///** 金额，最小单位 */
    //String value;
    ///**  批量转账相同txHash 有不一样的index */
    //String transactionIdex;

    public enum TransactionStatus {
        NOT_FOUND,
        SUCCESS,
        FAILED
    }

    public static TransactionReceiptVO parseFromResponse(GetTransactionResponse response, Network network) {
        final TransactionReceiptVO receiptVO = new TransactionReceiptVO();
        receiptVO.setStatus(TransactionStatus.valueOf(response.getStatus().name()));
        receiptVO.setTxHash(response.getTxHash());
        receiptVO.setLatestLedger(response.getLatestLedger());
        receiptVO.setLatestLedgerCloseTime(response.getLatestLedgerCloseTime());
        receiptVO.setOldestLedger(response.getOldestLedger());
        receiptVO.setOldestLedgerCloseTime(response.getOldestLedgerCloseTime());
        receiptVO.setApplicationOrder(response.getApplicationOrder());
        receiptVO.setFeeBump(response.getFeeBump());
        receiptVO.setEnvelopeXdr(response.getEnvelopeXdr());
        receiptVO.setResultXdr(response.getResultXdr());
        receiptVO.setResultMetaXdr(response.getResultMetaXdr());
        receiptVO.setLedger(response.getLedger());
        receiptVO.setCreatedAt(response.getCreatedAt());
        // 如果不存在交易, 无需继续解析交易相关信息
        if (receiptVO.getStatus() == TransactionStatus.NOT_FOUND) {
            return receiptVO;
        }

        try {
            TransactionResult transactionResult = TransactionResult.fromXdrBase64(receiptVO.getResultXdr());
            receiptVO.setGasFee(transactionResult.getFeeCharged().getInt64());
            receiptVO.setChainMsg(transactionResult.getResult().toString());

            Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(response.getEnvelopeXdr(), network);
            receiptVO.setFromAddress(transaction.getSourceAccount());
            List<PaymentOperation> paymentOperations = Stream.of(transaction.getOperations())
                    .filter(PaymentOperation.class::isInstance)
                    .map(PaymentOperation.class::cast)
                    .collect(Collectors.toList());
            receiptVO.setPayments(paymentOperations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return receiptVO;
    }

    public static TransactionReceiptVO parseFromTx(GetTransactionsResponse.Transaction tx, Network network) {
        final TransactionReceiptVO receiptVO = new TransactionReceiptVO();
        receiptVO.setStatus(TransactionStatus.valueOf(tx.getStatus().name()));
        receiptVO.setTxHash(tx.getTxHash());
        receiptVO.setLatestLedger(tx.getLatestLedger());
        receiptVO.setLatestLedgerCloseTime(tx.getLatestLedgerCloseTime());
        receiptVO.setOldestLedger(tx.getOldestLedger());
        receiptVO.setOldestLedgerCloseTime(tx.getOldestLedgerCloseTime());
        receiptVO.setApplicationOrder(tx.getApplicationOrder());
        receiptVO.setFeeBump(tx.getFeeBump());
        receiptVO.setEnvelopeXdr(tx.getEnvelopeXdr());
        receiptVO.setResultXdr(tx.getResultXdr());
        receiptVO.setResultMetaXdr(tx.getResultMetaXdr());
        receiptVO.setLedger(tx.getLedger());
        receiptVO.setCreatedAt(tx.getCreatedAt());
        // 如果不存在交易, 无需继续解析交易相关信息
        if (receiptVO.getStatus() == TransactionStatus.NOT_FOUND) {
            return receiptVO;
        }

        try {
            TransactionResult transactionResult = TransactionResult.fromXdrBase64(receiptVO.getResultXdr());
            receiptVO.setGasFee(transactionResult.getFeeCharged().getInt64());
            receiptVO.setChainMsg(transactionResult.getResult().toString());

            Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(response.getEnvelopeXdr(), network);
            receiptVO.setFromAddress(transaction.getSourceAccount());
            List<PaymentOperation> paymentOperations = Stream.of(transaction.getOperations())
                    .filter(PaymentOperation.class::isInstance)
                    .map(PaymentOperation.class::cast)
                    .collect(Collectors.toList());
            receiptVO.setPayments(paymentOperations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return receiptVO;
    }
}
