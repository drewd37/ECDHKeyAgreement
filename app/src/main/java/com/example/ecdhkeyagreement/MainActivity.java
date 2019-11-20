package com.example.ecdhkeyagreement;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.KeyAgreement;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //generate public/private DH keys for each client
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

        //begin key agreement protcol
        //i.e. generate shared secret key to be used for secure communication
        KeyAgreement ka1 = null;
        try {
            ka1 = KeyAgreement.getInstance("ECDH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            ka1.init(keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        //get public key of other key pair here
        //i.e. public key of other party communicating with
        try {
            ka1.doPhase(keyPair2.getPublic(), true);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }



        //generate secret key
        byte[] secret1 = ka1.generateSecret();

        KeyAgreement ka2 = null;
        try {
            ka2 = KeyAgreement.getInstance("ECDH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            ka2.init(keyPair2.getPrivate());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            ka2.doPhase(keyPair.getPublic(), true);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        byte[] secret2 = ka2.generateSecret();


        TextView top = (TextView) findViewById(R.id.textView);
        TextView bot = (TextView) findViewById(R.id.textView2);

      //  top.setText(secret1.toString());
      //  bot.setText(secret2.toString());

        if(Arrays.equals(secret1, secret2)){
            top.setText("Keys Same: True");
        }else{
            top.setText("Keys Same: False");
        }
        TextView equalText = (TextView) findViewById(R.id.equalsView);


    }
}
