package com.example.tp1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.internal.SafeIterableMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ConnectedDeviceActivity extends AppCompatActivity {

    private Button goBackButton;
    private TextView nameView;
    private TextView addressView;

    private Button getTemperature;

    private View temperatureView;

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice pairedDevice;

    private GattCallBack gattCallBack;

    private CustomHandler customHandler;

    private ArrayList<Float> temperatures = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_device);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name") != null
                ? intent.getStringExtra("name")
                : "NoName";
        String address = intent.getStringExtra("mac");

        Log.i("ConnectedDeviceActivity", intent.getStringExtra("name"));
        Log.i("ConnectedDeviceActivity", address);

        nameView = findViewById(R.id.connected_name);
        addressView = findViewById(R.id.connected_mac);

        temperatureView = findViewById(R.id.view_temp);

        nameView.setText("Nom : " + name);
        addressView.setText("Adress Mac : "  +address);

        goBackButton = findViewById(R.id.button_go_back);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        customHandler = new CustomHandler();

        // MANAGE BLUETOOTH
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevice = mBtAdapter.getRemoteDevice(address);
        gattCallBack = new GattCallBack(customHandler, name, address);
        pairedDevice.connectGatt(this, false, gattCallBack);

        getTemperature = findViewById(R.id.button_get_tmp);
        getTemperature.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gattCallBack.sendMessage("e");
            }
        });

    }

    class GattCallBack extends BluetoothGattCallback {
        private CustomHandler customHandler;
        private final String name;
        private final String address;

        private BluetoothGatt m_gatt;

        private BluetoothGattService serviceHM10;
        private BluetoothGattCharacteristic characteristicHM10;

        public GattCallBack(CustomHandler customHandler, String name, String address) {
            super();
            this.customHandler = customHandler;
            this.name = name;
            this.address = address;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("STATE", "STATE_CONNECTED");
                m_gatt = gatt;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("STATE", "STATE_DISCONNECTED");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            serviceHM10 = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            if(serviceHM10 == null) return;
            characteristicHM10 = serviceHM10.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
            if(characteristicHM10 == null) return;
            gatt.setCharacteristicNotification(characteristicHM10, true);
            characteristicHM10.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            Message m = customHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("value", value);
            m.setData(b);
            customHandler.sendMessage(m);
        }

        public void sendMessage(String message){
            Log.i("WRITE", message);
            byte[] byteArrray = message.getBytes();
            characteristicHM10.setValue(byteArrray);
            m_gatt.writeCharacteristic(characteristicHM10);
        }
    }

    class CustomHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            String valuesString = msg.getData().getString("value");
            List<String> valuesList = Arrays.asList(valuesString.split(","));

            for (String value : valuesList) {
                if (value.startsWith("temp=")) {
                    temperatures.add(Float.parseFloat(value.substring(5)));
                    temperatureView.invalidate();
                }
            }
        }
    }
}
