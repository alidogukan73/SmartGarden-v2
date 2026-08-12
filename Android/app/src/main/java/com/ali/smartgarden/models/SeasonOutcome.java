package com.ali.smartgarden.models;

/** A closing note for one zone's growing season, stored on the owner's phone. */
public class SeasonOutcome {
    private String id = "";
    private String zone_id = "";
    private String result = "";
    private String harvest_amount = "";
    private String next_season_note = "";
    private String yield_note = "";
    private String issues_note = "";
    private String successful_practices = "";
    private String water_summary = "";
    private String fertilizer_summary = "";
    private long recorded_at_epoch;

    public String getId() { return id; }
    public void setId(String value) { id = value == null ? "" : value; }
    public String getZone_id() { return zone_id; }
    public void setZone_id(String value) { zone_id = value == null ? "" : value; }
    public String getResult() { return result; }
    public void setResult(String value) { result = value == null ? "" : value; }
    public String getHarvest_amount() { return harvest_amount; }
    public void setHarvest_amount(String value) { harvest_amount = value == null ? "" : value; }
    public String getNext_season_note() { return next_season_note; }
    public void setNext_season_note(String value) { next_season_note = value == null ? "" : value; }
    public String getYield_note() { return yield_note; }
    public void setYield_note(String value) { yield_note = value == null ? "" : value; }
    public String getIssues_note() { return issues_note; }
    public void setIssues_note(String value) { issues_note = value == null ? "" : value; }
    public String getSuccessful_practices() { return successful_practices; }
    public void setSuccessful_practices(String value) { successful_practices = value == null ? "" : value; }
    public String getWater_summary() { return water_summary; }
    public void setWater_summary(String value) { water_summary = value == null ? "" : value; }
    public String getFertilizer_summary() { return fertilizer_summary; }
    public void setFertilizer_summary(String value) { fertilizer_summary = value == null ? "" : value; }
    public long getRecorded_at_epoch() { return recorded_at_epoch; }
    public void setRecorded_at_epoch(long value) { recorded_at_epoch = value; }
}
