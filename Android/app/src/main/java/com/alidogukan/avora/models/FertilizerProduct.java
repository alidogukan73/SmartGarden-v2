package com.alidogukan.avora.models;

import java.util.List;

public class FertilizerProduct {

    private String product_id;
    private String name;
    private String form;
    private String npk;
    private String application_type = "";
    private double label_dosage;
    private double label_dosage_min;
    private double label_dosage_max;
    private String dosage_unit;
    private int minimum_interval_days;
    private String notes;
    private boolean enabled = true;
    private long updated_at_epoch;
    private boolean verified;
    private String source_url;
    private List<String> recommended_stages;
    private List<String> functional_tags;
    private boolean organic_farming_eligible;
    private double stock_amount;
    private String stock_unit;
    private double low_stock_threshold;

    public FertilizerProduct() {
        // Firebase requires an empty constructor.
    }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String value) { product_id = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getForm() { return form; }
    public void setForm(String value) { form = value; }
    public String getNpk() { return npk; }
    public void setNpk(String value) { npk = value; }
    public String getApplication_type() { return application_type; }
    public void setApplication_type(String value) {
        application_type = value;
    }
    public double getLabel_dosage() { return label_dosage; }
    public void setLabel_dosage(double value) { label_dosage = value; }
    public double getLabel_dosage_min() { return label_dosage_min; }
    public void setLabel_dosage_min(double value) {
        label_dosage_min = value;
    }
    public double getLabel_dosage_max() { return label_dosage_max; }
    public void setLabel_dosage_max(double value) {
        label_dosage_max = value;
    }
    public String getDosage_unit() { return dosage_unit; }
    public void setDosage_unit(String value) { dosage_unit = value; }
    public int getMinimum_interval_days() { return minimum_interval_days; }
    public void setMinimum_interval_days(int value) {
        minimum_interval_days = value;
    }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public long getUpdated_at_epoch() { return updated_at_epoch; }
    public void setUpdated_at_epoch(long value) {
        updated_at_epoch = value;
    }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean value) { verified = value; }
    public String getSource_url() { return source_url; }
    public void setSource_url(String value) { source_url = value; }
    public List<String> getRecommended_stages() {
        return recommended_stages;
    }
    public void setRecommended_stages(List<String> value) {
        recommended_stages = value;
    }
    public List<String> getFunctional_tags() { return functional_tags; }
    public void setFunctional_tags(List<String> value) {
        functional_tags = value;
    }
    public boolean isOrganic_farming_eligible() {
        return organic_farming_eligible;
    }
    public void setOrganic_farming_eligible(boolean value) {
        organic_farming_eligible = value;
    }
    public double getStock_amount() { return stock_amount; }
    public void setStock_amount(double value) { stock_amount = value; }
    public String getStock_unit() { return stock_unit; }
    public void setStock_unit(String value) { stock_unit = value; }
    public double getLow_stock_threshold() {
        return low_stock_threshold;
    }
    public void setLow_stock_threshold(double value) {
        low_stock_threshold = value;
    }
}
