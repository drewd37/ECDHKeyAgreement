package com.example.ecdhkeyagreement.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.ecdhkeyagreement.crypto.CryptoManager;
import com.example.ecdhkeyagreement.crypto.CryptoUtil;
import com.example.ecdhkeyagreement.file.FileUtil;
import com.example.ecdhkeyagreement.key.KeyGenerator;
import com.example.ecdhkeyagreement.key.KeyManager;
import com.example.ecdhkeyagreement.key.KeyUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {

    private String role; // server / client

    private final UUID B_UUID = UUID.fromString("00002415-0000-1000-8000-00805F9B34FB");

    public CommunicationThread communicationThread;


    private BluetoothAdapter bluetoothAdapter;
    private static final Set<String> DEVICE_ADDRS = new HashSet<>(Arrays.asList("7C:D3:0A:0D:5A:36", "98:D6:F7:BC:60:18")); // O&P, Nexus 4
    private BluetoothDevice bluetoothDevice;

    public BluetoothManager(BluetoothAdapter adapter) {
        this.bluetoothAdapter = adapter;
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start(); // start listening
    }

    public boolean activeConnect() {
        bluetoothDevice = getBondedDevices();
        if (bluetoothDevice != null) {
            ConnectThread thread = new ConnectThread(bluetoothDevice);
            thread.start();
            return true;
        }
        else
            return false;
    }

    // search for target device in paired list
    public BluetoothDevice getBondedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (DEVICE_ADDRS.contains(deviceHardwareAddress))
                    return device;
            }
        }
        return null;
    }

    public String getDeviceName() {
        return bluetoothDevice.getName();
    }

    public String getRole() {
        return role;
    }

    // invoke this function to send Bluetooth data
    public void sendBlueToothData(byte[] data) {
        communicationThread.write(data);
    }





    // server thread
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String TAG = "ServerThread";

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Bluetooth", B_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.i(TAG, "Successfully connected");
                    role = "server";
                    communicationThread = new CommunicationThread(socket);
                    communicationThread.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the connect socket", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    // client thread, initiate Bluetooth connection
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String TAG = "ClientThread";

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(B_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            role = "client";
            communicationThread = new CommunicationThread(mmSocket);
            communicationThread.start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }



    // thread for communication
    public class CommunicationThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream
        public int currentState; // current state
        String TAG = "CommunicationThread";


        public CommunicationThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            currentState = DHState.INITIAL;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        private PublicKey byteArrayToPublicKey(byte[] key) {
            KeyFactory factory;
            try {
                factory = KeyFactory.getInstance("EC");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }

            PublicKey publicKey;
            try {
                publicKey = (ECPublicKey) factory.generatePublic(new X509EncodedKeySpec(key));
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
                return null;
            }
            return publicKey;
        }

        public void run() {
            mmBuffer = new byte[1024];

            // set up a timer to check timeout after 10 seconds
            Looper.prepare();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // resend messages after xx ms, deal with message lost
                    if (currentState == DHState.INITIAL) {
                        if (role.equals("client"))
                            write(KeyManager.getInstance().getKeyPair().getPublic().getEncoded());
                    }
                    else if (currentState == DHState.WAIT_FOR_OK) {
                        if (role.equals("server"))
                            write(KeyManager.getInstance().getKeyPair().getPublic().getEncoded());
                    }
                    else {
                        // key generated
                    }
                }
            }, 10000);

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    System.out.println("Start reading incoming data");
                    int len = mmInStream.read(mmBuffer);

                    switch (currentState) {
                        case DHState.INITIAL:
                            byte[] data = Arrays.copyOf(mmBuffer, len);
                            System.out.println(TAG + KeyUtil.byteArrayToHex(data));

                            if (role.equals("server")) {
                                // server receives the public key from client
                                PublicKey publicKey = byteArrayToPublicKey(data);
                                if (publicKey == null)
                                    continue;

                                System.out.println("Received key from client: " + KeyUtil.byteArrayToHex(publicKey.getEncoded()));

                                // generate dh key
                                KeyManager.getInstance().calculateDHKey(publicKey);
                                System.out.println("Secret DH key: " + KeyUtil.byteArrayToHex(KeyManager.getInstance().getDHKey()));


                                // server send its public key to client, and waits for the OK response
                                write(KeyManager.getInstance().getKeyPair().getPublic().getEncoded());
                                currentState = DHState.WAIT_FOR_OK;
                            } else {
                                // client receives the public key from server
                                PublicKey publicKey = byteArrayToPublicKey(data);
                                if (publicKey == null)
                                    continue;

                                System.out.println("Received key from server: " + KeyUtil.byteArrayToHex(publicKey.getEncoded()));

                                // generate dh key
                                KeyManager.getInstance().calculateDHKey(publicKey);
                                System.out.println("Secret DH key: " + KeyUtil.byteArrayToHex(KeyManager.getInstance().getDHKey()));

                                // send ok to server
                                write("OK".getBytes());
                                currentState = DHState.KEY_GENERATED;
                            }
                            break;

                        case DHState.WAIT_FOR_OK:
                            // server receives OK from client
                            data = Arrays.copyOf(mmBuffer, len);
                            if (Arrays.equals(data, "OK".getBytes()))
                                currentState = DHState.KEY_GENERATED;
                            break;

                        case DHState.KEY_GENERATED:
                            // receive file
                            byte[] fileData = new byte[1024];
                            if (len >= 1024) {
                                // large file
                                int l;
                                byte[] mybytearray = new byte[1024];
                                DataInputStream dis = new DataInputStream(mmInStream);
                                while ((l = dis.read(mybytearray)) != -1) {
                                    KeyUtil.byteAppend(fileData, mybytearray);
                                }
                            }
                            else {
                                fileData = Arrays.copyOf(mmBuffer, len);
                            }

                            File file = new File(Environment.getExternalStorageDirectory() + "/Download/","Received_file");
                            FileOutputStream fos = new FileOutputStream(file);

                            // decrypt
                            byte[] decryptData = CryptoManager.getInstance().decrpyt(fileData);
                            if (decryptData == null)
                                return;

                            // exclude nonce and digest
                            int finalDataLen = decryptData.length - 32 - 4;
                            byte[] digest = Arrays.copyOfRange(decryptData, decryptData.length - 32, decryptData.length);
                            byte[] dataAndNonce = Arrays.copyOfRange(decryptData, 0, decryptData.length - 32);
                            byte[] nonce = Arrays.copyOfRange(decryptData, finalDataLen, decryptData.length - 32);
                            byte[] finalData = Arrays.copyOfRange(decryptData, 0, finalDataLen);

                            // integrity check
                            byte[] calculatedDigest = CryptoUtil.sha256(dataAndNonce);
                            if (!Arrays.equals(digest, calculatedDigest)) {
                                System.out.println("Integrity check failed, discard file...");
                                currentState = DHState.INITIAL;
                                continue;
                            }

                            // check nonce
                            if (CryptoManager.getInstance().checkNonce(nonce))
                                CryptoManager.getInstance().addNonce(nonce);
                            else {
                                System.out.println("Reused nonce, discard file...");
                                currentState = DHState.INITIAL;
                                continue;
                            }

                            fos.write(finalData);
                            fos.close();
                            System.out.println(TAG + KeyUtil.byteArrayToHex(decryptData));

                            // generate new DH Key

                            if (role.equals("server")) {
                                // regenerate DH key
                                currentState = DHState.INITIAL;
                                KeyGenerator keyGenerator = new KeyGenerator();
                                keyGenerator.generateKeyPair();
                                KeyPair keyPair = keyGenerator.getKeyPair();
                                KeyManager.getInstance().setKeyPair(keyPair);
                            }
                            else {
                                // regenerate DH key
                                currentState = DHState.INITIAL;
                                KeyGenerator keyGenerator = new KeyGenerator();
                                keyGenerator.generateKeyPair();
                                KeyPair keyPair = keyGenerator.getKeyPair();
                                KeyManager.getInstance().setKeyPair(keyPair);

                                // client initiate key negotiation
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    return;
                                }
                                System.out.println("Sent to server: " + KeyUtil.byteArrayToHex(KeyManager.getInstance().getKeyPair().getPublic().getEncoded()));
                                write(KeyManager.getInstance().getKeyPair().getPublic().getEncoded());
                            }
                            break;

                    }
                    // Send the obtained bytes to the UI activity.
//                    Message readMsg = handler.obtainMessage(
//                            DHState.MESSAGE_READ, numBytes, -1, mmBuffer);
//                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();

                // Share the sent message with the UI activity.
//                Message writtenMsg = handler.obtainMessage(
//                        type, -1, -1, mmBuffer);
//                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
//                Message writeErrorMsg =
//                        handler.obtainMessage(DHState.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast",
//                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


}
