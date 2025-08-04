package com.safeheron.stellar.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author Allenzsy
 * @Date 2025/7/16 0:18
 * @Description: 待签名的 signature 和公钥
 */
@Getter
@AllArgsConstructor
public class NeedSignSignatureDTO {

    /**
     * 交易的 hash 值, Hex 格式
     */
    String txHash;

    /**
     * 需要此地址签名, From 地址, Hex 格式
     */
    String address;

}
