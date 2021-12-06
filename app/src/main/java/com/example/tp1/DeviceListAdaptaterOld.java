package com.example.tp1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class DeviceListAdaptaterOld extends ArrayAdapter<Device> {
    private Context context;

    public DeviceListAdaptaterOld(@NonNull Context context, int resource) {
        super(context, resource);

        this.context = context;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_layout, parent, false);
        }

        Device device = getItem(position);

        TextView deviceName = (TextView) convertView.findViewById(R.id.device_name);
        TextView deviceMac = (TextView) convertView.findViewById(R.id.device_mac);

        Button connect = (Button) convertView.findViewById(R.id.button_connect);

        deviceName.setText(device.name);
        deviceMac.setText(device.mac);

        return  convertView;
    }

    public void CustomAdd(Device device){
        if (device.name == null || device.mac == null) return;
        for(int i = 0; i < getCount(); i++)
            if (getItem(i).equals(device)) return;
        add(device);
    }

    public void clearDevices(){
        clear();
    }
}
