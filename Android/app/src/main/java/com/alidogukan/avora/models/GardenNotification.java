package com.alidogukan.avora.models;

/** A durable in-app AVORA notification. */
public class GardenNotification {
    private String id = "";
    private String type = "SYSTEM";
    private String priority = "NORMAL";
    private String zone_id = "";
    private String season_id = "";
    private String title = "";
    private String description = "";
    private String source_key = "";
    private long created_at_epoch;
    private boolean read;
    private boolean saved;

    public GardenNotification() { }
    public String getId() { return id; } public void setId(String value) { id = safe(value); }
    public String getType() { return type; } public void setType(String value) { type = safe(value); }
    public String getPriority() { return priority; } public void setPriority(String value) { priority = safe(value); }
    public String getZone_id() { return zone_id; } public void setZone_id(String value) { zone_id = safe(value); }
    public String getSeason_id() { return season_id; } public void setSeason_id(String value) { season_id = safe(value); }
    public String getTitle() { return title; } public void setTitle(String value) { title = safe(value); }
    public String getDescription() { return description; } public void setDescription(String value) { description = safe(value); }
    public String getSource_key() { return source_key; } public void setSource_key(String value) { source_key = safe(value); }
    public long getCreated_at_epoch() { return created_at_epoch; } public void setCreated_at_epoch(long value) { created_at_epoch = value; }
    public boolean isRead() { return read; } public void setRead(boolean value) { read = value; }
    public boolean isSaved() { return saved; } public void setSaved(boolean value) { saved = value; }
    private static String safe(String value) { return value == null ? "" : value; }
}
