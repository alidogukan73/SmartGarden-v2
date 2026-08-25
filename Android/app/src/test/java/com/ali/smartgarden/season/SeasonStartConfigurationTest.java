package com.ali.smartgarden.season;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.GardenZone;

import org.junit.Test;

public class SeasonStartConfigurationTest {

    @Test
    public void customCropCreatesStablePlantType() {
        assertEquals("kirmizi_havuc", SeasonStartConfiguration.customPlantType("Kırmızı Havuç"));
    }

    @Test
    public void customCropSuggestsMatchingEmoji() {
        assertEquals("🥕", SeasonStartConfiguration.suggestedCropEmoji("Havuç"));
        assertEquals("🥬", SeasonStartConfiguration.suggestedCropEmoji("Kıvırcık marul"));
        assertEquals("🌱", SeasonStartConfiguration.suggestedCropEmoji("Tanınmayan deneme ürünü"));
    }

    @Test
    public void blankSensorCanNeverBeEnabled() {
        SeasonStartConfiguration configuration = new SeasonStartConfiguration(
                "Havuç", "havuc", "🥕", "", true
        );

        assertTrue(configuration.isValid());
        assertFalse(configuration.isSensorEnabled());
    }

    @Test
    public void sensorIdentityOrEnabledStateChangeIsDetected() {
        SeasonStartConfiguration same = new SeasonStartConfiguration(
                "Domates", "tomato", "🍅", "soil-001", true
        );
        SeasonStartConfiguration changed = new SeasonStartConfiguration(
                "Domates", "tomato", "🍅", "soil-002", true
        );

        assertFalse(same.changesSensor("soil-001", true));
        assertTrue(same.changesSensor("soil-001", false));
        assertTrue(changed.changesSensor("soil-001", true));
    }

    @Test
    public void existingZoneConfigurationIsPreserved() {
        GardenZone zone = new GardenZone();
        zone.setName("Biber");
        zone.setPlant_type("pepper");
        zone.setEmoji("🌶️");
        zone.setSensor_id("soil-002");
        zone.setSensor_enabled(true);

        SeasonStartConfiguration configuration = SeasonStartConfiguration.fromZone(zone);

        assertEquals("Biber", configuration.getCropName());
        assertEquals("pepper", configuration.getPlantType());
        assertEquals("soil-002", configuration.getSensorId());
        assertTrue(configuration.isSensorEnabled());
    }
}
