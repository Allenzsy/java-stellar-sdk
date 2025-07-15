package com.safeheron.stellar.entity;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.xdr.AssetType;

/**
 * @Author Allenzsy
 * @Date 2025/7/6 12:20
 * @Description:
 */
@Getter
public final class CurrencyNative extends Currency {

    String tokenIdentifier;

    public CurrencyNative() {
        super("XLM", "NATIVE");
        this.tokenIdentifier = "NATIVE";
    }

    @Override
    public int compareTo(@NotNull Currency other) {
        if (AssetType.ASSET_TYPE_NATIVE.equals(other.getType())) {
            return 0;
        }
        return -1;
    }

    @Override
    public org.stellar.sdk.xdr.Asset toXdr() {
        org.stellar.sdk.xdr.Asset xdr = new org.stellar.sdk.xdr.Asset();
        xdr.setDiscriminant(AssetType.ASSET_TYPE_NATIVE);
        return xdr;
    }

    @Override
    public AssetType getType() {
        return AssetType.ASSET_TYPE_NATIVE;
    }
}
