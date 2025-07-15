package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.TransactionBuilderAccount;

/**
 * @Author Allenzsy
 * @Date 2025/7/12 2:29
 * @Description:
 */

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class AccountIdentifier implements TransactionBuilderAccount {

    /** The account ID. */
    @NonNull private final String accountId;

    /** The sequence number of the account. */
    @NonNull private Long sequenceNumber;

    @Override
    public KeyPair getKeyPair() {
        return KeyPair.fromAccountId(accountId);
    }

    @Override
    public void setSequenceNumber(long seqNum) {
        sequenceNumber = seqNum;
    }

    @Override
    public Long getIncrementedSequenceNumber() {
        return sequenceNumber + 1;
    }

    /** Increments sequence number in this object by one. */
    public void incrementSequenceNumber() {
        sequenceNumber++;
    }

}
