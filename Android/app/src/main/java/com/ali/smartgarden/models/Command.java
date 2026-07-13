package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

public class Command {

    private boolean auto_mode;

    private boolean relay;

    private boolean enabled;

    private long moisture_limit;

    private long pump_duration;

    private long restart_delta;

    private long cooldown_seconds;

    public Command() {
        // Firebase Realtime Database için boş constructor zorunlu.
    }

    @PropertyName("auto_mode")
    public boolean isAutoMode() {
        return auto_mode;
    }

    @PropertyName("auto_mode")
    public void setAutoMode(boolean auto_mode) {
        this.auto_mode = auto_mode;
    }

    public boolean isRelay() {
        return relay;
    }

    public void setRelay(boolean relay) {
        this.relay = relay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PropertyName("moisture_limit")
    public long getMoistureLimit() {
        return moisture_limit;
    }

    @PropertyName("moisture_limit")
    public void setMoistureLimit(long moisture_limit) {
        this.moisture_limit = moisture_limit;
    }

    @PropertyName("pump_duration")
    public long getPumpDuration() {
        return pump_duration;
    }

    @PropertyName("pump_duration")
    public void setPumpDuration(long pump_duration) {
        this.pump_duration = pump_duration;
    }

    @PropertyName("restart_delta")
    public long getRestartDelta() {
        return restart_delta;
    }

    @PropertyName("restart_delta")
    public void setRestartDelta(long restart_delta) {
        this.restart_delta = restart_delta;
    }

    @PropertyName("cooldown_seconds")
    public long getCooldownSeconds() {
        return cooldown_seconds;
    }

    @PropertyName("cooldown_seconds")
    public void setCooldownSeconds(long cooldown_seconds) {
        this.cooldown_seconds = cooldown_seconds;
    }
}