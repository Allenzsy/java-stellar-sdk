package com.safeheron.stellar.entity;

import com.safeheron.stellar.util.Util;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.Network;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.xdr.AssetCode4;
import org.stellar.sdk.xdr.AssetType;

/**
 * @Author Allenzsy
 * @Date 2025/7/6 12:27
 * @Description:
 */
@Getter
public class CurrencyAlphaNum4 extends Currency {

    public CurrencyAlphaNum4(String code, String issuer, Network network) {
        this(code, issuer);
        this.tokenIdentifier = getContractId(network);
    }

    public CurrencyAlphaNum4(String code, String issuer) {
        super(code, issuer);
        if (code.isEmpty() || code.length() > 4) {
            throw new IllegalArgumentException("The length of code must be between 1 and 4 characters.");
        }
    }

    @Override
    public int compareTo(@NotNull Currency other) {
        if (AssetType.ASSET_TYPE_CREDIT_ALPHANUM12.equals(other.getType())) {
            return -1;
        } else if (AssetType.ASSET_TYPE_NATIVE.equals(other.getType())) {
            return 1;
        }

        CurrencyAlphaNum4 o = (CurrencyAlphaNum4) other;

        if (!this.getCode().equals(o.getCode())) {
            return this.getCode().compareTo(o.getCode());
        }

        return this.getIssuer().compareTo(o.getIssuer());
    }

    public static CurrencyAlphaNum4 fromXdr(org.stellar.sdk.xdr.AlphaNum4 alphaNum4) {
        String assetCode4 = Util.paddedByteArrayToString(alphaNum4.getAssetCode().getAssetCode4());
        String accountId = StrKey.encodeEd25519PublicKey(alphaNum4.getIssuer());
        return new CurrencyAlphaNum4(assetCode4, accountId);
    }

    @Override
    public org.stellar.sdk.xdr.Asset toXdr() {
        org.stellar.sdk.xdr.Asset xdr = new org.stellar.sdk.xdr.Asset();
        xdr.setDiscriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM4);
        org.stellar.sdk.xdr.AlphaNum4 credit = new org.stellar.sdk.xdr.AlphaNum4();
        AssetCode4 assetCode4 = new AssetCode4();
        assetCode4.setAssetCode4(Util.paddedByteArray(code, 4));
        credit.setAssetCode(assetCode4);
        credit.setIssuer(StrKey.encodeToXDRAccountId(issuer));
        xdr.setAlphaNum4(credit);
        return xdr;
    }

    @Override
    public AssetType getType() {
        return AssetType.ASSET_TYPE_CREDIT_ALPHANUM4;
    }
}
