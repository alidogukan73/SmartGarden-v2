package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.FertilizerProduct;

/**
 * Single source of truth for product eligibility in fertilizer recommendations
 * and planned application flows.
 *
 * <p>Historical records are intentionally handled separately by the UI: a
 * farmer must still be able to record what was actually applied, even when it
 * conflicts with this policy. Such records must be saved as an explicit
 * override rather than silently presented as a safe recommendation.</p>
 */
public final class FertilizerSafetyPolicy {

    public enum RejectionReason {
        NONE,
        PRODUCT_MISSING,
        PRODUCT_DISABLED,
        PROFILE_MISSING,
        PLAN_DISABLED,
        STAGE_MISSING,
        SEASON_CLOSED,
        STAGE_INCOMPATIBLE,
        ORGANIC_PRODUCT_REQUIRED
    }

    private FertilizerSafetyPolicy() {
    }

    public static Result evaluate(
            FertilizerProduct product,
            FertilizationProfile profile
    ) {
        if (product == null) {
            return Result.rejected(RejectionReason.PRODUCT_MISSING);
        }
        if (!product.isEnabled()) {
            return Result.rejected(RejectionReason.PRODUCT_DISABLED);
        }
        if (profile == null) {
            return Result.rejected(RejectionReason.PROFILE_MISSING);
        }
        if (!profile.isEnabled()) {
            return Result.rejected(RejectionReason.PLAN_DISABLED);
        }

        String stage = FertilizerStagePolicy.normalize(profile.getGrowth_stage());
        if (stage.isEmpty()) {
            return Result.rejected(RejectionReason.STAGE_MISSING);
        }
        if (FertilizerStagePolicy.SEASON_END.equals(stage)) {
            return Result.rejected(RejectionReason.SEASON_CLOSED);
        }
        if (!FertilizerApplicationSafety.isStageCompatible(product, profile)) {
            return Result.rejected(RejectionReason.STAGE_INCOMPATIBLE);
        }
        if (requiresOrganicProduct(profile) && !isOrganicCompatible(product)) {
            return Result.rejected(RejectionReason.ORGANIC_PRODUCT_REQUIRED);
        }
        return Result.allowed();
    }

    public static boolean isEligible(
            FertilizerProduct product,
            FertilizationProfile profile
    ) {
        return evaluate(product, profile).isAllowed();
    }

    /**
     * Eligibility used while building a stage-specific product picker.
     * The draft profile is intentionally enabled so the picker validates the
     * product and stage without depending on whether the user has saved the
     * plan switch yet.
     */
    public static boolean isEligibleForStage(
            FertilizerProduct product,
            String growthStage
    ) {
        FertilizationProfile draftProfile = new FertilizationProfile();
        draftProfile.setEnabled(true);
        draftProfile.setGrowth_stage(growthStage);
        return isEligible(product, draftProfile);
    }

    /**
     * AVORA's current production policy: once fruit formation begins, only
     * products suitable for organic farming may be recommended.
     */
    public static boolean requiresOrganicProduct(FertilizationProfile profile) {
        if (profile == null) return false;
        String stage = FertilizerStagePolicy.normalize(profile.getGrowth_stage());
        return FertilizerStagePolicy.FRUITING.equals(stage)
                || FertilizerStagePolicy.HARVEST.equals(stage);
    }

    public static boolean isOrganicCompatible(FertilizerProduct product) {
        // Product category or name is not proof of organic-farming approval.
        // The explicit user/catalog approval is the single source of truth.
        return product != null && product.isOrganic_farming_eligible();
    }

    public static final class Result {
        private final boolean allowed;
        private final RejectionReason rejectionReason;

        private Result(boolean allowed, RejectionReason rejectionReason) {
            this.allowed = allowed;
            this.rejectionReason = rejectionReason;
        }

        public static Result allowed() {
            return new Result(true, RejectionReason.NONE);
        }

        public static Result rejected(RejectionReason reason) {
            return new Result(false, reason == null
                    ? RejectionReason.PRODUCT_MISSING : reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public RejectionReason getRejectionReason() {
            return rejectionReason;
        }
    }
}
