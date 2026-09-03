package com.alidogukan.avora.season;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.SeasonStatus;
import com.alidogukan.avora.models.ZoneSeasonState;

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

    @Test
    public void removedZoneIsRecoveredFromItsRecordedArchive() {
        GardenSeason archived = season(
                "zone-005", "season-bean", SeasonStatus.CLOSED, 200L, true);
        archived.setZone_name("Fasulye");
        archived.setManual_journal_event_count(1);

        List<GardenZone> archiveZones = repository.inactiveArchiveZones(
                List.of(), List.of(archived));

        assertEquals(1, archiveZones.size());
        GardenZone recovered = archiveZones.get(0);
        assertEquals("zone-005", recovered.getZone_id());
        assertEquals("Fasulye", recovered.getName());
        assertFalse(recovered.isEnabled());
        assertEquals(List.of(archived),
                repository.visibleFor(recovered, List.of(archived)));
    }

    @Test
    public void reusedChannelKeepsOldAreaArchiveSeparate() {
        GardenZone current = new GardenZone();
        current.setZone_id("zone-003");
        current.setArea_id(ZoneAreaIdentity.newAreaId());
        current.setEnabled(true);
        current.setLifecycle_status("ACTIVE");
        GardenSeason legacyArchive = season(
                "zone-003", "season-old", SeasonStatus.CLOSED, 200L, true);
        legacyArchive.setManual_journal_event_count(1);

        List<GardenZone> archiveZones = repository.inactiveArchiveZones(
                List.of(current), List.of(legacyArchive));

        assertEquals(1, archiveZones.size());
        assertEquals("", archiveZones.get(0).getArea_id());
    }

    @Test
    public void archiveAlreadyRepresentedByCurrentAreaIsNotDuplicated() {
        GardenZone current = new GardenZone();
        current.setZone_id("zone-002");
        current.setEnabled(true);
        current.setLifecycle_status("ACTIVE");
        GardenSeason archived = season(
                "zone-002", "season-pepper", SeasonStatus.CLOSED, 200L, true);
        archived.setPhoto_count(1);

        assertTrue(repository.inactiveArchiveZones(
                List.of(current), List.of(archived)).isEmpty());
    }

    @Test
    public void existingInactiveArchiveIdentityIsReused() {
        GardenZone inactive = new GardenZone();
        inactive.setZone_id("zone-004");
        inactive.setEnabled(false);
        inactive.setLifecycle_status("INACTIVE");
        GardenSeason archived = season(
                "zone-004", "season-eggplant", SeasonStatus.CLOSED, 200L, true);
        archived.setFertilizer_application_count(1);

        List<GardenZone> archiveZones = repository.inactiveArchiveZones(
                List.of(inactive), List.of(archived));

        assertEquals(1, archiveZones.size());
        assertSame(inactive, archiveZones.get(0));
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
