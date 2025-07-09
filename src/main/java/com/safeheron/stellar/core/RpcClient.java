package com.safeheron.stellar.core;

import com.safeheron.stellar.entity.AddressIdentifier;
import com.safeheron.stellar.entity.AddressVerify;
import com.safeheron.stellar.entity.PublicKeyIdentifier;
import okhttp3.OkHttpClient;
import org.stellar.sdk.Address;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.responses.sorobanrpc.GetLedgerEntriesResponse;
import org.stellar.sdk.xdr.*;

import java.io.IOException;
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
