package com.safeheron.stellar.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author Allenzsy
 * @Date 2025/7/26 21:52
 * @Description:
 */
@Getter
@Setter
@AllArgsConstructor
public class Fee {

    // 预估的调用合约的基础费用, 单位是最小单位 1 Stroop = 0.0000001 XLM
    Long preSorobanInclusionFee;
    // 预估的非调用合约的基础费用, 单位是最小单位 1 Stroop = 0.0000001 XLM
    Long preInclusionFee;
    // 预估的费用总和, 单位是最小单位 1 Stroop = 0.0000001 XLM
    Long totalFee;

}
