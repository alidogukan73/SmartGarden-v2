package com.ali.smartgarden.models;

public class GardenZone {

    private String zone_id;
    private String name;
    private String plant_type;
    private String emoji;
    private String sensor_id;
    private boolean sensor_enabled = true;
    private int sensor_calibration_dry_raw = 12650;
    private int sensor_calibration_wet_raw = 505;
    private String valve_id;
    private String valve_type;
    private String valve_mode;
    private int valve_gpio_bcm;
    private int valve_gpio_physical_pin;
    private boolean enabled = true;
    private boolean irrigation_enabled;
    private int order;
    private int moisture_limit = 40;
    private int pump_duration = 10;
    private int cooldown_seconds = 600;
    private int restart_delta = 10;
    private int moisture;
    private int rssi;
    private int raw;
    private double voltage;
    private long updated_at_epoch;
    private ZoneIrrigationStatus irrigation_status;
    private FertilizationProfile fertilization;

    public GardenZone() {
        // Firebase için boş constructor
    }

    public GardenZone(
            String zoneId,
            String name,
            String plantType,
            String emoji,
            String sensorId,
            boolean enabled,
            int order
    ) {
        this.zone_id = zoneId;
        this.name = name;
        this.plant_type = plantType;
        this.emoji = emoji;
        this.sensor_id = sensorId;
        this.enabled = enabled;
        this.order = order;
    }

    public boolean hasSensorData() {
        return updated_at_epoch > 0L;
    }

    public String getZone_id() {
        return zone_id;
    }

    public void setZone_id(String zoneId) {
        this.zone_id = zoneId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlant_type() {
        return plant_type;
    }

    public void setPlant_type(String plantType) {
        this.plant_type = plantType;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensorId) {
        this.sensor_id = sensorId;
    }

    public boolean isSensor_enabled() {
        return sensor_enabled;
    }

    public void setSensor_enabled(boolean sensorEnabled) {
        this.sensor_enabled = sensorEnabled;
    }

    public int getSensor_calibration_dry_raw() {
        return sensor_calibration_dry_raw;
    }

    public void setSensor_calibration_dry_raw(int dryRaw) {
        this.sensor_calibration_dry_raw = dryRaw;
    }

    public int getSensor_calibration_wet_raw() {
        return sensor_calibration_wet_raw;
    }

    public void setSensor_calibration_wet_raw(int wetRaw) {
        this.sensor_calibration_wet_raw = wetRaw;
    }

    public String getValve_id() {
        return valve_id;
    }

    public void setValve_id(String valveId) {
        this.valve_id = valveId;
    }

    public String getValve_type() {
        return valve_type;
    }

    public void setValve_type(String valveType) {
        this.valve_type = valveType;
    }

    public String getValve_mode() {
        return valve_mode;
    }

    public void setValve_mode(String valveMode) {
        this.valve_mode = valveMode;
    }

    public int getValve_gpio_bcm() {
        return valve_gpio_bcm;
    }

    public void setValve_gpio_bcm(int valveGpioBcm) {
        this.valve_gpio_bcm = valveGpioBcm;
    }

    public int getValve_gpio_physical_pin() {
        return valve_gpio_physical_pin;
    }

    public void setValve_gpio_physical_pin(int valveGpioPhysicalPin) {
        this.valve_gpio_physical_pin = valveGpioPhysicalPin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIrrigation_enabled() {
        return irrigation_enabled;
    }

    public void setIrrigation_enabled(boolean irrigationEnabled) {
        this.irrigation_enabled = irrigationEnabled;
    }

    public int getMoisture_limit() {
        return moisture_limit;
    }

    public void setMoisture_limit(int moistureLimit) {
        this.moisture_limit = moistureLimit;
    }

    public int getPump_duration() {
        return pump_duration;
    }

    public void setPump_duration(int pumpDuration) {
        this.pump_duration = pumpDuration;
    }

    public int getCooldown_seconds() {
        return cooldown_seconds;
    }

    public void setCooldown_seconds(int cooldownSeconds) {
        this.cooldown_seconds = cooldownSeconds;
    }

    public int getRestart_delta() {
        return restart_delta;
    }

    public void setRestart_delta(int restartDelta) {
        this.restart_delta = restartDelta;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getMoisture() {
        return moisture;
    }

    public void setMoisture(int moisture) {
        this.moisture = moisture;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getRaw() {
        return raw;
    }

    public void setRaw(int raw) {
        this.raw = raw;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public long getUpdated_at_epoch() {
        return updated_at_epoch;
    }

    public void setUpdated_at_epoch(long updatedAtEpoch) {
        this.updated_at_epoch = updatedAtEpoch;
    }

    public ZoneIrrigationStatus getIrrigation_status() {
        return irrigation_status;
    }

    public void setIrrigation_status(
            ZoneIrrigationStatus irrigationStatus
    ) {
        this.irrigation_status = irrigationStatus;
    }

    public FertilizationProfile getFertilization() {
        return fertilization;
    }

    public void setFertilization(
            FertilizationProfile fertilization
    ) {
        this.fertilization = fertilization;
    }
}
