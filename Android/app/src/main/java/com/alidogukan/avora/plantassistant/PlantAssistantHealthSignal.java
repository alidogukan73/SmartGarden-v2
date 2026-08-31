package com.alidogukan.avora.plantassistant;

/** A recent, advisory-only Plant Doctor finding for one garden zone. */
public final class PlantAssistantHealthSignal {
    private final String zoneId;
    private final String urgency;
    private final String title;
    private final long createdAtEpoch;

    public PlantAssistantHealthSignal(String zoneId, String urgency, String title, long createdAtEpoch) {
        this.zoneId = zoneId == null ? "" : zoneId;
        this.urgency = urgency == null ? "" : urgency;
        this.title = title == null ? "" : title;
        this.createdAtEpoch = createdAtEpoch;
    }

    public String getZoneId() { return zoneId; }
    public String getUrgency() { return urgency; }
    public String getTitle() { return title; }

    public long getCreatedAtEpoch() { return createdAtEpoch; }

    public boolean isRecent(long nowEpoch) {
        return createdAtEpoch > 0 && nowEpoch >= createdAtEpoch
                && nowEpoch - createdAtEpoch <= 14L * 24L * 60L * 60L;
    }
}
