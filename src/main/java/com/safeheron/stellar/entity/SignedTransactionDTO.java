package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Author Allenzsy
 * @Date 2025/7/18 0:07
 * @Description:
 */
@Getter
@Setter
@NoArgsConstructor
public class SignedTransactionDTO {

    /** 离线计算, 交易txHash, Hex格式 */
    String txHash;

    /** 已经签名交易. 可以提交上链 */
    String signedTransaction;


}
