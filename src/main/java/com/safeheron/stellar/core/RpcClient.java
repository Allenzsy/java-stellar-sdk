package com.safeheron.stellar.core;

import com.safeheron.stellar.entity.*;
import okhttp3.OkHttpClient;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.sorobanrpc.GetLedgerEntriesResponse;
import org.stellar.sdk.xdr.*;
import org.stellar.sdk.xdr.Asset;
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

    public AddressVerify validAddressOnChain(AddressIdentifier address) {
        AddressVerify addressFormat = AddressIdentifier.validAddressFormat(address);
        if (!addressFormat.isFormatCorrect()) {
            return addressFormat;
        }
        boolean isExistOnChain;
        try {
            if (addressFormat.isAccountAddress()) {
                TransactionBuilderAccount account = this.getAccount(address.getStellarAddress());
                isExistOnChain = account != null && address.equals(account.getAccountId());
            } else {
                SCVal scVal = new SCVal();
                scVal.setDiscriminant(SCValType.SCV_LEDGER_KEY_CONTRACT_INSTANCE);
                final Optional<ContractDataEntry> contractData =
                        this.getContractDataEntry(address.getStellarAddress(), scVal, Durability.PERSISTENT);
                isExistOnChain = contractData.isPresent();
            }
        } catch (Exception e) {
            throw e;
        }
        addressFormat.setExistOnChain(isExistOnChain);
        return addressFormat;
    }


    public List<Balance> getBalances(String accountAddress, List<Currency> currencies) {
        Collections.sort(currencies);
        ArrayList<LedgerKey> ledgerKeys = new ArrayList<>();
        ArrayList<Balance> balances = new ArrayList<>();
        AccountID accountId = KeyPair.fromAccountId(accountAddress).getXdrAccountId();
        for (Currency currency : currencies) {
            // 查询 Native 币的余额必须通过查询账户信息获取
            if (currency.getType() == AssetType.ASSET_TYPE_NATIVE) {
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
            org.stellar.sdk.xdr.Asset assetXdr = currency.toXdr();
            TrustLineAsset trustLineAsset = new TrustLineAsset();
            trustLineAsset.setDiscriminant(assetXdr.getDiscriminant());
            trustLineAsset.setAlphaNum4(assetXdr.getAlphaNum4());
            trustLineAsset.setAlphaNum12(assetXdr.getAlphaNum12());
            LedgerKey.LedgerKeyTrustLine ledgerKeyTrustLine = LedgerKey.LedgerKeyTrustLine.builder()
                    .accountID(accountId)
                    .asset(trustLineAsset)
                    .build();
            LedgerKey ledgerKey = LedgerKey.builder()
                    .trustLine(ledgerKeyTrustLine)
                    .discriminant(LedgerEntryType.TRUSTLINE)
                    .build();
            ledgerKeys.add(ledgerKey);
        }

        GetLedgerEntriesResponse ledgerEntries = this.getLedgerEntries(ledgerKeys);
        for (GetLedgerEntriesResponse.LedgerEntryResult ledgerEntryResult : ledgerEntries.getEntries()) {
            LedgerEntry.LedgerEntryData entryData;
            try {
                entryData = LedgerEntry.LedgerEntryData.fromXdrBase64(ledgerEntryResult.getXdr());
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid ledgerEntryData: " + ledgerEntryResult.getXdr(), e);
            }
            if (entryData.getAccount() != null) {
                AccountEntry account = entryData.getAccount();
                Balance balance = new Balance(account.getBalance().getInt64(), new CurrencyNative());
                balances.add(balance);
                continue;
            }
            TrustLineEntry trustLine = entryData.getTrustLine();
            TrustLineAsset trustLineAsset = trustLine.getAsset();
            Balance balance = new Balance();
            switch (trustLineAsset.getDiscriminant()) {
                case ASSET_TYPE_NATIVE:
                    balance.setCurrency(new CurrencyNative());
                    break;
                case ASSET_TYPE_CREDIT_ALPHANUM4:
                    balance.setCurrency(CurrencyAlphaNum4.fromXdr(trustLineAsset.getAlphaNum4()));
                    break;
                case ASSET_TYPE_CREDIT_ALPHANUM12:
                    balance.setCurrency(CurrencyAlphaNum12.fromXdr(trustLineAsset.getAlphaNum12()));
                    break;
            }
            if (balance.getCurrency() == null) {
                throw new IllegalArgumentException("Unknown asset type " + trustLineAsset.getDiscriminant());
            }
            balance.setBalance(trustLine.getBalance().getInt64());
            balances.add(balance);
        }

        return balances;
    }

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
