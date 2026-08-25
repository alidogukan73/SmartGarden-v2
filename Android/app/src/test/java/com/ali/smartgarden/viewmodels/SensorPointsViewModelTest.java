package com.ali.smartgarden.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;

import org.junit.Test;

import java.util.List;

public class SensorPointsViewModelTest {

    @Test
    public void alwaysBuildsEightPhysicalSensorPoints() {
        GardenZone tomato = zone(1, "soil-001", true);
        GardenZone pepper = zone(2, "soil-002", true);
        GardenZone noSensor = zone(7, "", true);
        GardenZone inactive = zone(8, "soil-008", false);

        List<GardenZone> points = SensorPointsViewModel.buildSensorPoints(
                List.of(tomato, pepper, noSensor, inactive)
        );

        assertEquals(8, points.size());
        assertSame(tomato, points.get(0));
        assertSame(pepper, points.get(1));
        assertEquals("soil-003", points.get(2).getSensor_id());
        assertFalse(points.get(2).isSensor_enabled());
        assertEquals("", points.get(2).getZone_id());
        assertEquals("soil-008", points.get(7).getSensor_id());
        assertFalse(points.get(7).isSensor_enabled());
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
