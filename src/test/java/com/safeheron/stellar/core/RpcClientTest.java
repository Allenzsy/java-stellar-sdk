package com.safeheron.stellar.core;

import com.safeheron.stellar.entity.*;
import org.junit.Assert;
import org.junit.Test;
import org.stellar.sdk.*;
import org.stellar.sdk.exception.AccountNotFoundException;
import org.stellar.sdk.operations.InvokeHostFunctionOperation;
import org.stellar.sdk.operations.Operation;
import org.stellar.sdk.requests.sorobanrpc.GetTransactionsRequest;
import org.stellar.sdk.scval.Scv;
import org.stellar.sdk.xdr.SCVal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Allenzsy
 * @Date 2025/7/22 21:22
 * @Description:
 */
public class RpcClientTest {

    @Test
    public void test_validAddressOnChain() {
        String heyBytes = "3a5c1615c31b1c129a9bb594f68ffe15cdaa9de52f4f379e8cb5928e58cbdb4a";
        String stellarAddress = "GD6VYNE4NY5RHWQ7NHYPQC4CPSUDSYKVOI5A3NVUF5ZTLJ7MAEHNBDSJ";
        PublicKeyIdentifier publicKeyIdentifier = new PublicKeyIdentifier(heyBytes, PublicKeyIdentifier.CurveType.PUBLIC_KEY_TYPE_ED25519);
        List<AddressIdentifier> addressIdentifiers = AddressIdentifier.derive(publicKeyIdentifier);
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        try (RpcClient server = new RpcClient(sorobanTestUri)) {
            AddressVerify addressVerify = server.validAddressOnChain(addressIdentifiers.get(0));
            Assert.assertTrue(addressVerify.isFormatCorrect());
            Assert.assertTrue(addressVerify.isAccountAddress());
            Assert.assertFalse(addressVerify.isContractAddress());
            Assert.assertFalse(addressVerify.isExistOnChain());
            addressVerify = server.validAddressOnChain(addressIdentifiers.get(1));
            Assert.assertTrue(addressVerify.isFormatCorrect());
            Assert.assertTrue(addressVerify.isContractAddress());
            Assert.assertFalse(addressVerify.isAccountAddress());
            Assert.assertFalse(addressVerify.isExistOnChain());
            // 格式不正确
            AddressIdentifier identifier = new AddressIdentifier(heyBytes.substring(10), stellarAddress.substring(10), Address.AddressType.ACCOUNT);
            addressVerify = server.validAddressOnChain(identifier);
            Assert.assertFalse(addressVerify.isFormatCorrect());
            Assert.assertFalse(addressVerify.isContractAddress());
            Assert.assertFalse(addressVerify.isAccountAddress());
            Assert.assertFalse(addressVerify.isExistOnChain());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getBalances() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        try (RpcClient server = new RpcClient(sorobanTestUri)) {
            String accountId = "GATFC6P337LCDOPQWNVEUHGG25B6UQX6PWOYZP4UED6WFQLDFUNQ4LYZ";
            String assetCode = "USDC";
            // testnet issuer
            String assetIssuer  = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
            AssetTypeCreditAlphaNum4 usdc = new AssetTypeCreditAlphaNum4(assetCode, assetIssuer);
            AssetTypeNative xlm =new AssetTypeNative();
            ArrayList<Currency> currencies = new ArrayList<>();
            currencies.add(new Currency(usdc));
            currencies.add(new Currency(xlm));
            List<Balance> balances = server.getBalances(accountId, currencies);
            Assert.assertNotNull(balances);
            System.out.println(balances);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void test_getNetworkFee() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        try (RpcClient server = new RpcClient(sorobanTestUri)) {
            // 对于非合约调用的交易
            Fee fee = server.getNetworkFee(2);
            Assert.assertEquals(200L, fee.getTotalFee().longValue());

            // 对于调用合约的交易
            KeyPair source = KeyPair.fromSecretSeed("SBXVE62FBBXFUV3QXLJVUZDI4TW6YHWHEXBSJK5HW54UW5HA2IVC4VWA"); // 发起合约调用的账户, 需要用私钥签名
            String contractAddress = "CBUKCX5U2YUBUVUVOCYUWMPVR6AV72FK73ZQFMHAUI6NUVJEH5MELSQP"; // 合约地址
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
            fee = server.getNetworkFee(transaction);
            Assert.assertEquals(50850L, fee.getTotalFee().longValue());
            System.out.printf("手续费: %d%n", fee.getTotalFee());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void test_getLatestBlock() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        try (RpcClient server = new RpcClient(sorobanTestUri)) {
            BlockHeader latestBlock = server.getLatestBlock();
            Assert.assertNotNull(latestBlock);
            Assert.assertNotEquals( "1753705636", latestBlock.getTimeStamp());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getTransctionsByblock() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        String blockHash = "5244913d1e4700d8327d32de23fa62a200cf5cd9a19bb7d9d4b27f7ed4bbd22c";
        String parenBlockHash = "a43e09df83efc84ed2421b541d7807eb672703a2c0865474051b12626ca7f389";
        try (RpcClient server = new RpcClient(sorobanTestUri)) {
            BlockHeader latestBlock = server.getLatestBlock();
            BlockTxnsVO blockTxnsVO = server.getTransctionsByblock(latestBlock.getHeight(),
                    GetTransactionsRequest.PaginationOptions.builder().limit(0L).build(), Network.TESTNET);
            Assert.assertNotNull(blockTxnsVO);
            Assert.assertNotEquals(blockHash, blockTxnsVO.getBlockHash());
            Assert.assertNotEquals(parenBlockHash, blockTxnsVO.getParetHash());
            System.out.printf("交易数量: %d%n", blockTxnsVO.getTransactions().size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
