package com.example.ecdhkeyagreement.communication;

// Defines several constants used when transmitting messages between the
// service and the UI.

public class DHState {
    public static final int INITIAL = 0;
    public static final int WAIT_FOR_OK = 1; // transfer normal messages
    public static final int MESSAGE_TOAST = 2;
    public static final int CLIENT_SEND_KEY = 3;
    public static final int SERVER_SEND_KEY = 4;
    public static final int KEY_GENERATED = 5; // key generation finished
    public static final int MESSAGE_FILE = 6; // transfer files

    // ... (Add other message types here as needed.)
}
