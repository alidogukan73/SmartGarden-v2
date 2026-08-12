package com.ali.smartgarden.models;

/** A dated, user-recorded event in one garden zone's season. */
public class GardenEvent {
    private String id = "";
    private String zone_id = "";
    private String type = "";
    private String note = "";
    private String source = "MANUAL";
    private String source_key = "";
    private long occurred_at_epoch;

    public String getId() { return id; }
    public void setId(String id) { this.id = id == null ? "" : id; }
    public String getZone_id() { return zone_id; }
    public void setZone_id(String zoneId) { this.zone_id = zoneId == null ? "" : zoneId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type == null ? "" : type; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? "" : note; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value == null ? "MANUAL" : value; }
    public String getSource_key() { return source_key; }
    public void setSource_key(String value) { source_key = value == null ? "" : value; }
    public long getOccurred_at_epoch() { return occurred_at_epoch; }
    public void setOccurred_at_epoch(long occurredAtEpoch) { this.occurred_at_epoch = occurredAtEpoch; }
}
