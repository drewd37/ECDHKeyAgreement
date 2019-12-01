package com.example.ecdhkeyagreement.key;

public class KeyUtil {

    public static String byteArrayToHex(byte[] a) {
        if(a == null)
            return "";
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] byteAppend(byte[] b1, byte[] b2) {
        // create a destination array that is the size of the two arrays
        byte[] destination = new byte[b1.length + b2.length];

        // copy ciphertext into start of destination (from pos 0, copy ciphertext.length bytes)
        System.arraycopy(b1, 0, destination, 0, b1.length);

        // copy mac into end of destination (from pos ciphertext.length, copy mac.length bytes)
        System.arraycopy(b2, 0, destination, b1.length, b2.length);

        return destination;
    }
}
