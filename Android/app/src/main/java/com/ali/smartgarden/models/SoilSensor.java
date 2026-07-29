package com.ali.smartgarden.models;


public class SoilSensor {

    private int raw;
    private double voltage;
    private int moisture;

    private String sensor_id;
    private String firmware;

    private int rssi;
    private long uptime_seconds;

    private String updated_at;
    private long updated_at_epoch;

    public SoilSensor() {
        // Firebase için boş constructor
    }


    public int getRaw() {
        return raw;
    }


    public double getVoltage() {
        return voltage;
    }


    public int getMoisture() {
        return moisture;
    }


    public String getSensor_id() {
        return sensor_id;
    }


    public String getFirmware() {
        return firmware;
    }

    public String getUpdated_at() {

        return updated_at;
    }

    public long getUpdated_at_epoch() {

        return updated_at_epoch;
    }

    public int getRssi() {
        return rssi;
    }

    public long getUptime_seconds() {
        return uptime_seconds;
    }


}
