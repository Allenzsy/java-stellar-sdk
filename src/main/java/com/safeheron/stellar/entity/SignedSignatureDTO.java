package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author Allenzsy
 * @Date 2025/7/17 23:57
 * @Description: 已签名的 signature 和公钥
 */
@Getter
@AllArgsConstructor
public class SignedSignatureDTO {

    /**
     * 签名完成后的数据(hash), Hex格式
     */
    String signedTxHash;

    /**
     * 签名公钥, Hex格式
     */
    String publickKey;

}
