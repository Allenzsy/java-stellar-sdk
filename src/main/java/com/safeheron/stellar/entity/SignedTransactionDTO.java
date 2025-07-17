package com.safeheron.stellar.entity;

/**
 * @Author Allenzsy
 * @Date 2025/7/18 0:07
 * @Description:
 */
public class SignedTransactionDTO {

    /** 离线计算, 交易txHash */
    String txHash;

    /** 已经签名交易. 可以提交上链 */
    String signedTransaction;

}
