package com.alidogukan.avora.season;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.SeasonStatus;

import java.util.List;

/** Computes one safe moisture interval for crops sharing a sensor and valve. */
public final class SharedIrrigationCompatibility {
    private SharedIrrigationCompatibility() { }

    public static Result evaluate(
            List<GardenSeason> activeSeasons,
            SeasonStartConfiguration candidate
    ) {
        int commonMin = candidate.getIdealMoistureMin();
        int commonMax = candidate.getIdealMoistureMax();
        if (activeSeasons != null) {
            for (GardenSeason season : activeSeasons) {
                if (season == null || !SeasonStatus.isActive(season.getStatus())) continue;
                commonMin = Math.max(commonMin, season.getIdeal_moisture_min());
                commonMax = Math.min(commonMax, season.getIdeal_moisture_max());
            }
        }
        return new Result(commonMin <= commonMax, commonMin, commonMax);
    }

    /** Recomputes the shared watering threshold after one crop leaves the area. */
    public static int commonMinimumOrFallback(
            List<GardenSeason> activeSeasons,
            int fallback
    ) {
        int commonMin = -1;
        if (activeSeasons != null) {
            for (GardenSeason season : activeSeasons) {
                if (season == null || !SeasonStatus.isActive(season.getStatus())) continue;
                int min = season.getIdeal_moisture_min();
                int max = season.getIdeal_moisture_max();
                if (min < 0 || max < min || (min == 0 && max == 100)) continue;
                commonMin = Math.max(commonMin, min);
            }
        }
        return commonMin < 0 ? fallback : commonMin;
    }

    public static final class Result {
        private final boolean compatible;
        private final int commonMin;
        private final int commonMax;

        Result(boolean compatible, int commonMin, int commonMax) {
            this.compatible = compatible;
            this.commonMin = commonMin;
            this.commonMax = commonMax;
        }

        public boolean isCompatible() { return compatible; }
        public int getCommonMin() { return commonMin; }
        public int getCommonMax() { return commonMax; }
    }
}
