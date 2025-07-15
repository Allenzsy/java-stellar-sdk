package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @Author Allenzsy
 * @Date 2025/7/16 0:16
 * @Description:
 */
@Getter
@AllArgsConstructor
public class NeedSignTransactionDTO {

    /** Hex格式, 未签名交易序列化 */
    String unsignedTransaction;

    /** 待签名数据, 若存在多签则有多个 */
    List<NeedSignSignatureDTO> unsignedSignatures;

}
