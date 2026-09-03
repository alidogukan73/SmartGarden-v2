package com.alidogukan.avora.season;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.SeasonStatus;

import org.junit.Test;

import java.util.List;

public final class SharedIrrigationCompatibilityTest {
    @Test
    public void overlappingCropRangesProduceOneSharedInterval() {
        GardenSeason tomato = season(35, 65);
        SeasonStartConfiguration pepper = new SeasonStartConfiguration(
                "Biber", "pepper", "🌶️", 40, 70);

        SharedIrrigationCompatibility.Result result =
                SharedIrrigationCompatibility.evaluate(List.of(tomato), pepper);

        assertTrue(result.isCompatible());
        assertEquals(40, result.getCommonMin());
        assertEquals(65, result.getCommonMax());
    }

    @Test
    public void disjointRangesAreRejectedForAutomaticSharedWatering() {
        GardenSeason dryCrop = season(15, 30);
        SeasonStartConfiguration wetCrop = new SeasonStartConfiguration(
                "Su seven", "wet", "🌱", 55, 75);

        assertFalse(SharedIrrigationCompatibility
                .evaluate(List.of(dryCrop), wetCrop)
                .isCompatible());
    }

    @Test
    public void closingRestrictiveCropRecomputesThresholdFromRemainingCrops() {
        GardenSeason tomato = season(35, 65);
        GardenSeason pepper = season(45, 70);

        assertEquals(45, SharedIrrigationCompatibility.commonMinimumOrFallback(
                List.of(tomato, pepper), 52));
        assertEquals(35, SharedIrrigationCompatibility.commonMinimumOrFallback(
                List.of(tomato), 52));
    }

    @Test
    public void legacyUnboundedCropKeepsExistingThreshold() {
        assertEquals(41, SharedIrrigationCompatibility.commonMinimumOrFallback(
                List.of(season(0, 100)), 41));
    }

    private static GardenSeason season(int min, int max) {
        GardenSeason value = new GardenSeason();
        value.setStatus(SeasonStatus.ACTIVE);
        value.setIdeal_moisture_min(min);
        value.setIdeal_moisture_max(max);
        return value;
    }
}
