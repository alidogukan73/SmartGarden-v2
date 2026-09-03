package com.alidogukan.avora.season;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.SeasonStatus;
import com.alidogukan.avora.models.ZoneSeasonState;

import org.junit.Test;

public final class ZoneAreaIdentityTest {
    @Test
    public void legacyZoneKeepsItsLegacyArchive() {
        GardenZone zone = zone("zone-003", "");
        GardenSeason bean = season("zone-003", "");

        assertTrue(ZoneAreaIdentity.belongsTo(zone, bean));
    }

    @Test
    public void newAreaOnReusedChannelDoesNotInheritLegacyArchive() {
        GardenZone zone = zone("zone-003", ZoneAreaIdentity.newAreaId());
        GardenSeason bean = season("zone-003", "");

        assertFalse(ZoneAreaIdentity.belongsTo(zone, bean));
    }

    @Test
    public void archiveFollowsAreaEvenWhenHardwareChannelChanges() {
        String areaId = ZoneAreaIdentity.newAreaId();
        GardenZone zone = zone("zone-004", areaId);
        GardenSeason season = season("zone-003", areaId);

        assertTrue(ZoneAreaIdentity.belongsTo(zone, season));
    }

    @Test
    public void legacyManifestExplicitlyReferencedByModernZoneRemainsCurrent() {
        GardenZone zone = zone("zone-001", ZoneAreaIdentity.newAreaId());
        ZoneSeasonState state = new ZoneSeasonState();
        state.setStatus(SeasonStatus.ACTIVE);
        state.setActive_season_id("season-old");
        zone.setSeason(state);
        GardenSeason season = season("zone-001", "");
        season.setSeason_id("season-old");

        assertFalse(ZoneAreaIdentity.belongsTo(zone, season));
        assertTrue(ZoneAreaIdentity.belongsToCurrentOrArea(zone, season));
    }

    private static GardenZone zone(String zoneId, String areaId) {
        GardenZone value = new GardenZone();
        value.setZone_id(zoneId);
        value.setArea_id(areaId);
        return value;
    }

    private static GardenSeason season(String zoneId, String areaId) {
        GardenSeason value = new GardenSeason();
        value.setZone_id(zoneId);
        value.setArea_id(areaId);
        return value;
    }
}
