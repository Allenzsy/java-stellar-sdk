package com.safeheron.stellar.entity;

import com.safeheron.stellar.util.Util;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.Network;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.xdr.AssetCode12;
import org.stellar.sdk.xdr.AssetType;

/**
 * @Author Allenzsy
 * @Date 2025/7/6 15:29
 * @Description:
 */
public class CurrencyAlphaNum12 extends Currency {


    public CurrencyAlphaNum12(String code, String issuer, Network network) {
        this(code, issuer);
        this.tokenIdentifier = getContractId(network);
    }

    public CurrencyAlphaNum12(String code, String issuer) {
        super(code, issuer);
        if (code.length() < 5 || code.length() > 12) {
            throw new IllegalArgumentException("The length of code must be between 5 and 12 characters.");
        }
    }

    @Override
    public int compareTo(@NotNull Currency o) {
        return 0;
    }

    public static CurrencyAlphaNum12 fromXdr(org.stellar.sdk.xdr.AlphaNum12 alphaNum12) {
        String assetCode12 = Util.paddedByteArrayToString(alphaNum12.getAssetCode().getAssetCode12());
        String accountId = StrKey.encodeEd25519PublicKey(alphaNum12.getIssuer());
        return new CurrencyAlphaNum12(assetCode12, accountId);
    }

    @Override
    public org.stellar.sdk.xdr.Asset toXdr() {
        org.stellar.sdk.xdr.Asset xdr = new org.stellar.sdk.xdr.Asset();
        xdr.setDiscriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM12);
        org.stellar.sdk.xdr.AlphaNum12 credit = new org.stellar.sdk.xdr.AlphaNum12();
        AssetCode12 assetCode12 = new AssetCode12();
        assetCode12.setAssetCode12(Util.paddedByteArray(code, 12));
        credit.setAssetCode(assetCode12);
        credit.setIssuer(StrKey.encodeToXDRAccountId(issuer));
        xdr.setAlphaNum12(credit);
        return xdr;
    }

    @Override
    public AssetType getType() {
        return AssetType.ASSET_TYPE_CREDIT_ALPHANUM12;
    }
}
