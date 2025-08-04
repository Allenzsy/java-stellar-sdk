package com.safeheron.stellar.entity;

import lombok.Getter;
import org.bouncycastle.util.encoders.Hex;
import org.stellar.sdk.xdr.PublicKey;
import org.stellar.sdk.xdr.PublicKeyType;
import org.stellar.sdk.xdr.Uint256;

/**
 * @Author Allenzsy
 * @Date 2025/7/10 1:43
 * @Description: 公钥
 */
@Getter
public class PublicKeyIdentifier {

    /** 32 bytes 公钥的十六进制字符串 */
    String hexBytes;

    /** 公钥类型 */
    CurveType curveType;

    public PublicKeyIdentifier(String hexBytes, CurveType curveType) {
        this.hexBytes = hexBytes.toLowerCase();
        this.curveType = curveType;
    }

    public PublicKeyIdentifier(String hexBytes) {
        this.hexBytes = hexBytes.toLowerCase();
        this.curveType = CurveType.PUBLIC_KEY_TYPE_ED25519;
    }

    public PublicKey toXdrPublicKey() {
        return new PublicKey(PublicKeyType.PUBLIC_KEY_TYPE_ED25519,
                new Uint256(Hex.decode(hexBytes)));
    }

    public enum CurveType {
        PUBLIC_KEY_TYPE_ED25519(0);

        private final int value;

        CurveType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
