package com.alidogukan.avora.models;

import java.util.Map;

public class FertilizationProfile {

    private boolean enabled;
    private String planting_date = "";
    private String growth_stage = "NOT_SET";
    private String active_plan_id = "";
    private String active_product_id = "";
    private long next_application_at_epoch;
    private long last_application_at_epoch;
    private double area_m2;
    private double tank_liters;
    private boolean reminder_enabled = true;
    private long updated_at_epoch;
    private double water_ph;
    private double water_ec_ms;
    private long water_analysis_updated_at_epoch;
    private Map<String, FertilizerApplicationSchedule>
            application_schedules;

    public FertilizationProfile() {
        // Firebase requires an empty constructor.
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPlanting_date() {
        return planting_date;
    }

    public void setPlanting_date(String plantingDate) {
        this.planting_date = plantingDate;
    }

    public String getGrowth_stage() {
        return growth_stage;
    }

    public void setGrowth_stage(String growthStage) {
        this.growth_stage = growthStage;
    }

    public String getActive_plan_id() {
        return active_plan_id;
    }

    public void setActive_plan_id(String activePlanId) {
        this.active_plan_id = activePlanId;
    }

    public String getActive_product_id() {
        return active_product_id;
    }

    public void setActive_product_id(String activeProductId) {
        this.active_product_id = activeProductId;
    }

    public long getNext_application_at_epoch() {
        return next_application_at_epoch;
    }

    public void setNext_application_at_epoch(long nextApplicationAtEpoch) {
        this.next_application_at_epoch = nextApplicationAtEpoch;
    }

    public long getLast_application_at_epoch() {
        return last_application_at_epoch;
    }

    public void setLast_application_at_epoch(long value) {
        last_application_at_epoch = value;
    }

    public double getArea_m2() { return area_m2; }
    public void setArea_m2(double value) { area_m2 = value; }
    public double getTank_liters() { return tank_liters; }
    public void setTank_liters(double value) { tank_liters = value; }

    public boolean isReminder_enabled() {
        return reminder_enabled;
    }

    public void setReminder_enabled(boolean reminderEnabled) {
        this.reminder_enabled = reminderEnabled;
    }

    public long getUpdated_at_epoch() {
        return updated_at_epoch;
    }

    public void setUpdated_at_epoch(long updatedAtEpoch) {
        this.updated_at_epoch = updatedAtEpoch;
    }
    public double getWater_ph() { return water_ph; }
    public void setWater_ph(double value) { this.water_ph = value; }
    public double getWater_ec_ms() { return water_ec_ms; }
    public void setWater_ec_ms(double value) { this.water_ec_ms = value; }
    public long getWater_analysis_updated_at_epoch() {
        return water_analysis_updated_at_epoch;
    }
    public void setWater_analysis_updated_at_epoch(long value) {
        this.water_analysis_updated_at_epoch = value;
    }
    public Map<String, FertilizerApplicationSchedule>
    getApplication_schedules() {
        return application_schedules;
    }
    public void setApplication_schedules(
            Map<String, FertilizerApplicationSchedule> value
    ) {
        application_schedules = value;
    }
}
