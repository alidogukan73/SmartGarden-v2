package com.alidogukan.avora.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class FertilizerApplicationTest {
    @Test
    public void sharedApplicationBelongsToEveryRecordedCropSeason() {
        FertilizerApplication application = new FertilizerApplication();
        application.setSeason_id("tomato");
        application.setSeason_ids(List.of("tomato", "pepper"));

        assertTrue(application.belongsToSeason("tomato"));
        assertTrue(application.belongsToSeason("pepper"));
        assertFalse(application.belongsToSeason("bean"));
    }

    @Test
    public void legacyApplicationFallsBackToPrimarySeason() {
        FertilizerApplication application = new FertilizerApplication();
        application.setSeason_id("tomato");

        assertTrue(application.belongsToSeason("tomato"));
        assertFalse(application.belongsToSeason("pepper"));
    }
}
