package com.ali.smartgarden.zones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.ali.smartgarden.models.GardenZone;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ZoneCapacityPolicyTest {
    @Test
    public void supportsExactlyEightChannels() {
        assertEquals("zone-008", ZoneCapacityPolicy.zoneId(8));
        assertEquals("soil-008", ZoneCapacityPolicy.sensorId(8));
        assertEquals("valve-008", ZoneCapacityPolicy.valveId(8));
        assertThrows(IllegalArgumentException.class, () -> ZoneCapacityPolicy.zoneId(9));
    }

    @Test
    public void nextSlotFollowsExistingFiveWithoutCreatingBlanks() {
        List<GardenZone> zones = new ArrayList<>();
        for (int slot = 1; slot <= 5; slot++) zones.add(zone(slot, true));
        assertEquals(6, ZoneCapacityPolicy.nextAvailableSlot(zones));
    }

    @Test
    public void ninthZoneIsBlockedWhenAllChannelsAreActive() {
        List<GardenZone> zones = new ArrayList<>();
        for (int slot = 1; slot <= 8; slot++) zones.add(zone(slot, true));
        assertEquals(-1, ZoneCapacityPolicy.nextAvailableSlot(zones));
    }

    @Test
    public void duplicateSensorAndValveAreRejected() {
        GardenZone first = zone(1, true);
        GardenZone duplicateSensor = zone(2, true);
        duplicateSensor.setSensor_id("soil-001");
        IllegalArgumentException sensorError = assertThrows(
                IllegalArgumentException.class,
                () -> ZoneCapacityPolicy.validateCandidate(duplicateSensor, List.of(first)));
        assertEquals(ZoneCapacityPolicy.ERROR_SENSOR_IN_USE, sensorError.getMessage());

        GardenZone duplicateValve = zone(2, true);
        duplicateValve.setValve_id("valve-001");
        IllegalArgumentException valveError = assertThrows(
                IllegalArgumentException.class,
                () -> ZoneCapacityPolicy.validateCandidate(duplicateValve, List.of(first)));
        assertEquals(ZoneCapacityPolicy.ERROR_VALVE_IN_USE, valveError.getMessage());
    }

    @Test
    public void inactiveZoneReleasesItsHardwareAndSlot() {
        GardenZone archived = zone(3, false);
        archived.setLifecycle_status(ZoneCapacityPolicy.LIFECYCLE_INACTIVE);
        List<GardenZone> existingSlots = new ArrayList<>();
        for (int slot = 1; slot <= 8; slot++) {
            existingSlots.add(slot == 3 ? archived : zone(slot, true));
        }
        GardenZone candidate = zone(1, true);
        candidate.setSensor_id("soil-003");
        candidate.setValve_id("valve-003");
        ZoneCapacityPolicy.validateCandidate(candidate, List.of(archived));
        assertEquals(3, ZoneCapacityPolicy.nextAvailableSlot(existingSlots));
        assertFalse(candidate.getSensor_id().isEmpty());
    }

    @Test
    public void activeZoneListExcludesDeactivatedAndInvalidRecords() {
        GardenZone active = zone(1, true);
        GardenZone disabled = zone(2, false);
        GardenZone archived = zone(3, true);
        archived.setLifecycle_status(ZoneCapacityPolicy.LIFECYCLE_INACTIVE);
        GardenZone invalid = new GardenZone();
        invalid.setZone_id("zone-009");
        invalid.setEnabled(true);

        List<GardenZone> visible = ZoneCapacityPolicy.activeZones(
                List.of(active, disabled, archived, invalid));

        assertEquals(1, visible.size());
        assertEquals("zone-001", visible.get(0).getZone_id());
    }


    @Test
    public void activeOrUnfinishedSeasonProtectsZoneFromDeactivation() {
        assertEquals(true, ZoneCapacityPolicy.hasProtectedSeason("ACTIVE", "season-1"));
        assertEquals(true, ZoneCapacityPolicy.hasProtectedSeason("PLANNED", "season-2"));
        assertEquals(true, ZoneCapacityPolicy.hasProtectedSeason("", "season-legacy"));
        assertEquals(false, ZoneCapacityPolicy.hasProtectedSeason("CLOSED", "season-1"));
        assertEquals(false, ZoneCapacityPolicy.hasProtectedSeason("", ""));
    }


    private static GardenZone zone(int slot, boolean enabled) {
        GardenZone zone = new GardenZone();
        zone.setZone_id(ZoneCapacityPolicy.zoneId(slot));
        zone.setSensor_id(ZoneCapacityPolicy.sensorId(slot));
        zone.setValve_id(ZoneCapacityPolicy.valveId(slot));
        zone.setEnabled(enabled);
        zone.setOrder(slot);
        return zone;
    }
}
