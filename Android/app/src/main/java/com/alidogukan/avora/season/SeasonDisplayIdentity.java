package com.alidogukan.avora.season;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;

/** Resolves a season's immutable display identity without borrowing a later crop. */
public final class SeasonDisplayIdentity {
    private static final String DEFAULT_EMOJI = "🌱";

    private SeasonDisplayIdentity() { }

    public static String name(GardenSeason season, GardenZone currentZone) {
        if (season != null) {
            String archivedName = safe(season.getZone_name());
            if (!archivedName.isBlank()) return archivedName;
            String archivedPlantType = safe(season.getPlant_type());
            if (!archivedPlantType.isBlank()) return archivedPlantType;
        }
        String currentName = safe(currentZone == null ? "" : currentZone.getName());
        return currentName;
    }

    public static String emoji(GardenSeason season, GardenZone currentZone) {
        if (season == null) {
            String currentEmoji = safe(currentZone == null ? "" : currentZone.getEmoji());
            return currentEmoji.isBlank() ? DEFAULT_EMOJI : currentEmoji;
        }

        String archivedEmoji = safe(season.getEmoji());
        if (!archivedEmoji.isBlank()) return archivedEmoji;

        String archivedIdentity = firstNonBlank(
                season.getZone_name(),
                season.getPlant_type(),
                season.getLabel()
        );
        return SeasonStartConfiguration.suggestedCropEmoji(archivedIdentity);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String clean = safe(value);
            if (!clean.isBlank()) return clean;
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
