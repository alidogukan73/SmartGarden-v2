package com.alidogukan.avora.plantassistant;

import com.alidogukan.avora.models.GardenPhoto;

import java.util.Collections;
import java.util.List;

/** Compares visible-growth scores only within the same garden zone. */
public final class PlantGrowthTrendPolicy {
    public static final String FIRST_RECORD = "FIRST_RECORD";
    public static final String IMPROVING = "IMPROVING";
    public static final String STABLE = "STABLE";
    public static final String DECLINING = "DECLINING";
    public static final int MEANINGFUL_CHANGE = 5;

    private PlantGrowthTrendPolicy() { }

    public static Result compare(List<GardenPhoto> records, String zoneId,
                                 String excludedPhotoId, int currentScore) {
        GardenPhoto previous = newestPrevious(
                records == null ? Collections.emptyList() : records,
                safe(zoneId), safe(excludedPhotoId));
        if (previous == null) return new Result(FIRST_RECORD, 0, 0L);
        int delta = currentScore - previous.getGrowth_score();
        String trend = delta >= MEANINGFUL_CHANGE
                ? IMPROVING
                : delta <= -MEANINGFUL_CHANGE ? DECLINING : STABLE;
        return new Result(trend, delta, previous.getCaptured_at_epoch());
    }

    private static GardenPhoto newestPrevious(List<GardenPhoto> records, String zoneId,
                                               String excludedPhotoId) {
        GardenPhoto newest = null;
        for (GardenPhoto photo : records) {
            if (photo == null
                    || !"growth_status".equals(photo.getAnalysis_goal())
                    || !zoneId.equals(safe(photo.getZone_id()))
                    || excludedPhotoId.equals(safe(photo.getId()))
                    || photo.getGrowth_score() < 0
                    || photo.getGrowth_score() > 100) {
                continue;
            }
            if (newest == null
                    || photo.getCaptured_at_epoch() > newest.getCaptured_at_epoch()) {
                newest = photo;
            }
        }
        return newest;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        public final String trend;
        public final int scoreDelta;
        public final long previousCapturedAtEpoch;

        Result(String trend, int scoreDelta, long previousCapturedAtEpoch) {
            this.trend = trend;
            this.scoreDelta = scoreDelta;
            this.previousCapturedAtEpoch = previousCapturedAtEpoch;
        }
    }
}
