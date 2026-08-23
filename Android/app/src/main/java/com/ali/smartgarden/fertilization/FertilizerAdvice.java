package com.ali.smartgarden.fertilization;

import java.util.List;

public class FertilizerAdvice {
    private final String zoneTitle, status, reason, context;
    private final List<String> candidates;
    private final List<String> risks;
    private final Experience experience;
    private final Recommendation recommendation;

    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks) {
        this(zoneTitle, status, reason, context, candidates, risks,
                Experience.none(), Recommendation.none());
    }

    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks, Experience experience) {
        this(zoneTitle, status, reason, context, candidates, risks,
                experience, Recommendation.none());
    }

    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks, Experience experience,
                            Recommendation recommendation) {
        this.zoneTitle = zoneTitle;
        this.status = status;
        this.reason = reason;
        this.context = context;
        this.candidates = candidates;
        this.risks = risks;
        this.experience = experience == null ? Experience.none() : experience;
        this.recommendation = recommendation == null
                ? Recommendation.none() : recommendation;
    }

    public String getZoneTitle() { return zoneTitle; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getContext() { return context; }
    public List<String> getCandidates() { return candidates; }
    public List<String> getRisks() { return risks; }
    public Experience getExperience() { return experience; }
    public Recommendation getRecommendation() { return recommendation; }

    /** The single next need selected by the advisor after all safety gates. */
    public static final class Recommendation {
        private final String productId;
        private final String productName;
        private final String applicationType;
        private final String need;
        private final long waitDays;
        private final boolean applicationReady;

        public Recommendation(String productId, String productName,
                              String applicationType, String need,
                              long waitDays, boolean applicationReady) {
            this.productId = productId == null ? "" : productId;
            this.productName = productName == null ? "" : productName;
            this.applicationType = applicationType == null ? "" : applicationType;
            this.need = need == null ? "" : need;
            this.waitDays = Math.max(0L, waitDays);
            this.applicationReady = applicationReady;
        }

        public static Recommendation none() {
            return new Recommendation("", "", "", "", 0L, false);
        }

        public boolean isAvailable() { return !productName.isBlank(); }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getApplicationType() { return applicationType; }
        public String getNeed() { return need; }
        public long getWaitDays() { return waitDays; }
        public boolean isApplicationReady() { return applicationReady; }
    }

    /** Structured, UI-neutral summary of outcomes for the top recommendation. */
    public static final class Experience {
        private final String productId;
        private final String productName;
        private final int observations;
        private final int requiredObservations;
        private final boolean reliable;
        private final int successScore;

        public Experience(String productName, int observations,
                          int requiredObservations, boolean reliable,
                          int successScore) {
            this("", productName, observations, requiredObservations,
                    reliable, successScore);
        }

        public Experience(String productId, String productName,
                          int observations, int requiredObservations,
                          boolean reliable, int successScore) {
            this.productId = productId == null ? "" : productId;
            this.productName = productName == null ? "" : productName;
            this.observations = Math.max(0, observations);
            this.requiredObservations = Math.max(1, requiredObservations);
            this.reliable = reliable;
            this.successScore = Math.max(0, Math.min(100, successScore));
        }

        public static Experience none() {
            return new Experience("", "", 0, 2, false, 0);
        }

        public boolean isAvailable() { return !productName.isBlank(); }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getObservations() { return observations; }
        public int getRequiredObservations() { return requiredObservations; }
        public boolean isReliable() { return reliable; }
        public int getSuccessScore() { return successScore; }
    }
}