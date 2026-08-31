package com.alidogukan.avora.season;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.ZoneSeasonState;

import java.util.ArrayList;
import java.util.List;

/** Selects the season records that are safe to present as live or archived data. */
public final class SeasonArchiveRepository {

    public List<GardenSeason> visibleFor(
            GardenZone zone,
            List<GardenSeason> allSeasons
    ) {
        List<GardenSeason> result = new ArrayList<>();
        String zoneId = safe(zone == null ? "" : zone.getZone_id());
        ZoneSeasonState current = zone == null ? null : zone.getSeason();
        if (allSeasons == null) return result;
        for (GardenSeason season : allSeasons) {
            if (season == null
                    || !zoneId.equals(safe(season.getZone_id()))) {
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

    public boolean hasRecorded(String zoneId, List<GardenSeason> allSeasons) {
        if (allSeasons == null) return false;
        for (GardenSeason season : allSeasons) {
            if (season != null
                    && safe(zoneId).equals(safe(season.getZone_id()))
                    && SeasonScope.isRealCompletedArchive(season)
                    && SeasonScope.hasRecordedActivity(season)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
