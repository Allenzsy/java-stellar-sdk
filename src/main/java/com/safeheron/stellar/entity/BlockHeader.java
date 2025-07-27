package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Author Allenzsy
 * @Date 2025/7/28 2:11
 * @Description:
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockHeader {

    /**
     * 在 Stellar 也称 ledger sequence
     */
    String height;
    /**
     * 在 Stellar 也称 ledger hash
     */
    String blockHash;
    /**
     * 在 Stellar 也称 ledger close time
     */
    Long timeStamp;
    /**
     * 在 Stellar 也称 previous ledger hash
     */
    String parenBlockHash;



}
