package com.example.tp1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MBtBroadCastReceiverOld extends BroadcastReceiver {

    private static final String TAG = "MBtBroadCastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.i(TAG, "revice " + action);

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            Log.i(TAG, "onReceive: " + "Vous avez changé le mode de bluetooth");
        }

        if(BluetoothDevice.ACTION_FOUND.equals(action)){
            Log.i(TAG, "onReceive: " + "Find a new device");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress();
            Log.i(TAG, "deviceName: " + deviceName);
            Log.i(TAG, "deviceHardwareAddress: " + deviceHardwareAddress);
        }
    }
}
