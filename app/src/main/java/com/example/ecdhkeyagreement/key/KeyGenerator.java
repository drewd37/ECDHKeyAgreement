package com.example.ecdhkeyagreement.key;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class KeyGenerator {

    private KeyPair keyPair;

    public KeyGenerator() {}

    // invoke before getting keys
    public void generateKeyPair() {
        KeyPairGenerator keyGen=null;
        try {
            keyGen = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        int size = 256;
        keyGen.initialize(size);
        this.keyPair = keyGen.generateKeyPair();
    }

    // invoke after generateKeyPair()
    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public void temp() {
        KeyPairGenerator keyGen=null;
        try {
            keyGen = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        int size = 256;
        keyGen.initialize(size);
        KeyPair keyPair = keyGen.generateKeyPair();

        //generate public/private DH keys for each client
        KeyPairGenerator keyGen2 = null;
        try {
            keyGen2 = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        keyGen2.initialize(size);
        KeyPair keyPair2 = keyGen2.generateKeyPair();

        //PublicKey pubKey = keyPair.getPublic();
        //PrivateKey privKey = keyPair.getPrivate();

        //begin key agreement protocol
        //i.e. generate shared secret key to be used for secure communication
    }
}
