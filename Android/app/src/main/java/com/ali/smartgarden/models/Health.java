package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

public class Health {

    private double cpuTemperature;
    private double cpuUsage;
    private double diskUsage;
    private String ipAddress;
    private boolean throttled;
    private double memoryUsage;
    private String updatedAt;
    private long uptimeSeconds;
    private long wifiSignal;
    private long throttledRaw;
    private boolean underVoltageNow;
    private boolean frequencyCappedNow;
    private boolean throttledNow;
    private boolean softTemperatureLimitNow;
    private boolean underVoltageHistory;
    private boolean frequencyCappedHistory;
    private boolean throttledHistory;
    private boolean softTemperatureLimitHistory;

    public Health() {
        ipAddress = "";
        updatedAt = "";
        firmware = "";
    }
    @PropertyName("throttled_raw")
    public long getThrottledRaw() {
        return throttledRaw;
    }

    @PropertyName("throttled_raw")
    public void setThrottledRaw(long throttledRaw) {
        this.throttledRaw = throttledRaw;
    }

    @PropertyName("under_voltage_now")
    public boolean isUnderVoltageNow() {
        return underVoltageNow;
    }

    @PropertyName("under_voltage_now")
    public void setUnderVoltageNow(boolean underVoltageNow) {
        this.underVoltageNow = underVoltageNow;
    }

    @PropertyName("frequency_capped_now")
    public boolean isFrequencyCappedNow() {
        return frequencyCappedNow;
    }

    @PropertyName("frequency_capped_now")
    public void setFrequencyCappedNow(boolean frequencyCappedNow) {
        this.frequencyCappedNow = frequencyCappedNow;
    }

    @PropertyName("throttled_now")
    public boolean isThrottledNow() {
        return throttledNow;
    }

    @PropertyName("throttled_now")
    public void setThrottledNow(boolean throttledNow) {
        this.throttledNow = throttledNow;
    }

    @PropertyName("soft_temperature_limit_now")
    public boolean isSoftTemperatureLimitNow() {
        return softTemperatureLimitNow;
    }

    @PropertyName("soft_temperature_limit_now")
    public void setSoftTemperatureLimitNow(
            boolean softTemperatureLimitNow
    ) {
        this.softTemperatureLimitNow =
                softTemperatureLimitNow;
    }

    @PropertyName("under_voltage_history")
    public boolean isUnderVoltageHistory() {
        return underVoltageHistory;
    }

    @PropertyName("under_voltage_history")
    public void setUnderVoltageHistory(
            boolean underVoltageHistory
    ) {
        this.underVoltageHistory =
                underVoltageHistory;
    }

    @PropertyName("frequency_capped_history")
    public boolean isFrequencyCappedHistory() {
        return frequencyCappedHistory;
    }

    @PropertyName("frequency_capped_history")
    public void setFrequencyCappedHistory(
            boolean frequencyCappedHistory
    ) {
        this.frequencyCappedHistory =
                frequencyCappedHistory;
    }

    @PropertyName("throttled_history")
    public boolean isThrottledHistory() {
        return throttledHistory;
    }

    @PropertyName("throttled_history")
    public void setThrottledHistory(
            boolean throttledHistory
    ) {
        this.throttledHistory =
                throttledHistory;
    }

    @PropertyName("soft_temperature_limit_history")
    public boolean isSoftTemperatureLimitHistory() {
        return softTemperatureLimitHistory;
    }

    @PropertyName("soft_temperature_limit_history")
    public void setSoftTemperatureLimitHistory(
            boolean softTemperatureLimitHistory
    ) {
        this.softTemperatureLimitHistory =
                softTemperatureLimitHistory;
    }

    @PropertyName("cpu_temperature")
    public double getCpuTemperature() {
        return cpuTemperature;
    }

    @PropertyName("cpu_temperature")
    public void setCpuTemperature(double cpuTemperature) {
        this.cpuTemperature = cpuTemperature;
    }

    @PropertyName("cpu_usage")
    public double getCpuUsage() {
        return cpuUsage;
    }

    @PropertyName("cpu_usage")
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    @PropertyName("disk_usage")
    public double getDiskUsage() {
        return diskUsage;
    }

    @PropertyName("disk_usage")
    public void setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
    }

    @PropertyName("ip_address")
    public String getIpAddress() {
        return ipAddress;
    }

    @PropertyName("ip_address")
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @PropertyName("is_throttled")
    public boolean isThrottled() {
        return throttled;
    }

    @PropertyName("is_throttled")
    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }

    @PropertyName("memory_usage")
    public double getMemoryUsage() {
        return memoryUsage;
    }

    @PropertyName("memory_usage")
    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    @PropertyName("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PropertyName("uptime_seconds")
    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    @PropertyName("uptime_seconds")
    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    @PropertyName("wifi_signal")
    public long getWifiSignal() {
        return wifiSignal;
    }

    @PropertyName("wifi_signal")
    public void setWifiSignal(long wifiSignal) {
        this.wifiSignal = wifiSignal;
    }

    private String firmware;

    @PropertyName("firmware")
    public String getFirmware() {
        return firmware;
    }

    @PropertyName("firmware")
    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }
}