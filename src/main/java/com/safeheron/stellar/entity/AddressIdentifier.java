package com.safeheron.stellar.entity;

import kotlin.collections.ArrayDeque;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;
import org.stellar.sdk.Address.AddressType;
import org.stellar.sdk.StrKey;
import org.stellar.sdk.xdr.CryptoKeyType;
import org.stellar.sdk.xdr.MuxedAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author Allenzsy
 * @Date 2025/7/10 1:57
 * @Description:
 */
@EqualsAndHashCode
@Getter
public class AddressIdentifier {

    /** 原始公钥值 (未转换, 十六进制格式) */
    private final String hexPublicKey;
    /** Stellar 地址值 G... or C... */
    private final String stellarAddress;
    /** 地址类型 */
    private final AddressType type;

    public AddressIdentifier(@NonNull String hexPublicKey, @NonNull String stellarAddress, @NonNull AddressType type) {
        this.hexPublicKey = hexPublicKey;
        this.stellarAddress = stellarAddress;
        this.type = type;
    }

    public static List<AddressIdentifier> derive(PublicKeyIdentifier publicKeyIdentifier) {
        String key = publicKeyIdentifier.getHexBytes();
        if (key == null || key.length() < 64 || key.length() > 66 || key.length() == 65 || !key.startsWith("0x")) {
            throw new IllegalArgumentException(
                    String.format("Unsupported address value, address length is: %s", key == null ? "null" : key.length() + ""));
        }

        key = key.startsWith("0x") ? key.substring(2) : key;
        final byte[] keyBytes = Hex.decode(key);
        final List<AddressIdentifier> list = new ArrayList<>();
        list.add(new AddressIdentifier(key, StrKey.encodeEd25519PublicKey(keyBytes), AddressType.ACCOUNT));
        list.add(new AddressIdentifier(key, StrKey.encodeContract(keyBytes), AddressType.CONTRACT));
        return list;
    }

    /**
     * 验证地址格式
     * @param address Stellar 地址字符串，G..., C..., M...
     * @return AddressVerify 地址验证结果 (并不验证链上是否存在)
     */
    public static AddressVerify validAddressFormat(AddressIdentifier address) {
        boolean isAccount = StrKey.isValidEd25519PublicKey(address.getStellarAddress());
        boolean isContract  = StrKey.isValidContract(address.getStellarAddress());
        boolean isMuxedAccount = isValidMuxedAccount(address.getStellarAddress());

        return new AddressVerify(isAccount || isContract || isMuxedAccount,
                isAccount,
                isContract,
                isMuxedAccount,
                false);
    }

    /**
     * Checks validity of Muxed Account (M...) address.
     *
     * @param muxedAccount Muxed Account Address to check
     * @return true if the given Muxed Account Address is a valid Muxed Account Address, false otherwise
     */
    public static boolean isValidMuxedAccount(String muxedAccount) {
        try {
            final MuxedAccount account = StrKey.decodeMuxedAccount(muxedAccount);
            return account.getDiscriminant() == CryptoKeyType.KEY_TYPE_MUXED_ED25519;
        } catch (Exception e) {
            return false;
        }
    }

}
