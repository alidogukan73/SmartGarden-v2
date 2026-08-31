package com.alidogukan.avora.zones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ZoneRemovalEndToEndTest {

    @Test
    public void untouchedClosedZoneIsPhysicallyDeleted() {
        assertEquals(
                ZoneCapacityPolicy.DeactivationAction.DELETE,
                decide(false, false));
    }

    @Test
    public void anyLocalOrCloudHistoryPreservesAnArchive() {
        assertEquals(
                ZoneCapacityPolicy.DeactivationAction.ARCHIVE,
                decide(true, false));
        assertEquals(
                ZoneCapacityPolicy.DeactivationAction.ARCHIVE,
                decide(false, true));
        assertEquals(
                ZoneCapacityPolicy.DeactivationAction.ARCHIVE,
                decide(true, true));
    }

    @Test
    public void activeSeasonStopsRemovalBeforeHistoryDecision() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> ZoneCapacityPolicy.decideDeactivation(
                        true,
                        "ACTIVE",
                        "season-001",
                        false,
                        false,
                        false));
        assertEquals(ZoneCapacityPolicy.ERROR_ACTIVE_SEASON, error.getMessage());
    }

    @Test
    public void wateringAndMissingZoneAreRejected() {
        IllegalStateException watering = assertThrows(
                IllegalStateException.class,
                () -> ZoneCapacityPolicy.decideDeactivation(
                        true, "CLOSED", "", true, false, false));
        assertEquals(ZoneCapacityPolicy.ERROR_IRRIGATION_BUSY,
                watering.getMessage());

        IllegalStateException missing = assertThrows(
                IllegalStateException.class,
                () -> ZoneCapacityPolicy.decideDeactivation(
                        false, "CLOSED", "", false, false, false));
        assertEquals(ZoneCapacityPolicy.ERROR_ZONE_NOT_FOUND,
                missing.getMessage());
    }

    private static ZoneCapacityPolicy.DeactivationAction decide(
            boolean localHistory,
            boolean cloudHistory
    ) {
        return ZoneCapacityPolicy.decideDeactivation(
                true,
                "CLOSED",
                "",
                false,
                localHistory,
                cloudHistory);
    }
}
