package com.example.ecdhkeyagreement.key;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;

// a singleton class to manage public / private / encryption DH keys
public class KeyManager {

    private static KeyManager instance = null;
    private KeyPair keyPair;
    private byte[] dhKey;
    private boolean finished = false;

    private KeyManager() {}

    public static KeyManager getInstance() {
        if (instance == null)
            instance = new KeyManager();
        return instance;
    }

    public boolean isFinished() {return instance.finished;}

    public void setFinished(Boolean finished) {this.finished = finished;}

    public void setKeyPair(KeyPair key) {
        instance.keyPair = key;
    }

    public KeyPair getKeyPair() {
        return instance.keyPair;
    }

    public byte[] getDHKey() {
        return instance.dhKey;
    }


    /**
     * DH key negotiation
     * @param publicKey public key from the other entity
     */
    public void calculateDHKey(PublicKey publicKey) {
        KeyAgreement ka1 = null;
        try {
            ka1 = KeyAgreement.getInstance("ECDH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            ka1.init(instance.keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        //get public key of other key pair here
        //i.e. public key of other party communicating with
        try {
            ka1.doPhase(publicKey, true);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        //generate secret key
        instance.dhKey = ka1.generateSecret();

//        KeyAgreement ka2 = null;
//        try {
//            ka2 = KeyAgreement.getInstance("ECDH");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        try {
//            ka2.init(keyPair2.getPrivate());
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        }
//        try {
//            ka2.doPhase(keyPair.getPublic(), true);
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        }
//
//        byte[] secret2 = ka2.generateSecret();
    }




}
