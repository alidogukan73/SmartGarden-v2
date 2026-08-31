package com.alidogukan.avora.fertilization;

/** A conservative tank-mix assessment. It never replaces the product label. */
public final class FertilizerMixResult {

    public enum RiskLevel {
        BLOCKED,
        CAUTION,
        UNVERIFIED
    }

    private final String title;
    private final String message;
    private final RiskLevel riskLevel;

    public FertilizerMixResult(
            String title,
            String message,
            RiskLevel riskLevel
    ) {
        this.title = title;
        this.message = message;
        this.riskLevel = riskLevel;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isBlocked() { return riskLevel == RiskLevel.BLOCKED; }
    public boolean requiresConfirmation() { return !isBlocked(); }
    public boolean isCaution() { return riskLevel != null; }
}