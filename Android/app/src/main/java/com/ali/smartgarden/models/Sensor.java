package com.ali.smartgarden.models;

public class Sensor {

    private long raw;

    private double voltage;

    private long moisture;

    public Sensor() {
        // Firebase
    }

    public long getRaw() {
        return raw;
    }

    public double getVoltage() {
        return voltage;
    }

    public long getMoisture() {
        return moisture;
    }

    public void setRaw(long raw) {
        this.raw = raw;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public void setMoisture(long moisture) {
        this.moisture = moisture;
    }
}
