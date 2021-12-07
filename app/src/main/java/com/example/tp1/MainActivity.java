package com.example.tp1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

/**
 * Activitée pricipale
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 0;
    private static final String CHANNEL_ID = "7f74fac41ad7478d968d3f1749c47d9e";

    private Button buttonSearching;
    private Button buttonCancelSearching;

    private BluetoothAdapter mBtAdapter;
    private MBtBroadCastReceiver mBtBroadCastReceiver;
    private BluetoothAdapter mBluetoothAdapter;

    private Btle_scan_callback btle_scan_callback;
    private BluetoothLeScanner btle_scanner;

    private DeviceListAdaptater adapter;

    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice pairedDevice;

    private GattCallBack gattCallBack;

    private CustomHandler customHandler;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.list_devices);
        setTitle("Scan Bluetooth");

        getPermissions();

        buttonSearching = findViewById(R.id.button_searching);
        buttonCancelSearching = findViewById(R.id.button_cancel_searching);
        buttonCancelSearching.setEnabled(false);

        addListnerBoutton();

        adapter = new DeviceListAdaptater(this, 0);

        listView.setAdapter(adapter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtBroadCastReceiver = new MBtBroadCastReceiver();

        // handle ble connection
        btle_scan_callback = new Btle_scan_callback();
        btle_scanner = mBluetoothAdapter.getBluetoothLeScanner();

        customHandler = new CustomHandler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mBtBroadCastReceiver, intentFilter);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void addListnerBoutton() {
        buttonSearching.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSearching.setEnabled(false);
                buttonCancelSearching.setEnabled(true);

                getAllDevices();

                btle_scanner.startScan(btle_scan_callback);
//                mBtAdapter.startDiscovery();
            }
        });

        buttonCancelSearching.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSearching.setEnabled(true);
                buttonCancelSearching.setEnabled(false);

                adapter.clearDevices();
                btle_scanner.stopScan(btle_scan_callback);
//                mBtAdapter.cancelDiscovery();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btle_scanner.stopScan(btle_scan_callback);
//        mBtAdapter.cancelDiscovery();
    }

    /**
     * return tous les devices deja connect et verif si on a le blue activé
     *
     * @return ArrayList
     */
    protected ArrayList getAllDevices() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // tous les appareils déjà connectés
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        ArrayList<Device> devices = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                devices.add(new Device(deviceName, deviceHardwareAddress));
            }
            for (Device device : devices) {
                adapter.CustomAdd(device);
            }
        }
        return devices;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
    }

    public void connectToGatt(String name, String address) {
        pairedDevice = mBtAdapter.getRemoteDevice(address);
        gattCallBack = new GattCallBack(customHandler, name, address);
        pairedDevice.connectGatt(this, false, gattCallBack);
    }

    public void disconnectToGatt(String address) {
        // TODO
    }

    // bluetooth 2.0
    class MBtBroadCastReceiver extends BroadcastReceiver {

        private static final String TAG = "MBtBroadCastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.i(TAG, "revice " + action);

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "onReceive: " + "Vous avez changé le mode de bluetooth");
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i(TAG, "onReceive: " + "Find a new device");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                adapter.CustomAdd(new Device(deviceName, deviceHardwareAddress));
            }
        }
    }

    // bluetooth le
    class Btle_scan_callback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            adapter.CustomAdd(new Device(device.getName(), device.getAddress()));
        }
    }

    class GattCallBack extends BluetoothGattCallback {
        private final String name;
        private final String address;
        private final CustomHandler handler;

        public GattCallBack(CustomHandler handler, String name, String address) {
            super();
            this.handler = handler;
            this.name = name;
            this.address = address;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            int code = -1;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("STATE", "STATE_CONNECTED");
                code = BluetoothProfile.STATE_CONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("STATE", "STATE_DISCONNECTED");
                code = BluetoothProfile.STATE_DISCONNECTED;
            }

            Message m = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("code", code);
            b.putString("name", name);
            b.putString("address", address);
            m.setData(b);
            handler.sendMessage(m);
        }
    }

    // liste des devices
    class DeviceListAdaptater extends ArrayAdapter<Device> {

        private final Context context;
        private final CustomHandler customHandler;
        private ViewGroup parent;

        public DeviceListAdaptater(@NonNull Context context, int resource) {
            super(context, resource);
            this.customHandler = new CustomHandler();
            this.context = context;
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            this.parent = parent;
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_layout, parent, false);
            }

            Device device = getItem(position);

            TextView deviceName = convertView.findViewById(R.id.device_name);
            TextView deviceMac = convertView.findViewById(R.id.device_mac);

            Button connect = convertView.findViewById(R.id.button_connect);
            Button disconnect = convertView.findViewById(R.id.button_disconnect);

            connect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i("deviceName", (String) deviceName.getText());
                    Log.i("deviceMac", (String) deviceMac.getText());

                    connectToGatt((String) deviceName.getText(), (String) deviceMac.getText());
                    disconnect.setEnabled(true);
                    connect.setEnabled(false);
                }
            });

            disconnect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i("deviceName", (String) deviceName.getText());
                    Log.i("deviceMac", (String) deviceMac.getText());

                    disconnectToGatt((String) deviceMac.getText());
                    disconnect.setEnabled(false);
                    connect.setEnabled(true);
                }
            });

            deviceName.setText(device.name);
            deviceMac.setText(device.mac);

            return convertView;
        }

        public void CustomAdd(Device device) {
            if (device.name == null || device.mac == null) return;
            for (int i = 0; i < getCount(); i++)
                if (getItem(i).equals(device)) return;
            add(device);
        }

        public void clearDevices() {
            clear();
        }

        public void resetConnectButtonAdress(String adr) {
            Log.i("resetConnectButtonAdre", adr);
            for (int i = 0; i < getCount(); i++) {
                if (getItem(i).mac.equals(adr)) {
                    Button connect = getView(i, null, this.parent).findViewById(R.id.button_connect);
                    Button disconnect = getView(i, null, this.parent).findViewById(R.id.button_disconnect);

                    connect.setEnabled(true);
                    disconnect.setEnabled(false);
                }
            }
        }
    }

    // handler qui start une nouvelle activity si on est connecté au serveur gatt
    class CustomHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String address = msg.getData().getString("address");
            String name = msg.getData().getString("name");
            Log.i("Handler adress", address);
            if (msg.getData().getInt("code") == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Handler status", "STATE_CONNECTED");
                Intent intent = new Intent(MainActivity.this, ConnectedDeviceActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("mac", address);
                MainActivity.this.startActivity(intent);

            } else if (msg.getData().getInt("code") == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("Handler status", "STATE_DISCONNECTED");
                adapter.resetConnectButtonAdress(address);
            }
        }
    }
}