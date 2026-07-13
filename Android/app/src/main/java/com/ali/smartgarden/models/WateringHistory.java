package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

public class WateringHistory {

    private String recordId;

    private String startedAt;
    private String finishedAt;

    private long duration;

    private long moistureBefore;
    private long moistureAfter;
    private long moistureDelta;

    private long moistureLimit;
    private long restartDelta;
    private long cooldownSeconds;

    private boolean completed;

    private String stopReason;
    private String mode;
    private String firmware;

    /**
     * Firebase Realtime Database için boş constructor zorunludur.
     */
    public WateringHistory() {
        startedAt = "";
        finishedAt = "";
        stopReason = "";
        mode = "";
        firmware = "";
        recordId = "";
    }

    /**
     * Bu alan Firebase kaydının anahtarını tutacak.
     * Veritabanındaki nesnenin içinde bulunmaz; ViewModel tarafından atanır.
     */
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    @PropertyName("started_at")
    public String getStartedAt() {
        return startedAt;
    }

    @PropertyName("started_at")
    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    @PropertyName("finished_at")
    public String getFinishedAt() {
        return finishedAt;
    }

    @PropertyName("finished_at")
    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    @PropertyName("duration")
    public long getDuration() {
        return duration;
    }

    @PropertyName("duration")
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @PropertyName("moisture_before")
    public long getMoistureBefore() {
        return moistureBefore;
    }

    @PropertyName("moisture_before")
    public void setMoistureBefore(long moistureBefore) {
        this.moistureBefore = moistureBefore;
    }

    @PropertyName("moisture_after")
    public long getMoistureAfter() {
        return moistureAfter;
    }

    @PropertyName("moisture_after")
    public void setMoistureAfter(long moistureAfter) {
        this.moistureAfter = moistureAfter;
    }

    @PropertyName("moisture_delta")
    public long getMoistureDelta() {
        return moistureDelta;
    }

    @PropertyName("moisture_delta")
    public void setMoistureDelta(long moistureDelta) {
        this.moistureDelta = moistureDelta;
    }

    @PropertyName("moisture_limit")
    public long getMoistureLimit() {
        return moistureLimit;
    }

    @PropertyName("moisture_limit")
    public void setMoistureLimit(long moistureLimit) {
        this.moistureLimit = moistureLimit;
    }

    @PropertyName("restart_delta")
    public long getRestartDelta() {
        return restartDelta;
    }

    @PropertyName("restart_delta")
    public void setRestartDelta(long restartDelta) {
        this.restartDelta = restartDelta;
    }

    @PropertyName("cooldown_seconds")
    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    @PropertyName("cooldown_seconds")
    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    @PropertyName("completed")
    public boolean isCompleted() {
        return completed;
    }

    @PropertyName("completed")
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @PropertyName("stop_reason")
    public String getStopReason() {
        return stopReason;
    }

    @PropertyName("stop_reason")
    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    @PropertyName("mode")
    public String getMode() {
        return mode;
    }

    @PropertyName("mode")
    public void setMode(String mode) {
        this.mode = mode;
    }

    @PropertyName("firmware")
    public String getFirmware() {
        return firmware;
    }

    @PropertyName("firmware")
    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }
}