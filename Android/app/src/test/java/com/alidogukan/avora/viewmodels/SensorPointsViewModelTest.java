package com.alidogukan.avora.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.zones.ZoneCapacityPolicy;

import org.junit.Test;

import java.util.List;

public class SensorPointsViewModelTest {

    @Test
    public void buildsOnlyAssignedActiveSensorPoints() {
        GardenZone tomato = zone(1, "soil-001", true);
        GardenZone pepper = zone(2, "soil-002", true);
        GardenZone noSensor = zone(7, "", true);
        GardenZone inactive = zone(8, "soil-008", false);

        List<GardenZone> points = SensorPointsViewModel.buildSensorPoints(
                List.of(tomato, pepper, noSensor, inactive)
        );

        assertEquals(2, points.size());
        assertSame(tomato, points.get(0));
        assertSame(pepper, points.get(1));
    }

    private static GardenZone zone(
            int slot,
            String sensorId,
            boolean enabled
    ) {
        GardenZone zone = new GardenZone();
        zone.setZone_id(ZoneCapacityPolicy.zoneId(slot));
        zone.setSensor_id(sensorId);
        zone.setSensor_enabled(!sensorId.isEmpty());
        zone.setEnabled(enabled);
        zone.setOrder(slot);
        if (!enabled) {
            zone.setLifecycle_status(ZoneCapacityPolicy.LIFECYCLE_INACTIVE);
        }
        return zone;
    }
}
