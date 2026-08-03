package com.ali.smartgarden.models;

public class FertilizerStageGuide {

    private String plant_type;
    private String growth_stage;
    private String primary_focus;
    private String support_options;
    private String caution;
    private boolean advisory_only;
    private boolean requires_soil_or_leaf_analysis;

    public FertilizerStageGuide() {
        // Firebase requires an empty constructor.
    }

    public String getPlant_type() { return plant_type; }
    public void setPlant_type(String value) { plant_type = value; }
    public String getGrowth_stage() { return growth_stage; }
    public void setGrowth_stage(String value) { growth_stage = value; }
    public String getPrimary_focus() { return primary_focus; }
    public void setPrimary_focus(String value) { primary_focus = value; }
    public String getSupport_options() { return support_options; }
    public void setSupport_options(String value) { support_options = value; }
    public String getCaution() { return caution; }
    public void setCaution(String value) { caution = value; }
    public boolean isAdvisory_only() { return advisory_only; }
    public void setAdvisory_only(boolean value) { advisory_only = value; }
    public boolean isRequires_soil_or_leaf_analysis() {
        return requires_soil_or_leaf_analysis;
    }
    public void setRequires_soil_or_leaf_analysis(boolean value) {
        requires_soil_or_leaf_analysis = value;
    }
}
