package com.alidogukan.avora.models;

import com.google.firebase.database.PropertyName;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
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
    public void setLastSeenEpoch(long lastSeenEpoch) {
        this.last_seen_epoch = lastSeenEpoch;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isRelay() {
        return relay;
    }

    public void setRelay(boolean relay) {
        this.relay = relay;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @PropertyName("last_seen")
    public String getLastSeen() {
        return last_seen;
    }

    @PropertyName("last_seen")
    public void setLastSeen(String lastSeen) {
        this.last_seen = lastSeen;
    }

    @PropertyName("last_sensor_read")
    public String getLastSensorRead() {
        return last_sensor_read;
    }

    @PropertyName("last_sensor_read")
    public void setLastSensorRead(String lastSensorRead) {
        this.last_sensor_read = lastSensorRead;
    }

    @PropertyName("last_watering")
    public String getLastWatering() {
        return last_watering;
    }

    @PropertyName("last_watering")
    public void setLastWatering(String lastWatering) {
        this.last_watering = lastWatering;
    }

    @PropertyName("watering_state")
    public String getWateringState() {
        return watering_state;
    }

    @PropertyName("watering_state")
    public void setWateringState(String wateringState) {
        this.watering_state = wateringState;
    }

    @PropertyName("last_error")
    public String getLastError() {
        return last_error;
    }

    @PropertyName("last_error")
    public void setLastError(String lastError) {
        this.last_error = lastError;
    }

    @PropertyName("error_incident_id")
    public String getErrorIncidentId() {
        return error_incident_id;
    }

    @PropertyName("error_incident_id")
    public void setErrorIncidentId(String errorIncidentId) {
        this.error_incident_id = errorIncidentId;
    }

    @PropertyName("restart_count")
    public long getRestartCount() {
        return restart_count;
    }

    @PropertyName("restart_count")
    public void setRestartCount(long restartCount) {
        this.restart_count = restartCount;
    }

    @PropertyName("cooldown_remaining")
    public long getCooldownRemaining() {
        return cooldown_remaining;
    }

    @PropertyName("cooldown_remaining")
    public void setCooldownRemaining(long cooldownRemaining) {
        this.cooldown_remaining = cooldownRemaining;
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