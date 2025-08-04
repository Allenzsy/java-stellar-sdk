package com.safeheron.stellar.entity;

import com.safeheron.stellar.core.RpcClient;
import com.safeheron.stellar.util.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.stellar.sdk.*;
import org.stellar.sdk.exception.AccountNotFoundException;
import org.stellar.sdk.operations.InvokeHostFunctionOperation;
import org.stellar.sdk.operations.Operation;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.scval.Scv;
import org.stellar.sdk.xdr.SCVal;

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
                .amount(new BigDecimal("0.0000001")) // 1.0000000 XLM
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
            String onLineTxHash = server.submit(signedTransaction.getSignedTransaction());
            // 验证
            Assert.assertEquals(offLineTxHash, onLineTxHash);
            TransactionVO receiptVO = server.getTransactionReceipt(onLineTxHash, Network.TESTNET);
            Assert.assertNotNull(receiptVO);
            System.out.println(receiptVO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getTransaction() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        final RpcClient server = new RpcClient(sorobanTestUri);
        String txNotFound = "97b8e82bc643d2475abbbc50afa558878a24e28ff21cb3c79c4b64d6e504e214";
        TransactionVO receiptVO = server.getTransactionReceipt(txNotFound, Network.TESTNET);
        Assert.assertEquals(TransactionVO.TransactionStatus.NOT_FOUND, receiptVO.getStatus());
        System.out.println(receiptVO);
    }

    @Test
    public void test_send_sorobanTransaction() throws Exception{
        // 发起合约调用的账户, 需要用私钥签名
        KeyPair source = KeyPair.fromSecretSeed("SBXVE62FBBXFUV3QXLJVUZDI4TW6YHWHEXBSJK5HW54UW5HA2IVC4VWA");
        // 合约地址
        String contractAddress = "CBUKCX5U2YUBUVUVOCYUWMPVR6AV72FK73ZQFMHAUI6NUVJEH5MELSQP";
        try (
            // stellar-rpc
            RpcClient server = new RpcClient("https://soroban-testnet.stellar.org");
        ) {

            TransactionBuilderAccount sourceAccount = null;
            try {
                sourceAccount = server.getAccount(source.getAccountId());
            } catch (AccountNotFoundException e) {
                throw new RuntimeException("Account not found, please activate it first");
            }

            // 创建合约调用的操作
            List<SCVal> contractArgs = new ArrayList<SCVal>();
            contractArgs.add(Scv.toString("ZSY"));
            InvokeHostFunctionOperation operation =
                    InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                                    contractAddress, "hello", contractArgs)
                            .build();
            // 创建预条件,
            TransactionPreconditions preconditions = TransactionPreconditions.builder().timeBounds(new TimeBounds(0, 0)).build();
            // 创建交易对象
            Transaction transaction = new Transaction(source.getAccountId(),
                    Transaction.MIN_BASE_FEE * 1,
                    sourceAccount.getIncrementedSequenceNumber(),
                    new Operation[]{operation},
                    null,
                    preconditions,
                    null,
                    Network.TESTNET);
            sourceAccount.incrementSequenceNumber();

            transaction = server.prepareTransaction(transaction);

            System.out.printf("手续费: %d%n", transaction.getFee());

            try {
                // 获取待签名交易, 包括未签名的交易序列化, 未签名的交易hash等
                NeedSignTransactionDTO needSignTransactionDTO = TransactionUtil.getUnsignedTransaction(
                        transaction,
                        Stream.of(Util.bytesToHex(source.getPublicKey())).collect(Collectors.toList()));

                // 模拟签名过程
                String unsignedTransaction = needSignTransactionDTO.getUnsignedTransaction();
                List<NeedSignSignatureDTO> needSignSignatures = needSignTransactionDTO.getUnsignedSignatures();
                List<SignedSignatureDTO> signedSignatures = new ArrayList<SignedSignatureDTO>();
                for (NeedSignSignatureDTO e : needSignSignatures) {
                    // 签名交易 hash
                    byte[] signedTxHash = source.sign(Util.hexToBytes(e.getTxHash()));
                    signedSignatures.add(new SignedSignatureDTO(Util.bytesToHex(signedTxHash), e.getAddress()));
                }

                SignedTransactionDTO signedTransaction = TransactionUtil.getSignedTransaction(unsignedTransaction, signedSignatures, Network.TESTNET);
                String offLineTxHash = signedTransaction.getTxHash();
                String onLineTxHash = server.submit(signedTransaction.getSignedTransaction());
                // 验证
                Assert.assertEquals(offLineTxHash, onLineTxHash);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
