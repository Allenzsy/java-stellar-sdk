package com.safeheron.stellar.entity;

import com.safeheron.stellar.util.Util;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.Network;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.exception.UnexpectedException;
import org.stellar.sdk.xdr.*;

import java.io.IOException;

/**
 * @Author zsy
 * @Date 2025/7/4 16:34
 * @Description: 文档中的 Currency 币种对应 Stellar 的 Asset
 */
@Getter
public abstract class Currency implements Comparable<Currency>{
    /** Asset code */
    @NonNull final String code;

    /** Asset issuer */
    @NonNull final String issuer;

    /** Asset ContractId */
    String tokenIdentifier;

    public Currency(String code, String issuer) {
        this.code = code;
        this.issuer = issuer;
    }

    @Override
    public String toString() {
        return getCode() + ":" + getIssuer();
    }

    /**
     * Generates Asset object from a given XDR object
     *
     * @param xdr XDR object
     */
    public static Currency fromXdr(org.stellar.sdk.xdr.Asset xdr) {
        String accountId;
        switch (xdr.getDiscriminant()) {
            case ASSET_TYPE_NATIVE:
                return new CurrencyNative();
            case ASSET_TYPE_CREDIT_ALPHANUM4:
                String assetCode4 =
                        Util.paddedByteArrayToString(xdr.getAlphaNum4().getAssetCode().getAssetCode4());
                accountId = StrKey.encodeEd25519PublicKey(xdr.getAlphaNum4().getIssuer());
                return new CurrencyAlphaNum4(assetCode4, accountId);
            case ASSET_TYPE_CREDIT_ALPHANUM12:
                String assetCode12 =
                        Util.paddedByteArrayToString(xdr.getAlphaNum12().getAssetCode().getAssetCode12());
                accountId = StrKey.encodeEd25519PublicKey(xdr.getAlphaNum12().getIssuer());
                return new CurrencyAlphaNum12(assetCode12, accountId);
            default:
                throw new IllegalArgumentException("Unknown asset type " + xdr.getDiscriminant());
        }
    }

    // 子类实现
    public abstract int compareTo(@NotNull Currency o);

    /** Generates XDR object from a given Asset object */
    public abstract org.stellar.sdk.xdr.Asset toXdr();

    /** Returns asset type. */
    public abstract AssetType getType();

    /**
     * Returns the contract Id for the asset contract.
     *
     * @param network The network where the asset is located.
     * @return The contract Id for the asset contract.
     */
    public String getContractId(Network network) {
        HashIDPreimage preimage =
                HashIDPreimage.builder()
                        .discriminant(EnvelopeType.ENVELOPE_TYPE_CONTRACT_ID)
                        .contractID(
                                HashIDPreimage.HashIDPreimageContractID.builder()
                                        .networkID(new Hash(network.getNetworkId()))
                                        .contractIDPreimage(
                                                ContractIDPreimage.builder()
                                                        .discriminant(ContractIDPreimageType.CONTRACT_ID_PREIMAGE_FROM_ASSET)
                                                        .fromAsset(this.toXdr())
                                                        .build())
                                        .build())
                        .build();
        byte[] rawContractId = null;
        try {
            rawContractId = Util.hash(preimage.toXdrByteArray());
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
        return StrKey.encodeContract(rawContractId);
    }
}
