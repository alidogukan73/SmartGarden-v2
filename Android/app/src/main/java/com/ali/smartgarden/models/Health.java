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

    public Health() {
        ipAddress = "";
        updatedAt = "";
        firmware = "";
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