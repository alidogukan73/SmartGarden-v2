package com.ali.smartgarden.fertilization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;

import org.junit.Test;

public class FertilizerDataFreshnessPolicyTest {

    private static final long NOW = 2_000_000_000L;

    @Test
    public void sensorUsesSameNinetySecondLiveWindowAsApplication() {
        GardenZone zone = zone();
        zone.setUpdated_at_epoch(NOW - 90L);
        assertTrue(FertilizerDataFreshnessPolicy.isSensorFresh(zone, NOW));

        zone.setUpdated_at_epoch(NOW - 91L);
        assertFalse(FertilizerDataFreshnessPolicy.isSensorFresh(zone, NOW));
    }

    @Test
    public void millisecondSensorTimestampIsNormalized() {
        GardenZone zone = zone();
        zone.setUpdated_at_epoch((NOW - 30L) * 1000L);
        assertTrue(FertilizerDataFreshnessPolicy.isSensorFresh(zone, NOW));
    }

    @Test
    public void weatherOlderThanSixHoursIsIgnored() {
        WeatherForecast weather = new WeatherForecast("Düzce", "Merkez",
                35.0, 0.0, 0.0, 10.0);
        weather.setUpdatedAtEpoch(NOW - 6L * 60L * 60L - 1L);
        assertFalse(FertilizerDataFreshnessPolicy.isWeatherFresh(weather, NOW));
    }

    @Test
    public void waterAnalysisOlderThanThirtyDaysIsStale() {
        FertilizationProfile profile = new FertilizationProfile();
        profile.setWater_ph(7.8);
        profile.setWater_analysis_updated_at_epoch(
                NOW - 30L * 24L * 60L * 60L - 1L);
        assertFalse(FertilizerDataFreshnessPolicy.isWaterAnalysisFresh(profile, NOW));
    }

    private static GardenZone zone() {
        GardenZone zone = new GardenZone();
        zone.setSensor_enabled(true);
        zone.setSensor_id("soil-001");
        return zone;
    }
}
