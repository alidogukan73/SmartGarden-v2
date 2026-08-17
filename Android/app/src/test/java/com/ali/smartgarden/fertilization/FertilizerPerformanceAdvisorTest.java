package com.ali.smartgarden.fertilization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FertilizerPerformanceAdvisorTest {

    private static final long NOW = 2_000_000_000L;

    @Test
    public void oneOutcomeDoesNotChangeRanking() {
        FertilizerPerformanceAdvisor.Result result =
                FertilizerPerformanceAdvisor.evaluate(
                        zone("zone-001"),
                        product("product-001", "Ürün A"),
                        Collections.singletonList(application(
                                "zone-001", "product-001", "Ürün A",
                                "IMPROVED", 5, false
                        )),
                        NOW
                );

        assertFalse(result.hasEvidence());
        assertEquals(1, result.getObservations());
        assertEquals(2, result.getMinimumObservations());
        assertEquals(0, result.getRankingAdjustment());
    }

    @Test
    public void repeatedPositiveOutcomesGiveSmallPositiveAdjustment() {
        FertilizerPerformanceAdvisor.Result result =
                FertilizerPerformanceAdvisor.evaluate(
                        zone("zone-001"),
                        product("product-001", "Ürün A"),
                        Arrays.asList(
                                application("zone-001", "product-001", "Ürün A",
                                        "IMPROVED", 5, false),
                                application("zone-001", "product-001", "Ürün A",
                                        "IMPROVED", 4, false)
                        ),
                        NOW
                );

        assertTrue(result.hasEvidence());
        assertTrue(result.getSuccessScore() >= 80);
        assertTrue(result.getRankingAdjustment() > 0);
        assertTrue(result.getRankingAdjustment() <= 10);
        assertTrue(result.summary().contains("AVORA bölge deneyimi"));
    }

    @Test
    public void repeatedIssuesGiveSmallNegativeAdjustment() {
        FertilizerPerformanceAdvisor.Result result =
                FertilizerPerformanceAdvisor.evaluate(
                        zone("zone-001"),
                        product("product-001", "Ürün A"),
                        Arrays.asList(
                                application("zone-001", "product-001", "Ürün A",
                                        "ISSUE", 1, false),
                                application("zone-001", "product-001", "Ürün A",
                                        "ISSUE", 2, false)
                        ),
                        NOW
                );

        assertTrue(result.hasEvidence());
        assertTrue(result.getSuccessScore() <= 20);
        assertTrue(result.getRankingAdjustment() < 0);
        assertTrue(result.getRankingAdjustment() >= -10);
    }

    @Test
    public void outcomesFromAnotherZoneAreIgnored() {
        FertilizerPerformanceAdvisor.Result result =
                FertilizerPerformanceAdvisor.evaluate(
                        zone("zone-001"),
                        product("product-001", "Ürün A"),
                        Arrays.asList(
                                application("zone-002", "product-001", "Ürün A",
                                        "IMPROVED", 5, false),
                                application("zone-002", "product-001", "Ürün A",
                                        "IMPROVED", 5, false)
                        ),
                        NOW
                );

        assertFalse(result.hasEvidence());
    }

    @Test
    public void mixedApplicationsTeachMoreSlowly() {
        FertilizerProduct product = product("product-001", "Ürün A");
        GardenZone zone = zone("zone-001");
        List<FertilizerApplication> standalone = Arrays.asList(
                application("zone-001", "product-001", "Ürün A",
                        "IMPROVED", 5, false),
                application("zone-001", "product-001", "Ürün A",
                        "IMPROVED", 5, false)
        );
        List<FertilizerApplication> mixed = Arrays.asList(
                application("zone-001", "product-001", "Ürün A",
                        "IMPROVED", 5, true),
                application("zone-001", "product-001", "Ürün A",
                        "IMPROVED", 5, true)
        );

        int standaloneAdjustment = FertilizerPerformanceAdvisor.evaluate(
                zone, product, standalone, NOW
        ).getRankingAdjustment();
        int mixedAdjustment = FertilizerPerformanceAdvisor.evaluate(
                zone, product, mixed, NOW
        ).getRankingAdjustment();

        assertTrue(mixedAdjustment > 0);
        assertTrue(mixedAdjustment < standaloneAdjustment);
    }

    @Test
    public void decisionEngineUsesZonePerformanceToOrderEqualProducts() {
        GardenZone zone = zone("zone-001");
        FertilizerProduct successful = product("successful", "Başarılı Ürün");
        FertilizerProduct problematic = product("problematic", "Sorunlu Ürün");
        List<FertilizerApplication> history = Arrays.asList(
                application("zone-001", "successful", "Başarılı Ürün",
                        "IMPROVED", 5, false),
                application("zone-001", "successful", "Başarılı Ürün",
                        "IMPROVED", 5, false),
                application("zone-001", "problematic", "Sorunlu Ürün",
                        "ISSUE", 1, false),
                application("zone-001", "problematic", "Sorunlu Ürün",
                        "ISSUE", 1, false)
        );

        FertilizerAdvice advice = FertilizerDecisionEngine.advise(
                zone,
                Arrays.asList(problematic, successful),
                null,
                history,
                NOW
        );

        assertEquals(2, advice.getCandidates().size());
        assertTrue(advice.getCandidates().get(0).contains("Başarılı Ürün"));
        assertTrue(advice.getExperience().isAvailable());
        assertTrue(advice.getExperience().isReliable());
        assertEquals("successful", advice.getExperience().getProductId());
        assertEquals("Başarılı Ürün", advice.getExperience().getProductName());
        assertEquals(2, advice.getExperience().getObservations());
        assertTrue(advice.getExperience().getSuccessScore() >= 80);
    }


    @Test
    public void matchingOutcomesReturnsOnlySameZoneProductNewestFirst() {
        FertilizerApplication older = application(
                "zone-001", "product-001", "Ürün A",
                "IMPROVED", 4, false
        );
        older.setApplication_id("older");
        older.setOutcome_observed_at_epoch(NOW - 8L * 86_400L);

        FertilizerApplication newer = application(
                "zone-001", "product-001", "Ürün A",
                "UNCHANGED", 3, false
        );
        newer.setApplication_id("newer");
        newer.setOutcome_observed_at_epoch(NOW - 2L * 86_400L);

        FertilizerApplication otherZone = application(
                "zone-002", "product-001", "Ürün A",
                "IMPROVED", 5, false
        );
        FertilizerApplication otherProduct = application(
                "zone-001", "product-002", "Ürün B",
                "IMPROVED", 5, false
        );
        FertilizerApplication pending = application(
                "zone-001", "product-001", "Ürün A",
                "", 0, false
        );

        List<FertilizerApplication> matches =
                FertilizerPerformanceAdvisor.matchingOutcomes(
                        "zone-001",
                        "product-001",
                        "Ürün A",
                        Arrays.asList(
                                older,
                                otherZone,
                                newer,
                                otherProduct,
                                pending
                        ),
                        NOW
                );

        assertEquals(2, matches.size());
        assertEquals("newer", matches.get(0).getApplication_id());
        assertEquals("older", matches.get(1).getApplication_id());
    }

    private static GardenZone zone(String zoneId) {
        FertilizationProfile profile = new FertilizationProfile();
        profile.setEnabled(true);
        profile.setGrowth_stage("FRUITING");
        profile.setArea_m2(20.0);
        profile.setTank_liters(100.0);

        GardenZone zone = new GardenZone();
        zone.setZone_id(zoneId);
        zone.setName("Domates");
        zone.setMoisture_limit(40);
        zone.setMoisture(60);
        zone.setUpdated_at_epoch(NOW);
        zone.setFertilization(profile);
        return zone;
    }

    private static FertilizerProduct product(String productId, String name) {
        FertilizerProduct product = new FertilizerProduct();
        product.setProduct_id(productId);
        product.setName(name);
        product.setNpk("10-5-40");
        product.setApplication_type("ORGANIC");
        product.setOrganic_farming_eligible(true);
        product.setDosage_unit("kg/dekar");
        product.setLabel_dosage_min(5.0);
        product.setLabel_dosage_max(5.0);
        product.setRecommended_stages(Collections.singletonList("FRUITING"));
        product.setStock_unit("g");
        product.setStock_amount(2_000.0);
        product.setEnabled(true);
        return product;
    }

    private static FertilizerApplication application(
            String zoneId,
            String productId,
            String productName,
            String outcomeStatus,
            int vigorScore,
            boolean mixed
    ) {
        FertilizerApplication application = new FertilizerApplication();
        application.setZone_id(zoneId);
        application.setProduct_id(productId);
        application.setProduct_name(productName);
        application.setApplied_at_epoch(NOW - 10L * 86_400L);
        application.setOutcome_observed_at_epoch(NOW - 5L * 86_400L);
        application.setOutcome_status(outcomeStatus);
        application.setOutcome_vigor_score(vigorScore);
        if (mixed) application.setMix_group_id("mix-001");
        return application;
    }
}
