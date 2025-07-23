package com.safeheron.stellar.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author Allenzsy
 * @Date 2025/7/16 0:18
 * @Description:
 */
@Getter
@AllArgsConstructor
public class NeedSignSignatureDTO {
    /** 交易的hash值, Hex格式 */
    String txHash;

    /** 需要此地址签名, From地址, Hex格式*/
    String address;
}
