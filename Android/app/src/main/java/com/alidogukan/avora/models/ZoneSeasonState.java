package com.alidogukan.avora.models;

import com.google.firebase.database.IgnoreExtraProperties;

/** Current season pointer stored below one zone. */
@IgnoreExtraProperties
public class ZoneSeasonState {
    private String active_season_id = "";
    private String status = "";
    private String label = "";
    private long started_at_epoch;
    private long ended_at_epoch;
    private boolean include_legacy_records;
    private long updated_at_epoch;

    public ZoneSeasonState() { }

    public String getActive_season_id() { return active_season_id; }
    public void setActive_season_id(String value) { active_season_id = safe(value); }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = safe(value); }
    public String getLabel() { return label; }
    public void setLabel(String value) { label = safe(value); }
    public long getStarted_at_epoch() { return started_at_epoch; }
    public void setStarted_at_epoch(long value) { started_at_epoch = Math.max(0L, value); }
    public long getEnded_at_epoch() { return ended_at_epoch; }
    public void setEnded_at_epoch(long value) { ended_at_epoch = Math.max(0L, value); }
    public boolean isInclude_legacy_records() { return include_legacy_records; }
    public void setInclude_legacy_records(boolean value) { include_legacy_records = value; }
    public long getUpdated_at_epoch() { return updated_at_epoch; }
    public void setUpdated_at_epoch(long value) { updated_at_epoch = Math.max(0L, value); }

    public boolean isActive() {
        return SeasonStatus.isActive(status) && !active_season_id.isBlank();
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
