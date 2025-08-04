package com.safeheron.stellar.entity;

import lombok.*;

import java.util.List;

/**
 * @Author Allenzsy
 * @Date 2025/7/22 2:27
 * @Description: 某一区块(Stellar中也叫账本)包含的所有交易
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BlockTxnsVO {

    /**
     * 在 Stellar 也称 ledger sequence
     */
    Long blockNum;
    /**
     *  当前块 Hash
     */
    String blockHash;
    /**
     *  上个区块hash，如果不方便可以不用
     */
    String paretHash;
    /**
     *  区块时间
     */
    Long timeStamp;
    /**
     * 解析出来的交易集合
     */
    List<TransactionVO> transactions;

}
