package com.alidogukan.avora.fertilization;

import com.alidogukan.avora.models.FertilizerApplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Learns a repeated dose and method pattern without turning a mixed
 * application or a single observation into an agronomic recommendation.
 */
public final class FertilizerExperiencePatternAdvisor {

    private static final int MINIMUM_PATTERN_OBSERVATIONS = 2;
    private static final int COMPARATIVE_GAP = 10;

    private FertilizerExperiencePatternAdvisor() { }

    public static Result evaluate(
            String zoneId,
            String productId,
            String productName,
            List<FertilizerApplication> history,
            long now
    ) {
        List<FertilizerApplication> outcomes =
                FertilizerPerformanceAdvisor.matchingOutcomes(
                        zoneId, productId, productName, history, now
                );
        if (outcomes.isEmpty()) return Result.insufficient(0, 0);

        Map<String, Aggregate> groups = new LinkedHashMap<>();
        int excludedMixed = 0;
        for (FertilizerApplication application : outcomes) {
            if (!safe(application.getMix_group_id()).isBlank()) {
                excludedMixed++;
                continue;
            }
            Double score = FertilizerPerformanceAdvisor.outcomeScore(application);
            double dose = roundedDose(application.getApplied_dose());
            String unit = safe(application.getDose_unit());
            String method = safe(application.getApplication_method());
            if (score == null || dose <= 0.0 || unit.isBlank() || method.isBlank()) {
                continue;
            }

            String key = dose + "|" + unit.toLowerCase(Locale.ROOT)
                    + "|" + method.toLowerCase(Locale.ROOT);
            Aggregate aggregate = groups.get(key);
            if (aggregate == null) {
                aggregate = new Aggregate(dose, unit, method);
                groups.put(key, aggregate);
            }
            aggregate.add(score);
        }

        List<Pattern> supported = new ArrayList<>();
        for (Aggregate aggregate : groups.values()) {
            if (aggregate.observations >= MINIMUM_PATTERN_OBSERVATIONS) {
                supported.add(aggregate.toPattern());
            }
        }
        supported.sort(
                Comparator.comparingInt(Pattern::getSuccessScore).reversed()
                        .thenComparing(
                                Comparator.comparingInt(
                                        Pattern::getObservations
                                ).reversed()
                        )
        );
        if (supported.isEmpty()) {
            return Result.insufficient(outcomes.size(), excludedMixed);
        }

        Pattern best = supported.get(0);
        boolean comparative = supported.size() >= 2
                && best.getSuccessScore()
                - supported.get(1).getSuccessScore() >= COMPARATIVE_GAP;
        return new Result(
                true,
                comparative,
                best,
                supported.size(),
                outcomes.size(),
                excludedMixed
        );
    }

    private static double roundedDose(double dose) {
        return Math.round(dose * 10.0) / 10.0;
    }

    private static int successScore(double average) {
        double bounded = Math.max(-1.0, Math.min(1.0, average));
        return (int) Math.round((bounded + 1.0) * 50.0);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Aggregate {
        private final double dose;
        private final String unit;
        private final String method;
        private int observations;
        private double totalScore;

        private Aggregate(double dose, String unit, String method) {
            this.dose = dose;
            this.unit = unit;
            this.method = method;
        }

        private void add(double score) {
            observations++;
            totalScore += score;
        }

        private Pattern toPattern() {
            return new Pattern(
                    dose,
                    unit,
                    method,
                    observations,
                    successScore(totalScore / observations)
            );
        }
    }

    public static final class Pattern {
        private final double dose;
        private final String unit;
        private final String method;
        private final int observations;
        private final int successScore;

        private Pattern(
                double dose,
                String unit,
                String method,
                int observations,
                int successScore
        ) {
            this.dose = dose;
            this.unit = unit;
            this.method = method;
            this.observations = observations;
            this.successScore = successScore;
        }

        public double getDose() { return dose; }
        public String getUnit() { return unit; }
        public String getMethod() { return method; }
        public int getObservations() { return observations; }
        public int getSuccessScore() { return successScore; }
    }

    public static final class Result {
        private final boolean available;
        private final boolean comparative;
        private final Pattern bestPattern;
        private final int supportedPatternCount;
        private final int evaluatedCount;
        private final int excludedMixedCount;

        private Result(
                boolean available,
                boolean comparative,
                Pattern bestPattern,
                int supportedPatternCount,
                int evaluatedCount,
                int excludedMixedCount
        ) {
            this.available = available;
            this.comparative = comparative;
            this.bestPattern = bestPattern;
            this.supportedPatternCount = supportedPatternCount;
            this.evaluatedCount = evaluatedCount;
            this.excludedMixedCount = excludedMixedCount;
        }

        private static Result insufficient(
                int evaluatedCount,
                int excludedMixedCount
        ) {
            return new Result(
                    false, false, null, 0,
                    evaluatedCount, excludedMixedCount
            );
        }

        public boolean isAvailable() { return available; }
        public boolean isComparative() { return comparative; }
        public Pattern getBestPattern() { return bestPattern; }
        public int getSupportedPatternCount() { return supportedPatternCount; }
        public int getEvaluatedCount() { return evaluatedCount; }
        public int getExcludedMixedCount() { return excludedMixedCount; }
        public int getMinimumPatternObservations() {
            return MINIMUM_PATTERN_OBSERVATIONS;
        }
    }
}
