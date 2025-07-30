package com.safeheron.stellar.core;

import com.safeheron.stellar.entity.BlockHeader;
import com.safeheron.stellar.entity.BlockTxnsVO;
import com.safeheron.stellar.entity.TransactionVO;
import org.junit.Assert;
import org.junit.Test;
import org.stellar.sdk.Network;
import org.stellar.sdk.requests.sorobanrpc.GetTransactionsRequest;

/**
 * @Author Allenzsy
 * @Date 2025/7/22 21:22
 * @Description:
 */
public class RpcClientTest {


    @Test
    public void test_getLatestBlock() {
        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        final RpcClient server = new RpcClient(sorobanTestUri);

        final BlockHeader latestBlock = server.getLatestBlock();
        System.out.println(latestBlock);
        Assert.assertNotNull(latestBlock);
        Assert.assertNotEquals(latestBlock.getTimeStamp(), "1753705636");
    }

    @Test
    public void test_getTransctionsByblock() {
        Long height = 686786L;
        String blockHash = "5244913d1e4700d8327d32de23fa62a200cf5cd9a19bb7d9d4b27f7ed4bbd22c";
        Long timeStamp = 1753705636L;
        String parenBlockHash = "a43e09df83efc84ed2421b541d7807eb672703a2c0865474051b12626ca7f389";

        String sorobanTestUri = "https://soroban-testnet.stellar.org";
        final RpcClient server = new RpcClient(sorobanTestUri);

        final BlockTxnsVO blockTxnsVO = server.getTransctionsByblock(height.toString(),
                GetTransactionsRequest.PaginationOptions.builder().limit(0L).build(), Network.TESTNET);
        Assert.assertEquals(blockHash, blockTxnsVO.getBlockHash());
    }

}
