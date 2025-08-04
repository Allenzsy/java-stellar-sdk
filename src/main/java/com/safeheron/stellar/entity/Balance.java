package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author zsy
 * @Date 2025/7/4 15:50
 * @Description: 余额
 */
@Getter
@Setter
@ToString
public class Balance {

    /**
     * 余额, 单位是最小单位例如 native 币是 1 Stroop = 0.0000001 XLM, token 币也是 0.0000001
     */
    Long value;

    /**
     * 币种
     */
    Currency currency;

    public Balance() {
    }

    public Balance(Long value, Currency currency) {
        this.value = value;
        this.currency = currency;
    }

}
