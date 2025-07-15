package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author zsy
 * @Date 2025/7/4 15:50
 * @Description:
 */
@Getter
@Setter
public class Balance {

    int minUnit;
    Long balance;
    Currency currency;

    public Balance() {
        this.minUnit = 7;
    }

    public Balance(Long balance, Currency currency) {
        this.minUnit = 7;
        this.balance = balance;
        this.currency = currency;
    }

}
