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

public class FertilizerHistoryRiskAdvisorTest {

    private static final long NOW = 2_000_000_000L;
    private static final long DAY = 86_400L;

    @Test
    public void threeNutritionEventsInSevenDaysAreFlagged() {
        List<FertilizerApplication> history = Arrays.asList(
                application("nitrogen", NOW - DAY, ""),
                application("nitrogen", NOW - 2L * DAY, ""),
                application("nitrogen", NOW - 3L * DAY, "")
        );

        List<String> risks = FertilizerHistoryRiskAdvisor.assess(
                zone("VEGETATIVE"),
                Collections.singletonList(product("nitrogen", "20-5-5")),
                history,
                NOW
        );

        assertTrue(contains(risks, "Son 7"));
        assertTrue(contains(risks, "azot"));
    }

    @Test
    public void twoProductsInOneMixCountAsOneApplication() {
        FertilizerApplication first = application("nitrogen", NOW - DAY, "mix-1");
        FertilizerApplication second = application("potassium", NOW - DAY, "mix-1");
        FertilizerApplication separate = application(
                "nitrogen", NOW - 2L * DAY, ""
        );

        List<String> risks = FertilizerHistoryRiskAdvisor.assess(
                zone("VEGETATIVE"),
                Arrays.asList(
                        product("nitrogen", "20-5-5"),
                        product("potassium", "10-5-40")
                ),
                Arrays.asList(first, second, separate),
                NOW
        );

        assertFalse(contains(risks, "Son 7"));
    }

    @Test
    public void excessiveRecordedDoseIsFlagged() {
        FertilizerApplication application = application(
                "balanced", NOW - DAY, ""
        );
        application.setApplied_dose(125.0);
        application.setRecommended_dose_max(100.0);

        List<String> risks = FertilizerHistoryRiskAdvisor.assess(
                zone("VEGETATIVE"),
                Collections.singletonList(product("balanced", "20-20-20")),
                Collections.singletonList(application),
                NOW
        );

        assertTrue(contains(risks, "doz"));
    }

    @Test
    public void problemOutcomeIsShownFirst() {
        FertilizerApplication application = application(
                "balanced", NOW - DAY, ""
        );
        application.setOutcome_status("ISSUE");

        List<String> risks = FertilizerHistoryRiskAdvisor.assess(
                zone("VEGETATIVE"),
                Collections.singletonList(product("balanced", "20-20-20")),
                Collections.singletonList(application),
                NOW
        );

        assertFalse(risks.isEmpty());
        assertTrue(risks.get(0).contains("sorun"));
    }

    @Test
    public void fruitingStageReviewsRecentPotassiumHistory() {
        FertilizerProduct potassium = product("potassium", "10-5-40");

        List<String> missing = FertilizerHistoryRiskAdvisor.assess(
                zone("FRUITING"),
                Collections.singletonList(potassium),
                Collections.emptyList(),
                NOW
        );
        List<String> present = FertilizerHistoryRiskAdvisor.assess(
                zone("FRUITING"),
                Collections.singletonList(potassium),
                Collections.singletonList(
                        application("potassium", NOW - 5L * DAY, "")
                ),
                NOW
        );

        assertTrue(contains(missing, "21"));
        assertEquals(0, present.size());
    }

    private static GardenZone zone(String stage) {
        GardenZone zone = new GardenZone();
        zone.setZone_id("zone-001");
        zone.setName("Domates");
        FertilizationProfile profile = new FertilizationProfile();
        profile.setEnabled(true);
        profile.setGrowth_stage(stage);
        zone.setFertilization(profile);
        return zone;
    }

    private static FertilizerProduct product(String id, String npk) {
        FertilizerProduct product = new FertilizerProduct();
        product.setProduct_id(id);
        product.setName(id);
        product.setNpk(npk);
        return product;
    }

    private static FertilizerApplication application(
            String productId,
            long appliedAt,
            String mixGroupId
    ) {
        FertilizerApplication application = new FertilizerApplication();
        application.setZone_id("zone-001");
        application.setProduct_id(productId);
        application.setProduct_name(productId);
        application.setApplication_type("NUTRITION");
        application.setApplied_at_epoch(appliedAt);
        application.setMix_group_id(mixGroupId);
        return application;
    }

    private static boolean contains(List<String> values, String text) {
        for (String value : values) {
            if (value != null && value.contains(text)) return true;
        }
        return false;
    }
}
