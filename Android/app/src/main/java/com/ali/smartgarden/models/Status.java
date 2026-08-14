package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;
public class Status {

    private boolean online;

    private boolean relay;

    private String version;

    private String last_seen;

    private String last_sensor_read;

    private String last_watering;

    private String watering_state;

    private String last_error;

    private String error_incident_id;

    private long restart_count;

    private long cooldown_remaining;

    private long last_seen_epoch;
    private String active_valve_id = "";
    private boolean valve_open;
    private String valve_mode = "";

    public Status() {
        // Firebase
    }

    @PropertyName("last_seen_epoch")
    public long getLastSeenEpoch() {
        return last_seen_epoch;
    }

    @PropertyName("last_seen_epoch")
    public void setLastSeenEpoch(long last_seen_epoch) {
        this.last_seen_epoch = last_seen_epoch;
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

    @PropertyName("last_seen")
    public String getLastSeen() {
        return last_seen;
    }

    @PropertyName("last_seen")
    public void setLastSeen(String last_seen) {
        this.last_seen = last_seen;
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

    @PropertyName("error_incident_id")
    public String getErrorIncidentId() {
        return error_incident_id;
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

    @PropertyName("error_incident_id")
    public void setErrorIncidentId(String errorIncidentId) {
        this.error_incident_id = errorIncidentId;
    }

    public void setRestartCount(long restart_count) {
        this.restart_count = restart_count;
    }

    public void setCooldownRemaining(long cooldown_remaining) {
        this.cooldown_remaining = cooldown_remaining;
    }

    @PropertyName("active_valve_id")
    public String getActiveValveId() {
        return active_valve_id;
    }

    @PropertyName("active_valve_id")
    public void setActiveValveId(String activeValveId) {
        this.active_valve_id = activeValveId;
    }

    @PropertyName("valve_open")
    public boolean isValveOpen() {
        return valve_open;
    }

    @PropertyName("valve_open")
    public void setValveOpen(boolean valveOpen) {
        this.valve_open = valveOpen;
    }

    @PropertyName("valve_mode")
    public String getValveMode() {
        return valve_mode;
    }

    @PropertyName("valve_mode")
    public void setValveMode(String valveMode) {
        this.valve_mode = valveMode;
    }
}
