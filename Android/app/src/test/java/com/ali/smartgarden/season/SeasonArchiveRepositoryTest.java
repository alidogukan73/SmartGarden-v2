package com.ali.smartgarden.season;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.GardenSeason;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.SeasonStatus;
import com.ali.smartgarden.models.ZoneSeasonState;

import org.junit.Test;

import java.util.List;

public class SeasonArchiveRepositoryTest {
    private final SeasonArchiveRepository repository =
            new SeasonArchiveRepository();

    @Test
    public void visibleListContainsCurrentManifestAndRealCompletedArchivesOnly() {
        GardenZone zone = zoneWithActiveSeason("zone-001", "season-current");
        GardenSeason current = season(
                "zone-001", "season-current", SeasonStatus.ACTIVE, 0L, false);
        GardenSeason orphan = season(
                "zone-001", "season-orphan", SeasonStatus.ACTIVE, 0L, false);
        GardenSeason completed = season(
                "zone-001", "season-completed", SeasonStatus.CLOSED, 200L, false);
        GardenSeason otherZone = season(
                "zone-002", "season-other", SeasonStatus.CLOSED, 200L, false);

        List<GardenSeason> visible = repository.visibleFor(
                zone,
                List.of(orphan, otherZone, completed, current));

        assertEquals(List.of(completed, current), visible);
    }

    @Test
    public void completedArchiveCanRequireActualFieldActivity() {
        GardenSeason emptyTest = season(
                "zone-001", "season-empty", SeasonStatus.CLOSED, 200L, false);
        GardenSeason fieldSeason = season(
                "zone-001", "season-field", SeasonStatus.CLOSED, 220L, false);
        fieldSeason.setWatering_count(1);
        GardenSeason emptyLegacy = season(
                "zone-001", "season-legacy", SeasonStatus.CLOSED, 230L, true);

        assertEquals(
                List.of(emptyTest, fieldSeason),
                repository.completed(
                        List.of(emptyTest, fieldSeason, emptyLegacy),
                        false));
        assertEquals(
                List.of(fieldSeason),
                repository.completed(
                        List.of(emptyTest, fieldSeason, emptyLegacy),
                        true));
    }

    @Test
    public void recordedArchiveCheckIsScopedToRequestedZone() {
        GardenSeason recorded = season(
                "zone-002", "season-recorded", SeasonStatus.CLOSED, 200L, false);
        recorded.setManual_journal_event_count(1);

        assertTrue(repository.hasRecorded("zone-002", List.of(recorded)));
        assertFalse(repository.hasRecorded("zone-001", List.of(recorded)));
        assertFalse(repository.hasRecorded("zone-002", null));
    }

    private static GardenZone zoneWithActiveSeason(
            String zoneId,
            String seasonId
    ) {
        ZoneSeasonState state = new ZoneSeasonState();
        state.setStatus(SeasonStatus.ACTIVE);
        state.setActive_season_id(seasonId);
        GardenZone zone = new GardenZone();
        zone.setZone_id(zoneId);
        zone.setSeason(state);
        return zone;
    }

    private static GardenSeason season(
            String zoneId,
            String seasonId,
            String status,
            long endedAt,
            boolean legacy
    ) {
        GardenSeason season = new GardenSeason();
        season.setZone_id(zoneId);
        season.setSeason_id(seasonId);
        season.setStatus(status);
        season.setEnded_at_epoch(endedAt);
        season.setIncludes_legacy_records(legacy);
        return season;
    }
}
