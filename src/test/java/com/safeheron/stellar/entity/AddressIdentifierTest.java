package com.safeheron.stellar.entity;

import org.junit.Assert;
import org.junit.Test;

/**
 * @Author Allenzsy
 * @Date 2025/8/4 3:11
 * @Description:
 */
public class AddressIdentifierTest {

    @Test
    public void test_deriveException() {
        String heyBytes = "3a5c1615c31b1c129a9bb594f68ffe15cdaa9de52f4f379e8cb5928e58cbdb4a";
        boolean res = false;
        try {
            PublicKeyIdentifier identifier = new PublicKeyIdentifier(heyBytes.substring(10));
            AddressIdentifier.derive(identifier);
        } catch (Exception e) {
            res = true;
        }
        Assert.assertTrue(res);
        try {
            res = false;
            PublicKeyIdentifier identifier = new PublicKeyIdentifier(heyBytes+"123123");
            AddressIdentifier.derive(identifier);

        } catch (Exception e) {
            res = true;
        }
        PublicKeyIdentifier identifier = new PublicKeyIdentifier("0x" + heyBytes);
        Assert.assertEquals(2, AddressIdentifier.derive(identifier).size());
        Assert.assertTrue(res);
    }

}
