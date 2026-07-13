package com.ali.smartgarden.models;

public class WateringHistory {

    private String started_at;

    private String finished_at;

    private long duration;

    private long moisture_before;

    private long moisture_after;

    private long moisture_delta;

    private long moisture_limit;

    private long restart_delta;

    private long cooldown_seconds;

    private boolean completed;

    private String stop_reason;

    private String mode;

    private String firmware;

    public WateringHistory() {
        // Firebase
    }

    public String getStartedAt() {
        return started_at;
    }

    public String getFinishedAt() {
        return finished_at;
    }

    public long getDuration() {
        return duration;
    }

    public long getMoistureBefore() {
        return moisture_before;
    }

    public long getMoistureAfter() {
        return moisture_after;
    }

    public long getMoistureDelta() {
        return moisture_delta;
    }

    public long getMoistureLimit() {
        return moisture_limit;
    }

    public long getRestartDelta() {
        return restart_delta;
    }

    public long getCooldownSeconds() {
        return cooldown_seconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getStopReason() {
        return stop_reason;
    }

    public String getMode() {
        return mode;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setStartedAt(String started_at) {
        this.started_at = started_at;
    }

    public void setFinishedAt(String finished_at) {
        this.finished_at = finished_at;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setMoistureBefore(long moisture_before) {
        this.moisture_before = moisture_before;
    }

    public void setMoistureAfter(long moisture_after) {
        this.moisture_after = moisture_after;
    }

    public void setMoistureDelta(long moisture_delta) {
        this.moisture_delta = moisture_delta;
    }

    public void setMoistureLimit(long moisture_limit) {
        this.moisture_limit = moisture_limit;
    }

    public void setRestartDelta(long restart_delta) {
        this.restart_delta = restart_delta;
    }

    public void setCooldownSeconds(long cooldown_seconds) {
        this.cooldown_seconds = cooldown_seconds;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setStopReason(String stop_reason) {
        this.stop_reason = stop_reason;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }
}