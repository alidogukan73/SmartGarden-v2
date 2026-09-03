package com.alidogukan.avora.season;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.ZoneSeasonState;

import java.util.Locale;
import java.util.UUID;

/** Separates a physical growing area from the reusable zone hardware channel. */
public final class ZoneAreaIdentity {
    private static final String LEGACY_PREFIX = "legacy-area-";
    private static final String AREA_PREFIX = "area-";

    private ZoneAreaIdentity() { }

    public static String newAreaId() {
        return AREA_PREFIX + UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
    }

    public static String effective(GardenZone zone) {
        if (zone == null) return "";
        String explicit = safe(zone.getArea_id());
        return explicit.isBlank() ? legacy(zone.getZone_id()) : explicit;
    }

    public static String legacy(String zoneId) {
        String clean = safe(zoneId).replaceAll("[^A-Za-z0-9_-]", "-");
        return clean.isBlank() ? "" : LEGACY_PREFIX + clean;
    }

    public static boolean belongsTo(GardenZone zone, GardenSeason season) {
        if (zone == null || season == null) return false;
        String areaId = effective(zone);
        String archivedAreaId = safe(season.getArea_id());
        if (!archivedAreaId.isBlank()) return areaId.equals(archivedAreaId);
        return areaId.equals(legacy(zone.getZone_id()))
                && safe(zone.getZone_id()).equals(safe(season.getZone_id()));
    }

    /** Legacy active manifests are trusted when the zone state explicitly points to them. */
    public static boolean belongsToCurrentOrArea(GardenZone zone, GardenSeason season) {
        if (belongsTo(zone, season)) return true;
        if (zone == null || season == null
                || !safe(zone.getZone_id()).equals(safe(season.getZone_id()))) return false;
        ZoneSeasonState current = zone.getSeason();
        return current != null
                && current.isActive()
                && current.isSeasonActive(season.getSeason_id());
    }

    public static boolean isLegacyArea(String areaId) {
        return safe(areaId).startsWith(LEGACY_PREFIX);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
