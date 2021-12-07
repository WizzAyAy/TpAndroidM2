package com.example.tp1;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Class qui represente l'activitée qui est lancé quand la connection bluetooth est activé
 */

public class ConnectedDeviceActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "7f74fac41ad7478d968d3f1749c47d9e";

    //notification
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManager;

    private Context context;
    private Button goBackButton;
    private Button speedUpButton;
    private Button slowDownButton;
    private Button saveSpeedButton;
    private Button emptyBoxButton;
    private TextView nameView;
    private TextView addressView;
    private TextView delayView;
    private TextView delaySavedView;
    private ImageView imageView;
    private CanvasView temperatureView;
    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice pairedDevice;
    private GattCallBack gattCallBack;
    private CustomHandler customHandler;
    private int delaySpeed;
    private int delaySpeedSaved;
    private BluetoothGatt m_gatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
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
        delayView = findViewById(R.id.delay_time);
        delaySavedView = findViewById(R.id.delay_time_saved);
        imageView = findViewById(R.id.iamge_box);

        imageView.setVisibility(View.INVISIBLE);

        temperatureView = findViewById(R.id.view_temp);

        nameView.setText("Nom : " + name);
        addressView.setText("Adress Mac : " + address);

        goBackButton = findViewById(R.id.button_go_back);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_gatt.close();
                CharSequence text = "deconnecté de " + name;
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                finish();
            }
        });

        customHandler = new CustomHandler();

        // MANAGE BLUETOOTH
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevice = mBtAdapter.getRemoteDevice(address);
        gattCallBack = new GattCallBack(customHandler, name, address);
        pairedDevice.connectGatt(this, false, gattCallBack);

        // permet d'acceler le rythme de prise de mesure
        speedUpButton = findViewById(R.id.button_speed_up);
        speedUpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (delaySpeed <= 100) {
                    CharSequence text = "Vitesse maximum atteinte";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                } else {
                    gattCallBack.sendMessage("-");
                }
            }
        });

        // permet de ralentir le rythme de prise de mesure
        slowDownButton = findViewById(R.id.button_slow_down);
        slowDownButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gattCallBack.sendMessage("s");
            }
        });

        // permet de sauvegarder le rythme de prise de mesure (pour le prochain démarage de l'arduino)
        saveSpeedButton = findViewById(R.id.button_save_speed);
        saveSpeedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gattCallBack.sendMessage("m");
            }
        });

        // permet de vider la boite aux lettres
        emptyBoxButton = findViewById(R.id.button_empty_box);
        emptyBoxButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gattCallBack.sendMessage("r");
            }
        });

        Intent notificationIntent = new Intent(ConnectedDeviceActivity.this, ConnectedDeviceActivity.class);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultIntent = PendingIntent.getActivity(ConnectedDeviceActivity.this, 0, notificationIntent, 0);

        // notification builder
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.box_foreground)
                .setContentTitle("Courrier dans la boite de " + name + " !")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultIntent)
                .setAutoCancel(true);

        notificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_gatt.close();
    }

    class GattCallBack extends BluetoothGattCallback {
        private final String name;
        private final String address;
        private final CustomHandler customHandler;

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
            if (serviceHM10 == null) return;
            characteristicHM10 = serviceHM10.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
            if (characteristicHM10 == null) return;
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

        public void sendMessage(String message) {
            Log.i("WRITE", message);
            byte[] byteArrray = message.getBytes();
            characteristicHM10.setValue(byteArrray);
            m_gatt.writeCharacteristic(characteristicHM10);
        }
    }

    class CustomHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            /**
             * PROTOCOLE
             * key=value
             * key value possible : temp temperature en degres celsius
             * key value possible : boite 1 (courrier) 0 (rien)
             * key value possible : delay ms entre chaque prise
             * key value possible : saved ms entre chaque prise
             */

            String valuesString = msg.getData().getString("value");
            String[] valuesList = valuesString.split(",");

            for (String value : valuesList) {
                Log.i("value", value);

                if (value.startsWith("temp=")) {
                    float temp = Float.parseFloat(value.substring(5));
                    temperatureView.addTemperature(temp);
                    temperatureView.invalidate();
                }

                if (value.startsWith("boite=")) {
                    int box = Integer.parseInt(value.substring(6));
                    if (box == 0) {
                        temperatureView.setBoxContent(false);
                        imageView.setVisibility(View.INVISIBLE);

                        builder.setContentText("Le facteur n'est pas encore passé");
                    } else {
                        temperatureView.setBoxContent(true);
                        imageView.setVisibility(View.VISIBLE);

                        builder.setContentText("Le facteur est passé");
                    }
                    notificationManager.notify(1, builder.build());
                    temperatureView.invalidate();
                }

                if (value.startsWith("delay=")) {
                    delaySpeed = Integer.parseInt(value.substring(6));
                    delayView.setText("speed : " + delaySpeed);
                }

                if (value.startsWith("saved=")) {
                    delaySpeedSaved = Integer.parseInt(value.substring(6));
                    delaySavedView.setText("saved speed : " + delaySpeedSaved);
                }
            }
        }
    }
}
