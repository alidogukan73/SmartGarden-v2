package com.ali.smartgarden.models;

public class ZoneIrrigationStatus {

    private String decision;
    private String decision_reason;
    private boolean cooldown_active;
    private int cooldown_remaining;
    private int queue_position;
    private boolean selected_for_watering;
    private boolean watering_active;
    private boolean sensor_stable;
    private int moisture_deficit;

    public ZoneIrrigationStatus() {
        // Required by Firebase.
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getDecision_reason() {
        return decision_reason;
    }

    public void setDecision_reason(String decisionReason) {
        this.decision_reason = decisionReason;
    }

    public boolean isCooldown_active() {
        return cooldown_active;
    }

    public void setCooldown_active(boolean cooldownActive) {
        this.cooldown_active = cooldownActive;
    }

    public int getCooldown_remaining() {
        return cooldown_remaining;
    }

    public void setCooldown_remaining(int cooldownRemaining) {
        this.cooldown_remaining = cooldownRemaining;
    }

    public int getQueue_position() {
        return queue_position;
    }

    public void setQueue_position(int queuePosition) {
        this.queue_position = queuePosition;
    }

    public boolean isSelected_for_watering() {
        return selected_for_watering;
    }

    public void setSelected_for_watering(
            boolean selectedForWatering
    ) {
        this.selected_for_watering = selectedForWatering;
    }

    public boolean isWatering_active() {
        return watering_active;
    }

    public void setWatering_active(boolean wateringActive) {
        this.watering_active = wateringActive;
    }

    public boolean isSensor_stable() {
        return sensor_stable;
    }

    public void setSensor_stable(boolean sensorStable) {
        this.sensor_stable = sensorStable;
    }

    public int getMoisture_deficit() {
        return moisture_deficit;
    }

    public void setMoisture_deficit(int moistureDeficit) {
        this.moisture_deficit = moistureDeficit;
    }
}
