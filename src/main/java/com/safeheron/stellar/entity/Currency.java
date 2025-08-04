package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.AssetTypeNative;

/**
 * @Author Allenzsy
 * @Date 2025/8/4 1:38
 * @Description: 币种
 */
@Getter
@NoArgsConstructor
public class Currency implements Comparable<Currency> {

    /**
     * 标识符
     */
    String symbol;

    /**
     * 小数位
     */
    Integer decimals;

    /**
     * token 合约地址或 Stellar 发行地址，主链币为常量“NATIVE”
     */
    String tokenIdentifier;

    /**
     * Stellar 资产, 分为 native 币和 token 币
     */
    Asset asset;

    public Currency(AssetTypeNative assetNative) {
        this.symbol = "XLM";
        this.decimals = 7;
        this.tokenIdentifier = "NATIVE";
        this.asset = assetNative;
    }

    public Currency(AssetTypeCreditAlphaNum assetToken) {
        this.symbol = assetToken.getCode();
        this.decimals = 7;
        this.tokenIdentifier = assetToken.getIssuer();
        this.asset = assetToken;
    }

    @Override
    public int compareTo(@NotNull Currency other) {
        return this.getAsset().compareTo(other.getAsset());
    }

}
