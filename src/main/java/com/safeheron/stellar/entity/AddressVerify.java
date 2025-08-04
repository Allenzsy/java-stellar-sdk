package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author Allenzsy
 * @Date 2025/6/11 23:44
 * @Description: 地址验证结果
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AddressVerify {

    /** 地址格式是否正确，如果不正确不用再校验地址是否真实存在于链上 */
    boolean isFormatCorrect;

    /** 是否 Stellar account 账户地址 (G...)  */
    boolean isAccountAddress;

    /** 是否 WASM contract (也叫 Soroban contract) 合约地址 (C...) */
    boolean isContractAddress;

    /** 是否 Muxed Account (并不存在于链上, 为了便捷和标准化而被嵌入到协议中) 多路复用账户地址 (M...) */
    boolean isMuxedAccountAddress;

    /** 地址在链上是否存在 */
    boolean isExistOnChain;

}
