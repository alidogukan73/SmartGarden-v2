package com.ali.smartgarden.fertilization;

import java.util.List;

public class FertilizerAdvice {
    private final String zoneTitle, status, reason, context;
    private final List<String> candidates;
    private final List<String> risks;
    private final Experience experience;

    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks) {
        this(zoneTitle, status, reason, context, candidates, risks,
                Experience.none());
    }

    public FertilizerAdvice(String zoneTitle, String status, String reason,
                            String context, List<String> candidates,
                            List<String> risks, Experience experience) {
        this.zoneTitle = zoneTitle;
        this.status = status;
        this.reason = reason;
        this.context = context;
        this.candidates = candidates;
        this.risks = risks;
        this.experience = experience == null ? Experience.none() : experience;
    }

    public String getZoneTitle() { return zoneTitle; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getContext() { return context; }
    public List<String> getCandidates() { return candidates; }
    public List<String> getRisks() { return risks; }
    public Experience getExperience() { return experience; }

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