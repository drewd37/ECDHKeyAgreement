package com.example.ecdhkeyagreement;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ecdhkeyagreement.communication.BluetoothManager;
import com.example.ecdhkeyagreement.crypto.CryptoManager;
import com.example.ecdhkeyagreement.crypto.CryptoUtil;
import com.example.ecdhkeyagreement.file.FileUtil;
import com.example.ecdhkeyagreement.key.KeyGenerator;
import com.example.ecdhkeyagreement.key.KeyManager;
import com.example.ecdhkeyagreement.key.KeyUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyPair;


public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 7777;
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter bluetoothAdapter;
    private static final int SELECT_PICTURE = 999;
    private static final int CHOOSE_FILE_CODE = 998;
    private String filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }


        // generate DH key pair
        KeyGenerator keyGenerator = new KeyGenerator();
        keyGenerator.generateKeyPair();
        KeyPair keyPair = keyGenerator.getKeyPair();
        KeyManager.getInstance().setKeyPair(keyPair);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bluetoothManager = new BluetoothManager(bluetoothAdapter);

        Button connectButton = findViewById(R.id.button_connect);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean result = bluetoothManager.activeConnect();
                if(result) {
                    Toast.makeText(MainActivity.this, "Successfully connected with " + bluetoothManager.getDeviceName(), Toast.LENGTH_LONG).show();

                    while(bluetoothManager.communicationThread == null) {}

                    // client starts the negotiation by sending the public key
                    byte[] a = KeyManager.getInstance().getKeyPair().getPublic().getEncoded();
                    System.out.println("Sent to server: " + KeyUtil.byteArrayToHex(KeyManager.getInstance().getKeyPair().getPublic().getEncoded()));
                    bluetoothManager.sendBlueToothData(KeyManager.getInstance().getKeyPair().getPublic().getEncoded());
                }
                else
                    Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_LONG).show();
            }
        });


        final EditText editText = findViewById(R.id.input);
        Button sendButton = findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothManager.sendBlueToothData(editText.getText().toString().getBytes());
            }
        });

        Button showButton = findViewById(R.id.button_show);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText(KeyUtil.byteArrayToHex(KeyManager.getInstance().getDHKey()));
            }
        });


        final Button fileButton = findViewById(R.id.button_GetFile);
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose File"), CHOOSE_FILE_CODE);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

//                Intent intent = new Intent();
//                intent.setType("image/*");
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });

        Button sendFileButton = findViewById(R.id.button_SendFile);
        sendFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("FilePath = " + filePath);
                if (filePath == null)
                    return;

                File myFile = new File (filePath);
                byte [] mybytearray  = new byte [(int)myFile.length()];
                FileInputStream fis;
                try {
                    fis = new FileInputStream(myFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                BufferedInputStream bis = new BufferedInputStream(fis);
                try {
                    bis.read(mybytearray, 0, mybytearray.length);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // nonce
                byte[] nonce = CryptoUtil.generateNonce();
                mybytearray = KeyUtil.byteAppend(mybytearray, nonce);

                // digest
                byte[] digest = CryptoUtil.sha256(mybytearray);
                mybytearray = KeyUtil.byteAppend(mybytearray, digest);

                // encrypt
                CryptoManager manager = new CryptoManager();
                mybytearray = manager.encrypt(mybytearray);

                System.out.println("Sending file... " + KeyUtil.byteArrayToHex(mybytearray));
                bluetoothManager.sendBlueToothData(mybytearray);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
        }
        else if (requestCode == CHOOSE_FILE_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                filePath = FileUtil.getFilePathForN(uri, this);
            }
            else {
                filePath = FileUtil.getPath(this, uri);
            }
        }
        else if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            filePath = FileUtil.getFilePathForN(selectedImageUri, this);
        }
        else if (resultCode == RESULT_CANCELED)
        {

        }
    }

}
