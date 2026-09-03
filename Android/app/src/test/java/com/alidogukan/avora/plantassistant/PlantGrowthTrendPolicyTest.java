package com.alidogukan.avora.plantassistant;

import static org.junit.Assert.assertEquals;

import com.alidogukan.avora.models.GardenPhoto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PlantGrowthTrendPolicyTest {

    @Test
    public void firstGrowthAssessmentCreatesBaseline() {
        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Collections.emptyList(), "zone-001", "new", 70);

        assertEquals(PlantGrowthTrendPolicy.FIRST_RECORD, result.trend);
        assertEquals(0, result.scoreDelta);
        assertEquals(0L, result.previousCapturedAtEpoch);
    }

    @Test
    public void meaningfulPositiveChangeIsImproving() {
        GardenPhoto previous = growth("old", "zone-001", 70, 100L);

        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Collections.singletonList(previous), "zone-001", "new", 76);

        assertEquals(PlantGrowthTrendPolicy.IMPROVING, result.trend);
        assertEquals(6, result.scoreDelta);
        assertEquals(100L, result.previousCapturedAtEpoch);
    }

    @Test
    public void smallVisualDifferenceIsStable() {
        GardenPhoto previous = growth("old", "zone-001", 70, 100L);

        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Collections.singletonList(previous), "zone-001", "new", 67);

        assertEquals(PlantGrowthTrendPolicy.STABLE, result.trend);
        assertEquals(-3, result.scoreDelta);
    }

    @Test
    public void meaningfulNegativeChangeIsDeclining() {
        GardenPhoto previous = growth("old", "zone-001", 70, 100L);

        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Collections.singletonList(previous), "zone-001", "new", 64);

        assertEquals(PlantGrowthTrendPolicy.DECLINING, result.trend);
        assertEquals(-6, result.scoreDelta);
    }

    @Test
    public void comparisonUsesNewestOtherRecordFromSameZone() {
        GardenPhoto older = growth("older", "zone-001", 40, 100L);
        GardenPhoto newest = growth("newest", "zone-001", 70, 300L);
        GardenPhoto current = growth("current", "zone-001", 10, 400L);
        GardenPhoto otherZone = growth("other", "zone-002", 95, 500L);
        GardenPhoto health = growth("health", "zone-001", 5, 600L);
        health.setAnalysis_goal("health_screening");

        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Arrays.asList(older, newest, current, otherZone, health),
                "zone-001", "current", 73);

        assertEquals(PlantGrowthTrendPolicy.STABLE, result.trend);
        assertEquals(3, result.scoreDelta);
        assertEquals(300L, result.previousCapturedAtEpoch);
    }

    @Test
    public void comparisonDoesNotMixCropsSharingOnePhysicalZone() {
        GardenPhoto tomato = growth("tomato", "zone-001", 90, 300L);
        tomato.setSeason_id("tomato-season");
        GardenPhoto pepper = growth("pepper", "zone-001", 60, 200L);
        pepper.setSeason_id("pepper-season");

        PlantGrowthTrendPolicy.Result result = PlantGrowthTrendPolicy.compare(
                Arrays.asList(tomato, pepper),
                "zone-001", "pepper-season", "new", 64);

        assertEquals(PlantGrowthTrendPolicy.STABLE, result.trend);
        assertEquals(4, result.scoreDelta);
        assertEquals(200L, result.previousCapturedAtEpoch);
    }

    private static GardenPhoto growth(String id, String zoneId, int score, long epoch) {
        GardenPhoto photo = new GardenPhoto();
        photo.setId(id);
        photo.setZone_id(zoneId);
        photo.setAnalysis_goal("growth_status");
        photo.setGrowth_score(score);
        photo.setCaptured_at_epoch(epoch);
        return photo;
    }
}
