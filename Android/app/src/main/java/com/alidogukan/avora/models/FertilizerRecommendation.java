package com.alidogukan.avora.models;

public class FertilizerRecommendation {

    private String plant_type;
    private String growth_stage;
    private String recommendation_id;
    private String product_id;
    private String method;
    private double dose_min;
    private double dose_max;
    private String dose_unit;
    private int interval_days;
    private boolean advisory_only;
    private boolean requires_soil_or_leaf_analysis;
    private String source_url;

    public FertilizerRecommendation() {
        // Firebase requires an empty constructor.
    }

    public String getPlant_type() { return plant_type; }
    public void setPlant_type(String value) { plant_type = value; }
    public String getGrowth_stage() { return growth_stage; }
    public void setGrowth_stage(String value) { growth_stage = value; }
    public String getRecommendation_id() { return recommendation_id; }
    public void setRecommendation_id(String value) {
        recommendation_id = value;
    }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String value) { product_id = value; }
    public String getMethod() { return method; }
    public void setMethod(String value) { method = value; }
    public double getDose_min() { return dose_min; }
    public void setDose_min(double value) { dose_min = value; }
    public double getDose_max() { return dose_max; }
    public void setDose_max(double value) { dose_max = value; }
    public String getDose_unit() { return dose_unit; }
    public void setDose_unit(String value) { dose_unit = value; }
    public int getInterval_days() { return interval_days; }
    public void setInterval_days(int value) { interval_days = value; }
    public boolean isAdvisory_only() { return advisory_only; }
    public void setAdvisory_only(boolean value) { advisory_only = value; }
    public boolean isRequires_soil_or_leaf_analysis() {
        return requires_soil_or_leaf_analysis;
    }
    public void setRequires_soil_or_leaf_analysis(boolean value) {
        requires_soil_or_leaf_analysis = value;
    }
    public String getSource_url() { return source_url; }
    public void setSource_url(String value) { source_url = value; }
}
