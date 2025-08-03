package com.safeheron.stellar.core;

import com.google.gson.reflect.TypeToken;
import com.safeheron.stellar.entity.*;
import com.safeheron.stellar.util.TransactionUtil;
import okhttp3.OkHttpClient;
import org.stellar.sdk.*;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.exception.AccountNotFoundException;
import org.stellar.sdk.exception.ConnectionErrorException;
import org.stellar.sdk.exception.RequestTimeoutException;
import org.stellar.sdk.exception.SorobanRpcException;
import org.stellar.sdk.requests.sorobanrpc.GetLedgersRequest;
import org.stellar.sdk.requests.sorobanrpc.GetTransactionRequest;
import org.stellar.sdk.requests.sorobanrpc.GetTransactionsRequest;
import org.stellar.sdk.requests.sorobanrpc.SendTransactionRequest;
import org.stellar.sdk.responses.sorobanrpc.*;
import org.stellar.sdk.xdr.*;
import org.stellar.sdk.xdr.TrustLineAsset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @Author Allenzsy
 * @Date 2025/7/10 0:18
 * @Description: 链接 Stellar RPC-API 的客户端
 */
public class RpcClient extends SorobanServer {


    public RpcClient(String serverURI) {
        super(serverURI);
    }

    public RpcClient(String serverURI, OkHttpClient httpClient) {
        super(serverURI, httpClient);
    }

    /**
     * 链上验证地址是否存在
     * @param address 地址
     * @return 链上验证地址的结果
     */
    public AddressVerify validAddressOnChain(AddressIdentifier address) {
        AddressVerify addressFormat = AddressIdentifier.validAddressFormat(address);
        if (!addressFormat.isFormatCorrect()) {
            return addressFormat;
        }
        boolean isExistOnChain;
        if (addressFormat.isAccountAddress()) {
            try {
                TransactionBuilderAccount account = this.getAccount(address.getStellarAddress());
                isExistOnChain = account != null && address.getStellarAddress().equals(account.getAccountId());
            } catch (AccountNotFoundException ex) {
                isExistOnChain = false;
            }
        } else {
            SCVal scVal = new SCVal();
            scVal.setDiscriminant(SCValType.SCV_LEDGER_KEY_CONTRACT_INSTANCE);
            final Optional<ContractDataEntry> contractData =
                    this.getContractDataEntry(address.getStellarAddress(), scVal, Durability.PERSISTENT);
            isExistOnChain = contractData.isPresent();
        }
        addressFormat.setExistOnChain(isExistOnChain);
        return addressFormat;
    }

    /**
     * 获取某一账户指定币种的余额
     * @param accountAddress 账户地址
     * @param currencies 指定的币种
     * @return 返回某一账户指定币种的余额
     */
    public List<Balance> getBalances(String accountAddress, List<Currency> currencies) {
        Collections.sort(currencies);
        ArrayList<LedgerKey> ledgerKeys = new ArrayList<>();
        ArrayList<Balance> balances = new ArrayList<>();
        AccountID accountId = KeyPair.fromAccountId(accountAddress).getXdrAccountId();
        for (Currency currency : currencies) {
            // 查询 Native 币的余额必须通过查询账户信息获取
            if (currency.getAsset().getType() == AssetType.ASSET_TYPE_NATIVE) {
                LedgerKey.LedgerKeyAccount ledgerKeyAccount = LedgerKey.LedgerKeyAccount.builder()
                        .accountID(StrKey.encodeToXDRAccountId(accountAddress))
                        .build();
                // 创建查询账户需要的 LedgerKey
                LedgerKey ledgerKey = LedgerKey.builder()
                        .account(ledgerKeyAccount)
                        .discriminant(LedgerEntryType.ACCOUNT)
                        .build();
                ledgerKeys.add(ledgerKey);
                continue;
            }
            // 查询 Token 币余额需要给定 code 和 issuer
            final org.stellar.sdk.TrustLineAsset asset = new org.stellar.sdk.TrustLineAsset(currency.getAsset());
            LedgerKey.LedgerKeyTrustLine ledgerKeyTrustLine = LedgerKey.LedgerKeyTrustLine.builder()
                    .accountID(accountId)
                    .asset(asset.toXdr())
                    .build();
            LedgerKey ledgerKey = LedgerKey.builder()
                    .trustLine(ledgerKeyTrustLine)
                    .discriminant(LedgerEntryType.TRUSTLINE)
                    .build();
            ledgerKeys.add(ledgerKey);
        }

        // 在账本中获取余额
        GetLedgerEntriesResponse ledgerEntries = this.getLedgerEntries(ledgerKeys);
        for (GetLedgerEntriesResponse.LedgerEntryResult ledgerEntryResult : ledgerEntries.getEntries()) {
            LedgerEntry.LedgerEntryData entryData;
            try {
                entryData = LedgerEntry.LedgerEntryData.fromXdrBase64(ledgerEntryResult.getXdr());
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid ledgerEntryData: " + ledgerEntryResult.getXdr(), e);
            }
            // 由于 native 币是从账户信息中获取, 因此需要判断返回类型是不是 Account
            if (entryData.getAccount() != null) {
                AccountEntry account = entryData.getAccount();
                Balance balance = new Balance(account.getBalance().getInt64(), new Currency(new AssetTypeNative()));
                balances.add(balance);
                continue;
            }
            // token 币从 trustLine 类型中获取
            TrustLineEntry trustLine = entryData.getTrustLine();
            TrustLineAsset trustLineAsset = trustLine.getAsset();
            Balance balance = new Balance(trustLine.getBalance().getInt64(), new Currency(AssetTypeCreditAlphaNum4.fromXdr(trustLineAsset.getAlphaNum4())));
            balances.add(balance);
        }
        return balances;
    }


    /**
     * 提交交易上链，Soroban-RPC 会 simply validates and enqueues the transaction，客户端需要再通过
     * SorobanServer#getTransaction 进一步查询交易状态，例如从 PENDING 转为 SUCCESS
     *
     * @param signedTransaction 已签名序列化后的交易
     * @return 返回txHash,和离线计算返回的txHash相同
     * @throws org.stellar.sdk.exception.NetworkException All the exceptions below are subclasses of
     *     NetworkError
     * @throws SorobanRpcException If the Soroban-RPC instance returns an error response.
     * @throws RequestTimeoutException If the request timed out.
     * @throws ConnectionErrorException When the request cannot be executed due to cancellation or
     *     connectivity problems, etc.
     * @see <a
     *     href="https://developers.stellar.org/docs/data/rpc/api-reference/methods/sendTransaction"
     *     target="_blank">sendTransaction documentation</a>
     */
    public String sendTransaction(String signedTransaction) {
        // TODO: In the future, it may be necessary to consider FeeBumpTransaction.
        SendTransactionRequest params = new SendTransactionRequest(signedTransaction);
        SendTransactionResponse transactionResponse = this.sendRequest(
                "sendTransaction", params, new TypeToken<SorobanRpcResponse<SendTransactionResponse>>() {
                });

        return transactionResponse.getHash();
    }


    /**
     * 获取指定交易
     * @param txHash 交易 hash, Hex 格式
     * @param network Stellar 网络标识 {@link Network}
     * @return TransactionVO
     */
    public TransactionVO getTransactionReceipt(String txHash, Network network) {
        GetTransactionRequest params = new GetTransactionRequest(txHash);
        GetTransactionResponse getTransactionResponse = this.sendRequest(
                "getTransaction", params, new TypeToken<SorobanRpcResponse<GetTransactionResponse>>() {
                });

        return TransactionUtil.parseFromResponse(getTransactionResponse, network);
    }

    /**
     * 获取指定账本中的交易
     * @param blockHeight 账本序号 ledger sequence
     * @param page 翻页参数
     * @param network Stellar 网络标识 {@link Network}
     * @return BlockTxnsVO
     */
    public BlockTxnsVO getTransctionsByblock(String blockHeight, GetTransactionsRequest.PaginationOptions page, Network network) {
        // 获取指定账本信息
        GetLedgersRequest.PaginationOptions paginationOptions = GetLedgersRequest.PaginationOptions.builder()
                .limit(1L).build();
        GetLedgersRequest getLedgersRequest = GetLedgersRequest.builder()
                .startLedger(Long.parseLong(blockHeight))
                .pagination(paginationOptions).build();
        GetLedgersResponse ledgers = this.getLedgers(getLedgersRequest);
        GetLedgersResponse.LedgerInfo ledgerInfo = ledgers.getLedgers().get(0);
        Hash previousLedgerHash = ledgerInfo.parseHeaderXdr().getHeader().getPreviousLedgerHash();

        // 获取指定账本的所有交易
        GetTransactionsRequest getTransactionsRequest = GetTransactionsRequest.builder()
                .startLedger(Long.valueOf(blockHeight))
                .pagination(page).build();

        GetTransactionsResponse transactionsResponse = this.getTransactions(getTransactionsRequest);
        List<TransactionVO> transactionVOs = TransactionUtil.parseFromResponse(transactionsResponse, network);

        // 组装返回
        return new BlockTxnsVO(ledgerInfo.getSequence(),
                ledgerInfo.getHash(),
                Util.bytesToHex(previousLedgerHash.getHash()).toLowerCase(),
                ledgerInfo.getLedgerCloseTime(),
                transactionVOs);
    }

    /**
     * 查询最新高度
     * @return BlockHeader, 包含最新账本序号, 账本 hash, 账本关闭时间, 前一个账本 hash
     */
    public BlockHeader getLatestBlock(){
        GetLatestLedgerResponse ledgerResponse = this.getLatestLedger();
        GetLedgersRequest.PaginationOptions paginationOptions = GetLedgersRequest.PaginationOptions.builder()
                .limit(1L).build();
        GetLedgersRequest getLedgersRequest = GetLedgersRequest.builder()
                .startLedger(ledgerResponse.getSequence().longValue())
                .pagination(paginationOptions).build();
        GetLedgersResponse ledgers = this.getLedgers(getLedgersRequest);
        GetLedgersResponse.LedgerInfo ledgerInfo = ledgers.getLedgers().get(0);
        Hash previousLedgerHash = ledgerInfo.parseHeaderXdr().getHeader().getPreviousLedgerHash();
        return new BlockHeader(ledgerInfo.getSequence().toString(),
                ledgerInfo.getHash(),
                ledgerInfo.getLedgerCloseTime(),
                Util.bytesToHex(previousLedgerHash.getHash()).toLowerCase());
    }

    /**
     * 预估调用合约的交易的手续费, 包含基础手续费和总体手续费
     * @param transaction 交易对象, 传入此方法的 transaction 对象无需签名.
     * @return Fee 基础手续费和手续费总和
     */
    public Fee getNetworkFee(Transaction transaction) {
        // 必须是调用合约的交易
        if (!transaction.isSorobanTransaction()) {
            throw new IllegalArgumentException(
                    "unsupported transaction: must contain exactly one InvokeHostFunctionOperation, BumpSequenceOperation, or RestoreFootprintOperation");
        }
        Fee fee = this.getNetworkFee(1);
        Transaction txToGetFee = new Transaction(
                transaction.getSourceAccount(),
                fee.getPreSorobanInclusionFee(),
                transaction.getSequenceNumber(),
                transaction.getOperations(),
                transaction.getMemo(),
                transaction.getPreconditions(),
                transaction.getSorobanData(),
                transaction.getNetwork());
        // 不会提交交易,  只是预估手续费并赋值 SorobanTransactionData 对象
        final Transaction tx = this.prepareTransaction(txToGetFee);
        fee.setTotalFee(tx.getFee());
        return fee;
    }

    /**
     * 预估非调用合约的交易的手续费, 包含基础手续费和总体手续费
     * @param numOfOP 交易中操作的数量
     * @return Fee 基础手续费和手续费总和
     */
    public Fee getNetworkFee(int numOfOP) {
        GetFeeStatsResponse feeStats = this.getFeeStats();
        Long p50 = feeStats.getInclusionFee().getP50();
        Long pMost = feeStats.getInclusionFee().getMode();
        Long baseInclusionFee = Math.max(p50, pMost);
        p50 = feeStats.getSorobanInclusionFee().getP50();
        pMost = feeStats.getSorobanInclusionFee().getMode();
        Long baseSorobanInclusionFee = Math.max(p50, pMost);
        return new Fee(baseSorobanInclusionFee, baseInclusionFee, baseInclusionFee * numOfOP);
    }

        /**
         * 读取合约的链上存储
         * @param contractId 合约地址，Encoded as Stellar Contract Address. e.g.
         *    "CCJZ5DGASBWQXR5MPFCJXMBI333XE5U3FSJTNQU7RIKE3P5GN2K2WYD5"
         * @param key SCVal（Stellar Contract Value）类型
         * @param durability The "durability keyspace" that this ledger key belongs to, which is either
         *    {@link Durability#TEMPORARY} or {@link Durability#PERSISTENT}.
         * @return A {@link GetLedgerEntriesResponse.LedgerEntryResult} object containing the ledger entry
         *     result.
         * @throws org.stellar.sdk.exception.NetworkException All the exceptions below are subclasses of
         *     NetworkError
         * @throws SorobanRpcException If the Soroban-RPC instance returns an error response.
         * @throws RequestTimeoutException If the request timed out.
         * @throws ConnectionErrorException When the request cannot be executed due to cancellation or
         *     connectivity problems, etc.
         */
    public Optional<ContractDataEntry> getContractDataEntry(String contractId, SCVal key, Durability durability) {

        ContractDataDurability contractDataDurability;
        switch (durability) {
            case TEMPORARY:
                contractDataDurability = ContractDataDurability.TEMPORARY;
                break;
            case PERSISTENT:
                contractDataDurability = ContractDataDurability.PERSISTENT;
                break;
            default:
                throw new IllegalArgumentException("Invalid durability: " + durability);
        }

        Address address = new Address(contractId);
        LedgerKey.LedgerKeyContractData ledgerKeyContractData =
                LedgerKey.LedgerKeyContractData.builder()
                        .contract(address.toSCAddress())
                        .key(key)
                        .durability(contractDataDurability)
                        .build();
        LedgerKey ledgerKey =
                LedgerKey.builder()
                        .discriminant(LedgerEntryType.CONTRACT_DATA)
                        .contractData(ledgerKeyContractData)
                        .build();
        GetLedgerEntriesResponse getLedgerEntriesResponse = this.getLedgerEntries(Collections.singleton(ledgerKey));
        List<GetLedgerEntriesResponse.LedgerEntryResult> entries = getLedgerEntriesResponse.getEntries();
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        LedgerEntry.LedgerEntryData ledgerEntryData;
        try {
            ledgerEntryData = LedgerEntry.LedgerEntryData.fromXdrBase64(entries.get(0).getXdr());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid ledgerEntryData: " + entries.get(0).getXdr(), e);
        }
        return Optional.of(ledgerEntryData.getContractData());
    }

}
