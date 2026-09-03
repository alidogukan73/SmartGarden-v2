package com.alidogukan.avora.season;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.ZoneSeasonState;
import com.alidogukan.avora.zones.ZoneCapacityPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Selects the season records that are safe to present as live or archived data. */
public final class SeasonArchiveRepository {

    public List<GardenSeason> visibleFor(
            GardenZone zone,
            List<GardenSeason> allSeasons
    ) {
        List<GardenSeason> result = new ArrayList<>();
        ZoneSeasonState current = zone == null ? null : zone.getSeason();
        if (allSeasons == null) return result;
        for (GardenSeason season : allSeasons) {
            if (season == null
                    || !ZoneAreaIdentity.belongsToCurrentOrArea(zone, season)) {
                continue;
            }
            if (SeasonScope.isVisibleSeason(season, current)) {
                result.add(season);
            }
        }
        return result;
    }

    public List<GardenSeason> completed(
            List<GardenSeason> history,
            boolean requireRecordedActivity
    ) {
        List<GardenSeason> result = new ArrayList<>();
        if (history == null) return result;
        for (GardenSeason season : history) {
            if (SeasonScope.isRealCompletedArchive(season)
                    && (!requireRecordedActivity
                    || SeasonScope.hasRecordedActivity(season))) {
                result.add(season);
            }
        }
        return result;
    }

    public boolean hasRecorded(GardenZone zone, List<GardenSeason> allSeasons) {
        if (allSeasons == null) return false;
        for (GardenSeason season : allSeasons) {
            if (season != null
                    && ZoneAreaIdentity.belongsTo(zone, season)
                    && SeasonScope.isRealCompletedArchive(season)
                    && SeasonScope.hasRecordedActivity(season)) {
                return true;
            }
        }
        return false;
    }

    /** Backward-compatible lookup for legacy zones that predate physical area ids. */
    public boolean hasRecorded(String zoneId, List<GardenSeason> allSeasons) {
        GardenZone legacyZone = new GardenZone();
        legacyZone.setZone_id(safe(zoneId));
        return hasRecorded(legacyZone, allSeasons);
    }

    /**
     * Returns inactive physical areas that own a real archive. When a removed
     * area's zone node no longer exists, a read-only identity is rebuilt from
     * its immutable season manifest instead of hiding the archive.
     */
    public List<GardenZone> inactiveArchiveZones(
            List<GardenZone> zones,
            List<GardenSeason> allSeasons
    ) {
        List<GardenZone> currentZones = zones == null
                ? new ArrayList<>() : zones;
        Map<String, GardenZone> result = new LinkedHashMap<>();

        for (GardenZone zone : currentZones) {
            if (zone == null
                    || !ZoneCapacityPolicy.isValidZoneId(zone.getZone_id())
                    || !ZoneCapacityPolicy.isInactive(zone)
                    || !hasRecorded(zone, allSeasons)) {
                continue;
            }
            result.put(ZoneAreaIdentity.effective(zone), zone);
        }

        if (allSeasons == null) return new ArrayList<>(result.values());
        for (GardenSeason season : allSeasons) {
            if (!SeasonScope.isRealCompletedArchive(season)
                    || !SeasonScope.hasRecordedActivity(season)
                    || representedByCurrentZone(currentZones, season)) {
                continue;
            }
            String areaKey = archivedAreaKey(season);
            if (!areaKey.isBlank() && !result.containsKey(areaKey)) {
                result.put(areaKey, archiveIdentity(season));
            }
        }
        return new ArrayList<>(result.values());
    }

    private static boolean representedByCurrentZone(
            List<GardenZone> zones,
            GardenSeason season
    ) {
        for (GardenZone zone : zones) {
            if (ZoneAreaIdentity.belongsTo(zone, season)) return true;
        }
        return false;
    }

    private static String archivedAreaKey(GardenSeason season) {
        String areaId = safe(season == null ? "" : season.getArea_id());
        return areaId.isBlank()
                ? ZoneAreaIdentity.legacy(season == null ? "" : season.getZone_id())
                : areaId;
    }

    private static GardenZone archiveIdentity(GardenSeason season) {
        GardenZone zone = new GardenZone();
        zone.setZone_id(safe(season.getZone_id()));
        zone.setArea_id(safe(season.getArea_id()));
        zone.setArea_name(safe(season.getArea_name()));
        zone.setName(safe(season.getZone_name()));
        zone.setPlant_type(safe(season.getPlant_type()));
        zone.setEmoji(safe(season.getEmoji()));
        zone.setEnabled(false);
        zone.setLifecycle_status(ZoneCapacityPolicy.LIFECYCLE_INACTIVE);
        ZoneSeasonState state = new ZoneSeasonState();
        state.setStatus(com.alidogukan.avora.models.SeasonStatus.CLOSED);
        state.setEnded_at_epoch(season.getEnded_at_epoch());
        zone.setSeason(state);
        return zone;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
