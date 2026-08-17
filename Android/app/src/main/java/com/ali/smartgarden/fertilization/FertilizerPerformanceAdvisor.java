package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Learns a small, bounded ranking adjustment from outcomes recorded by the
 * user. Agronomic suitability, repeat intervals and stock safety remain the
 * primary decision rules.
 */
public final class FertilizerPerformanceAdvisor {

    private static final int MINIMUM_OBSERVATIONS = 2;
    private static final int FULL_CONFIDENCE_OBSERVATIONS = 5;
    private static final int MAXIMUM_ADJUSTMENT = 10;
    private static final long DAY_SECONDS = 86_400L;
    private static final long MAXIMUM_AGE_DAYS = 730L;

    private FertilizerPerformanceAdvisor() { }

    public static Result evaluate(
            GardenZone zone,
            FertilizerProduct product,
            List<FertilizerApplication> history,
            long now
    ) {
        if (zone == null || product == null || history == null
                || history.isEmpty()) {
            return Result.insufficient(0);
        }

        int observations = 0;
        double weightedTotal = 0.0;
        double totalWeight = 0.0;
        for (FertilizerApplication application : history) {
            if (!eligible(zone, product, application, now)) continue;
            Double observation = outcomeScore(application);
            if (observation == null) continue;

            double weight = recencyWeight(application, now);
            if (!safe(application.getMix_group_id()).isBlank()) {
                // A mixed application cannot establish which product caused
                // the result, so it teaches the ranking more slowly.
                weight *= 0.5;
            }
            weightedTotal += observation * weight;
            totalWeight += weight;
            observations++;
        }

        if (observations < MINIMUM_OBSERVATIONS || totalWeight <= 0.0) {
            return Result.insufficient(observations);
        }

        double average = clamp(weightedTotal / totalWeight, -1.0, 1.0);
        double confidence = Math.min(1.0,
                totalWeight / FULL_CONFIDENCE_OBSERVATIONS);
        int adjustment = (int) Math.round(
                average * MAXIMUM_ADJUSTMENT * confidence
        );
        int successScore = (int) Math.round((average + 1.0) * 50.0);
        return new Result(true, observations, successScore, adjustment);
    }

    public static List<FertilizerApplication> matchingOutcomes(
            String zoneId,
            String productId,
            String productName,
            List<FertilizerApplication> history,
            long now
    ) {
        List<FertilizerApplication> result = new ArrayList<>();
        if (history == null || history.isEmpty()) return result;
        for (FertilizerApplication application : history) {
            if (eligible(zoneId, productId, productName, application, now)
                    && outcomeScore(application) != null) {
                result.add(application);
            }
        }
        result.sort(Comparator.comparingLong(
                FertilizerPerformanceAdvisor::resultTimestamp
        ).reversed());
        return result;
    }
    private static boolean eligible(
            GardenZone zone,
            FertilizerProduct product,
            FertilizerApplication application,
            long now
    ) {
        return eligible(
                safe(zone.getZone_id()), safe(product.getProduct_id()),
                safe(product.getName()), application, now
        );
    }

    private static boolean eligible(
            String zoneId,
            String productId,
            String productName,
            FertilizerApplication application,
            long now
    ) {
        if (application == null
                || !safe(zoneId).equals(application.getZone_id())
                || !sameProduct(productId, productName, application)) {
            return false;
        }
        long reference = resultTimestamp(application);
        return reference > 0L && reference <= now
                && now - reference <= MAXIMUM_AGE_DAYS * DAY_SECONDS;
    }

    private static boolean sameProduct(
            String productId,
            String productName,
            FertilizerApplication application
    ) {
        productId = safe(productId);
        productName = safe(productName);
        String applicationProductId = safe(application.getProduct_id());
        if (!productId.isBlank() && !applicationProductId.isBlank()) {
            return productId.equals(applicationProductId);
        }
        return !productName.isBlank()
                && productName.equalsIgnoreCase(
                safe(application.getProduct_name())
        );
    }

    static Double outcomeScore(FertilizerApplication value) {
        String status = safe(value.getOutcome_status()).toUpperCase(Locale.ROOT);
        boolean hasStatus = "IMPROVED".equals(status)
                || "UNCHANGED".equals(status)
                || "ISSUE".equals(status);
        int vigor = value.getOutcome_vigor_score();
        boolean hasVigor = vigor >= 1 && vigor <= 5;
        if (!hasStatus && !hasVigor) return null;

        double statusScore = "IMPROVED".equals(status)
                ? 1.0 : "ISSUE".equals(status) ? -1.0 : 0.0;
        double vigorScore = hasVigor ? (vigor - 3.0) / 2.0 : 0.0;
        if (hasStatus && hasVigor) {
            return statusScore * 0.65 + vigorScore * 0.35;
        }
        return hasStatus ? statusScore : vigorScore;
    }

    private static long resultTimestamp(FertilizerApplication application) {
        return application.getOutcome_observed_at_epoch() > 0L
                ? application.getOutcome_observed_at_epoch()
                : application.getApplied_at_epoch();
    }
    private static double recencyWeight(
            FertilizerApplication application,
            long now
    ) {
        long reference = application.getOutcome_observed_at_epoch() > 0L
                ? application.getOutcome_observed_at_epoch()
                : application.getApplied_at_epoch();
        long ageDays = Math.max(0L, (now - reference) / DAY_SECONDS);
        if (ageDays <= 180L) return 1.0;
        if (ageDays <= 365L) return 0.75;
        return 0.5;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        private final boolean hasEvidence;
        private final int observations;
        private final int successScore;
        private final int rankingAdjustment;

        private Result(
                boolean hasEvidence,
                int observations,
                int successScore,
                int rankingAdjustment
        ) {
            this.hasEvidence = hasEvidence;
            this.observations = observations;
            this.successScore = successScore;
            this.rankingAdjustment = rankingAdjustment;
        }

        private static Result insufficient(int observations) {
            return new Result(false, observations, 0, 0);
        }

        public boolean hasEvidence() { return hasEvidence; }
        public int getObservations() { return observations; }
        public int getSuccessScore() { return successScore; }
        public int getRankingAdjustment() { return rankingAdjustment; }
        public int getMinimumObservations() { return MINIMUM_OBSERVATIONS; }

        public String summary() {
            if (!hasEvidence) return "";
            String label = successScore >= 70
                    ? "olumlu" : successScore < 40 ? "dikkat" : "nötr";
            return "AVORA bölge deneyimi: " + observations + " sonuç · "
                    + successScore + "/100 · " + label;
        }
    }
}
