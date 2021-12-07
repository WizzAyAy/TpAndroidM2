package com.example.tp1;

import java.util.Objects;

/**
 * Class qui represente un object bluetooth
 */

public class Device {
    public String name;
    public String mac;

    public Device(String name, String mac) {
        this.name = name;
        this.mac = mac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(name, device.name) && Objects.equals(mac, device.mac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mac);
    }
}
