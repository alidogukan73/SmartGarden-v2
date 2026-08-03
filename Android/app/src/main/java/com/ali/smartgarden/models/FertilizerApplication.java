package com.ali.smartgarden.models;

public class FertilizerApplication {

    private String application_id;
    private String zone_id;
    private String zone_name;
    private String product_id;
    private String product_name;
    private double applied_dose;
    private String dose_unit;
    private double area_m2;
    private double tank_liters;
    private double recommended_dose_min;
    private double recommended_dose_max;
    private long applied_at_epoch;
    private long next_application_at_epoch;
    private String source = "MANUAL";
    private String application_type = "NUTRITION";
    private String application_method;
    private String notes;

    public FertilizerApplication() {
        // Firebase requires an empty constructor.
    }

    public String getApplication_id() { return application_id; }
    public void setApplication_id(String value) { application_id = value; }
    public String getZone_id() { return zone_id; }
    public void setZone_id(String value) { zone_id = value; }
    public String getZone_name() { return zone_name; }
    public void setZone_name(String value) { zone_name = value; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String value) { product_id = value; }
    public String getProduct_name() { return product_name; }
    public void setProduct_name(String value) { product_name = value; }
    public double getApplied_dose() { return applied_dose; }
    public void setApplied_dose(double value) { applied_dose = value; }
    public String getDose_unit() { return dose_unit; }
    public void setDose_unit(String value) { dose_unit = value; }
    public double getArea_m2() { return area_m2; }
    public void setArea_m2(double value) { area_m2 = value; }
    public double getTank_liters() { return tank_liters; }
    public void setTank_liters(double value) { tank_liters = value; }
    public double getRecommended_dose_min() {
        return recommended_dose_min;
    }
    public void setRecommended_dose_min(double value) {
        recommended_dose_min = value;
    }
    public double getRecommended_dose_max() {
        return recommended_dose_max;
    }
    public void setRecommended_dose_max(double value) {
        recommended_dose_max = value;
    }
    public long getApplied_at_epoch() { return applied_at_epoch; }
    public void setApplied_at_epoch(long value) { applied_at_epoch = value; }
    public long getNext_application_at_epoch() {
        return next_application_at_epoch;
    }
    public void setNext_application_at_epoch(long value) {
        next_application_at_epoch = value;
    }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getApplication_type() { return application_type; }
    public void setApplication_type(String value) {
        application_type = value;
    }
    public String getApplication_method() { return application_method; }
    public void setApplication_method(String value) {
        application_method = value;
    }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
