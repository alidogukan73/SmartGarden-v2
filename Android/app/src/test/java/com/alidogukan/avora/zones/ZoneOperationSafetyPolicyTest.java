package com.alidogukan.avora.zones;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ZoneOperationSafetyPolicyTest {
    @Test
    public void anotherZonesBusyFlagDoesNotBlockTheTargetZone() {
        assertFalse(ZoneOperationSafetyPolicy.isTargetBusy(
                "zone-003", "valve-003",
                false, false, 0L, false,
                true, "zone-001", "valve-001"));
    }

    @Test
    public void targetZoneActivityStillBlocksRemovalAndSeasonCancellation() {
        assertTrue(ZoneOperationSafetyPolicy.isTargetBusy(
                "zone-003", "valve-003",
                true, false, 0L, false,
                false, "", ""));
        assertTrue(ZoneOperationSafetyPolicy.isTargetBusy(
                "zone-003", "valve-003",
                false, false, 0L, true,
                false, "", ""));
        assertTrue(ZoneOperationSafetyPolicy.isTargetBusy(
                "zone-003", "valve-003",
                false, false, 0L, false,
                true, "zone-003", "valve-003"));
    }

    @Test
    public void unownedRunningHardwareRemainsFailSafe() {
        assertTrue(ZoneOperationSafetyPolicy.isTargetBusy(
                "zone-003", "valve-003",
                false, false, 0L, false,
                true, "", ""));
    }
}
