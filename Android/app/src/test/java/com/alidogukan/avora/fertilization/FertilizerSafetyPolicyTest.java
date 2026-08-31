package com.alidogukan.avora.fertilization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.FertilizerProduct;

import org.junit.Test;

import java.util.Arrays;

public class FertilizerSafetyPolicyTest {

    @Test
    public void chemicalProductIsAllowedBeforeFruitFormation() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.FLOWERING);
        FertilizerProduct product = product(FertilizerStagePolicy.FLOWERING, false);

        assertTrue(FertilizerSafetyPolicy.isEligible(product, profile));
    }

    @Test
    public void chemicalProductIsRejectedDuringFruitFormation() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.FRUITING);
        FertilizerProduct product = product(FertilizerStagePolicy.FRUITING, false);

        FertilizerSafetyPolicy.Result result =
                FertilizerSafetyPolicy.evaluate(product, profile);

        assertFalse(result.isAllowed());
        assertEquals(
                FertilizerSafetyPolicy.RejectionReason.ORGANIC_PRODUCT_REQUIRED,
                result.getRejectionReason()
        );
    }

    @Test
    public void organicCategoryWithoutExplicitApprovalIsRejectedDuringFruitFormation() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.FRUITING);
        FertilizerProduct product = product(FertilizerStagePolicy.FRUITING, false);
        product.setApplication_type("ORGANIC");

        FertilizerSafetyPolicy.Result result =
                FertilizerSafetyPolicy.evaluate(product, profile);

        assertFalse(result.isAllowed());
        assertEquals(
                FertilizerSafetyPolicy.RejectionReason.ORGANIC_PRODUCT_REQUIRED,
                result.getRejectionReason()
        );
    }

    @Test
    public void stagePickerUsesTheSameOrganicApprovalRule() {
        FertilizerProduct unapproved = product(FertilizerStagePolicy.FRUITING, false);
        FertilizerProduct approved = product(FertilizerStagePolicy.FRUITING, true);

        assertFalse(FertilizerSafetyPolicy.isEligibleForStage(
                unapproved,
                FertilizerStagePolicy.FRUITING
        ));
        assertTrue(FertilizerSafetyPolicy.isEligibleForStage(
                approved,
                FertilizerStagePolicy.FRUITING
        ));
    }

    @Test
    public void organicEligibleProductIsAllowedDuringHarvest() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.HARVEST);
        FertilizerProduct product = product(FertilizerStagePolicy.HARVEST, true);

        assertTrue(FertilizerSafetyPolicy.isEligible(product, profile));
    }

    @Test
    public void productConfiguredForAnotherStageIsRejected() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.VEGETATIVE);
        FertilizerProduct product = product(FertilizerStagePolicy.FLOWERING, false);

        FertilizerSafetyPolicy.Result result =
                FertilizerSafetyPolicy.evaluate(product, profile);

        assertFalse(result.isAllowed());
        assertEquals(
                FertilizerSafetyPolicy.RejectionReason.STAGE_INCOMPATIBLE,
                result.getRejectionReason()
        );
    }

    @Test
    public void seasonEndRejectsEveryProduct() {
        FertilizationProfile profile = profile(FertilizerStagePolicy.SEASON_END);
        FertilizerProduct product = product(FertilizerStagePolicy.HARVEST, true);

        FertilizerSafetyPolicy.Result result =
                FertilizerSafetyPolicy.evaluate(product, profile);

        assertFalse(result.isAllowed());
        assertEquals(
                FertilizerSafetyPolicy.RejectionReason.SEASON_CLOSED,
                result.getRejectionReason()
        );
    }

    private static FertilizationProfile profile(String stage) {
        FertilizationProfile profile = new FertilizationProfile();
        profile.setEnabled(true);
        profile.setGrowth_stage(stage);
        return profile;
    }

    private static FertilizerProduct product(String stage, boolean organic) {
        FertilizerProduct product = new FertilizerProduct();
        product.setEnabled(true);
        product.setRecommended_stages(Arrays.asList(stage));
        product.setOrganic_farming_eligible(organic);
        product.setApplication_type(organic ? "ORGANIC" : "NUTRITION");
        return product;
    }
}
