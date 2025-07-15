package com.safeheron.stellar.util;


import org.stellar.sdk.*;
import org.stellar.sdk.xdr.MemoType;
import org.stellar.sdk.xdr.Uint64;
import org.stellar.sdk.xdr.XdrString;
import org.stellar.sdk.xdr.XdrUnsignedHyperInteger;

/**
 * @Author Allenzsy
 * @Date 2025/7/15 23:37
 * @Description:
 */
public class MemoUtil {

    public static org.stellar.sdk.xdr.Memo toXdr(Memo memo) {
        org.stellar.sdk.xdr.Memo res = new org.stellar.sdk.xdr.Memo();
        if (memo instanceof MemoNone) {
            res.setDiscriminant(MemoType.MEMO_NONE);
        } else if (memo instanceof MemoText) {
            res.setDiscriminant(MemoType.MEMO_TEXT);
            res.setText(new XdrString(((MemoText) memo).getBytes()));
        } else if (memo instanceof MemoId) {
            res.setDiscriminant(MemoType.MEMO_ID);
            Uint64 idXdr = new Uint64(new XdrUnsignedHyperInteger(((MemoId) memo).getId()));
            res.setId(idXdr);
        } else if (memo instanceof MemoHash) {
            res.setDiscriminant(MemoType.MEMO_HASH);
            org.stellar.sdk.xdr.Hash hash = new org.stellar.sdk.xdr.Hash();
            hash.setHash(((MemoHash) memo).getBytes());
            res.setHash(hash);
        } else if (memo instanceof MemoReturnHash) {
            res.setDiscriminant(MemoType.MEMO_RETURN);
            org.stellar.sdk.xdr.Hash hash = new org.stellar.sdk.xdr.Hash();
            hash.setHash(((MemoReturnHash) memo).getBytes());
            res.setRetHash(hash);
        } else {
            throw new IllegalArgumentException("Wrong Memo Type");
        }

        return res;
    }

}
