package com.ali.smartgarden.fertilization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.FertilizerApplication;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class FertilizerExperiencePatternAdvisorTest {

    private static final long NOW = 2_000_000_000L;

    @Test
    public void repeatedPositivePatternBeatsRepeatedProblemPattern() {
        FertilizerExperiencePatternAdvisor.Result result =
                FertilizerExperiencePatternAdvisor.evaluate(
                        "zone-001",
                        "product-001",
                        "Ürün A",
                        Arrays.asList(
                                application(100, "g", "Damlama",
                                        "IMPROVED", 5, false),
                                application(100, "g", "Damlama",
                                        "IMPROVED", 4, false),
                                application(150, "g", "Yapraktan",
                                        "ISSUE", 1, false),
                                application(150, "g", "Yapraktan",
                                        "ISSUE", 2, false)
                        ),
                        NOW
                );

        assertTrue(result.isAvailable());
        assertTrue(result.isComparative());
        assertEquals(2, result.getSupportedPatternCount());
        assertEquals(100.0, result.getBestPattern().getDose(), 0.01);
        assertEquals("g", result.getBestPattern().getUnit());
        assertEquals("Damlama", result.getBestPattern().getMethod());
        assertEquals(2, result.getBestPattern().getObservations());
        assertTrue(result.getBestPattern().getSuccessScore() >= 80);
    }

    @Test
    public void oneObservationCannotCreateLearnedPattern() {
        FertilizerExperiencePatternAdvisor.Result result =
                FertilizerExperiencePatternAdvisor.evaluate(
                        "zone-001",
                        "product-001",
                        "Ürün A",
                        Collections.singletonList(
                                application(100, "g", "Damlama",
                                        "IMPROVED", 5, false)
                        ),
                        NOW
                );

        assertFalse(result.isAvailable());
        assertEquals(1, result.getEvaluatedCount());
        assertEquals(2, result.getMinimumPatternObservations());
    }

    @Test
    public void mixedApplicationsAreExcludedFromSingleProductPattern() {
        FertilizerExperiencePatternAdvisor.Result result =
                FertilizerExperiencePatternAdvisor.evaluate(
                        "zone-001",
                        "product-001",
                        "Ürün A",
                        Arrays.asList(
                                application(100, "g", "Damlama",
                                        "IMPROVED", 5, true),
                                application(100, "g", "Damlama",
                                        "IMPROVED", 5, true)
                        ),
                        NOW
                );

        assertFalse(result.isAvailable());
        assertEquals(2, result.getExcludedMixedCount());
    }

    @Test
    public void anotherZoneAndProductDoNotTeachPattern() {
        FertilizerApplication otherZone = application(
                100, "g", "Damlama", "IMPROVED", 5, false
        );
        otherZone.setZone_id("zone-002");
        FertilizerApplication otherProduct = application(
                100, "g", "Damlama", "IMPROVED", 5, false
        );
        otherProduct.setProduct_id("product-002");
        otherProduct.setProduct_name("Ürün B");

        FertilizerExperiencePatternAdvisor.Result result =
                FertilizerExperiencePatternAdvisor.evaluate(
                        "zone-001",
                        "product-001",
                        "Ürün A",
                        Arrays.asList(otherZone, otherProduct),
                        NOW
                );

        assertFalse(result.isAvailable());
        assertEquals(0, result.getEvaluatedCount());
    }

    private static FertilizerApplication application(
            double dose,
            String unit,
            String method,
            String status,
            int vigor,
            boolean mixed
    ) {
        FertilizerApplication application = new FertilizerApplication();
        application.setZone_id("zone-001");
        application.setProduct_id("product-001");
        application.setProduct_name("Ürün A");
        application.setApplied_dose(dose);
        application.setDose_unit(unit);
        application.setApplication_method(method);
        application.setApplied_at_epoch(NOW - 10L * 86_400L);
        application.setOutcome_observed_at_epoch(NOW - 5L * 86_400L);
        application.setOutcome_status(status);
        application.setOutcome_vigor_score(vigor);
        if (mixed) application.setMix_group_id("mix-001");
        return application;
    }
}
