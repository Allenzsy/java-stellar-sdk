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

    Long preSorobanInclusionFee;

    Long preInclusionFee;

    Long totalFee;

}
