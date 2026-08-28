package com.ali.smartgarden.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.DeviceInfoSnapshot;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.Status;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class DeviceInfoViewModelTest {
    private static final long NOW_MILLIS = 1_700_000_000_000L;

    @Test
    public void mapCountsOnlyEnabledConfiguredZones() {
        Status status = status(true, NOW_MILLIS);
        GardenZone active = zone(true, "soil-001", true, "PHYSICAL", "valve-001");
        GardenZone enabledWithoutHardware = zone(true, "", false, "SIMULATED", "");
        GardenZone inactive = zone(false, "soil-008", true, "PHYSICAL", "valve-008");
        DeviceInfoSnapshot snapshot = new DeviceInfoSnapshot(
                status, null, Arrays.asList(active, enabledWithoutHardware, inactive),
                Collections.singleton("2.1.1"));

        DeviceInfoViewModel.DeviceInfoState result =
                DeviceInfoViewModel.map(snapshot, NOW_MILLIS);

        assertTrue(result.connected);
        assertEquals(2, result.enabledZones);
        assertEquals(1, result.enabledSensors);
        assertEquals(1, result.physicalValves);
        assertEquals(Collections.singleton("2.1.1"), result.firmwareVersions);
    }

    @Test
    public void mapRejectsStaleOnlineHeartbeat() {
        Status status = status(true, NOW_MILLIS - 181_000L);
        DeviceInfoSnapshot snapshot = new DeviceInfoSnapshot(
                status, null, Collections.emptyList(), Collections.emptySet());

        DeviceInfoViewModel.DeviceInfoState result =
                DeviceInfoViewModel.map(snapshot, NOW_MILLIS);

        assertFalse(result.connected);
        assertEquals(0, result.enabledZones);
    }

    private static Status status(boolean online, long lastSeenMillis) {
        Status status = new Status();
        status.setOnline(online);
        status.setLastSeenEpoch(lastSeenMillis / 1000L);
        return status;
    }

    private static GardenZone zone(boolean enabled, String sensorId,
                                   boolean sensorEnabled, String valveMode,
                                   String valveId) {
        GardenZone zone = new GardenZone();
        zone.setEnabled(enabled);
        zone.setSensor_id(sensorId);
        zone.setSensor_enabled(sensorEnabled);
        zone.setValve_mode(valveMode);
        zone.setValve_id(valveId);
        return zone;
    }
}
