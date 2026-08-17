package com.ali.smartgarden.models;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
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
    private String mix_group_id;
    private String mix_partner_product_id;
    private String mix_partner_product_name;
    private String mix_risk_level;
    private long outcome_follow_up_due_at_epoch;
    private long outcome_observed_at_epoch;
    private String outcome_status;
    private int outcome_vigor_score;
    private String outcome_notes;

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
    public String getMix_group_id() { return mix_group_id; }
    public void setMix_group_id(String value) { mix_group_id = value; }
    public String getMix_partner_product_id() { return mix_partner_product_id; }
    public void setMix_partner_product_id(String value) { mix_partner_product_id = value; }
    public String getMix_partner_product_name() { return mix_partner_product_name; }
    public void setMix_partner_product_name(String value) { mix_partner_product_name = value; }
    public String getMix_risk_level() { return mix_risk_level; }
    public void setMix_risk_level(String value) { mix_risk_level = value; }
    public long getOutcome_follow_up_due_at_epoch() {
        return outcome_follow_up_due_at_epoch;
    }
    public void setOutcome_follow_up_due_at_epoch(long value) {
        outcome_follow_up_due_at_epoch = value;
    }
    public long getOutcome_observed_at_epoch() {
        return outcome_observed_at_epoch;
    }
    public void setOutcome_observed_at_epoch(long value) {
        outcome_observed_at_epoch = value;
    }
    public String getOutcome_status() { return outcome_status; }
    public void setOutcome_status(String value) { outcome_status = value; }
    public int getOutcome_vigor_score() { return outcome_vigor_score; }
    public void setOutcome_vigor_score(int value) {
        outcome_vigor_score = value;
    }
    public String getOutcome_notes() { return outcome_notes; }
    public void setOutcome_notes(String value) { outcome_notes = value; }
}
