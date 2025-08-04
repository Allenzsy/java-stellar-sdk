package com.safeheron.stellar.entity;

import lombok.*;

/**
 * @Author Allenzsy
 * @Date 2025/7/22 2:11
 * @Description: 区块（也称账本）信息
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
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
