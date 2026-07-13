package com.ali.smartgarden.models;

public class Status {

    private boolean online;

    private boolean relay;

    private String version;

    private String last_seen;

    private String last_sensor_read;

    private String last_watering;

    private String watering_state;

    private String last_error;

    private long restart_count;

    private long cooldown_remaining;

    public Status() {
        // Firebase
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isRelay() {
        return relay;
    }

    public String getVersion() {
        return version;
    }

    public String getLastSeen() {
        return last_seen;
    }

    public String getLastSensorRead() {
        return last_sensor_read;
    }

    public String getLastWatering() {
        return last_watering;
    }

    public String getWateringState() {
        return watering_state;
    }

    public String getLastError() {
        return last_error;
    }

    public long getRestartCount() {
        return restart_count;
    }

    public long getCooldownRemaining() {
        return cooldown_remaining;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setRelay(boolean relay) {
        this.relay = relay;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setLastSeen(String last_seen) {
        this.last_seen = last_seen;
    }

    public void setLastSensorRead(String last_sensor_read) {
        this.last_sensor_read = last_sensor_read;
    }

    public void setLastWatering(String last_watering) {
        this.last_watering = last_watering;
    }

    public void setWateringState(String watering_state) {
        this.watering_state = watering_state;
    }

    public void setLastError(String last_error) {
        this.last_error = last_error;
    }

    public void setRestartCount(long restart_count) {
        this.restart_count = restart_count;
    }

    public void setCooldownRemaining(long cooldown_remaining) {
        this.cooldown_remaining = cooldown_remaining;
    }
}