package com.example.ecdhkeyagreement.crypto;

import android.util.Base64;

import com.example.ecdhkeyagreement.key.KeyManager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private byte[] key;

    public CryptoManager() {
    }

    public byte[] encrypt(byte[] data) {
        try
        {
            key = KeyManager.getInstance().getDHKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.encode(cipher.doFinal(data), Base64.DEFAULT);
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public byte[] decrpyt(byte[] data) {
        try
        {
            key = KeyManager.getInstance().getDHKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }


}
