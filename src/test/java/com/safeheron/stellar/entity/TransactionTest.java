package com.safeheron.stellar.entity;

import com.safeheron.stellar.core.RpcClient;
import com.safeheron.stellar.util.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.stellar.sdk.*;
import org.stellar.sdk.operations.Operation;
import org.stellar.sdk.operations.PaymentOperation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author zsy
 * @Date 2025/7/3 16:03
 * @Description: 构造交易测试
 */
public class TransactionTest {

    @Test
    public void test_send_transaction(){
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        final RpcClient server = new RpcClient(sorobanTestUri);

        KeyPair source = KeyPair.fromSecretSeed("SBXVE62FBBXFUV3QXLJVUZDI4TW6YHWHEXBSJK5HW54UW5HA2IVC4VWA");
        // GD6VYNE4NY5RHWQ7NHYPQC4CPSUDSYKVOI5A3NVUF5ZTLJ7MAEHNBDSJ
        KeyPair target = KeyPair.fromSecretSeed("SDUDSUG7ZJQVK4A5ULXIHXWSYS3IDNTKHPLFMBCXJDRQOW2YIZ44AWRV");

        TransactionBuilderAccount account = server.getAccount(source.getAccountId());

        // 创建转账操作
        PaymentOperation paymentOperation = PaymentOperation
                .builder()
                .destination(target.getAccountId())
                .asset(new AssetTypeNative())
                .amount(BigDecimal.valueOf(3)) // 1.0000000
                .build();
        // 创建预条件,
        TransactionPreconditions preconditions = TransactionPreconditions.builder().timeBounds(new TimeBounds(0, 0)).build();
        // 创建交易对象
        Transaction transaction = new Transaction(source.getAccountId(),
                Transaction.MIN_BASE_FEE * 1,
                account.getIncrementedSequenceNumber(),
                new Operation[]{paymentOperation},
                null,
                preconditions,
                null,
                Network.TESTNET);
        account.incrementSequenceNumber();

        try {
            // 获取待签名交易, 包括未签名的交易序列化, 未签名的交易hash等
            NeedSignTransactionDTO needSignTransactionDTO = TransactionUtil.getUnsignedTransaction(
                    transaction,
                    Stream.of(Util.bytesToHex(source.getPublicKey())).collect(Collectors.toList()));

            // 模拟签名过程
            String unsignedTransaction = needSignTransactionDTO.getUnsignedTransaction();
            List<NeedSignSignatureDTO> needSignSignatures = needSignTransactionDTO.getUnsignedSignatures();
            List<SignedSignatureDTO> list = new ArrayList<SignedSignatureDTO>();
            for (NeedSignSignatureDTO e : needSignSignatures) {
                // 签名交易 hash
                byte[] signedTxHash = source.sign(Util.hexToBytes(e.getTxHash()));
                list.add(new SignedSignatureDTO(Util.bytesToHex(signedTxHash), e.getAddress()));
            }

            SignedTransactionDTO signedTransaction = TransactionUtil.getSignedTransaction(unsignedTransaction, list, Network.TESTNET);
            String offLineTxHash = signedTransaction.getTxHash();
            String onLineTxHash = server.sendTransaction(signedTransaction.getSignedTransaction());
            // 验证
            Assert.assertEquals(offLineTxHash, onLineTxHash);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        // account1.
        //
        // // 构造交易
        // TransactionBuilder transactionBuilder = new TransactionBuilder(account, Network.TESTNET);
        // Transaction transaction = transactionBuilder
        //         .addOperation(PaymentOperation
        //                 .builder()
        //                 .destination(target.getAccountId())
        //                 .asset(new AssetTypeNative())
        //                 .amount(BigDecimal.valueOf(3))
        //                 .build())
        //         .setBaseFee(Transaction.MIN_BASE_FEE)
        //         .setTimeout(TransactionPreconditions.TIMEOUT_INFINITE)
        //         .build();
        // // 获取未签名交易的序列化，HEX格式
        // String needToSignHex = transaction.hashHex();
        // System.out.println("未签名交易的序列化：" + needToSignHex);
        // // 交易签名
        // // transaction.sign(source); 此方法只适用于能直接拿到私钥的场景
        // // 实际项目需要将 needToSignHex 送给签名服务器，完成签名，然后转为 org.stellar.sdk.xdr.Signature ---> DecoratedSignature 类型
        // DecoratedSignature decoratedSignature = source.signDecorated(Util.hexToBytes(needToSignHex));
        // transaction.addSignature(decoratedSignature);
        // // 提交上链
        // SendTransactionResponse transactionResponse = server.sendTransaction(transaction);
        //
        // // SendTransactionResponse(status=PENDING, errorResultXdr=null, diagnosticEventsXdr=null, hash=9d53276a790de874cb62da5c1d532d262c335240c14803c475f6d74e8fdf3a06, latestLedger=325735, latestLedgerCloseTime=1751897592)
        // // 返回的 txHash
        // final String hash = transactionResponse.getHash();
        // System.out.println(transactionResponse);


    }

    @Test
    public void test_getTransaction() {
        String txHash = "97b8e82bc643d2475abbbc50afa558878a24e28ff21cb3c79c4b64d6e504e214";
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        final RpcClient server = new RpcClient(sorobanTestUri);

        TransactionVO receiptVO = server.getTransactionReceipt(txHash, Network.TESTNET);
        System.out.println(receiptVO);
    }
}
