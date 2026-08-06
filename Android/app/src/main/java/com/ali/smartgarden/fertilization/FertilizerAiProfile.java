package com.ali.smartgarden.fertilization;

import java.util.List;

/**
 * Explainable agricultural guidance attached to one fertilizer product.
 * It supports decisions but never replaces the product label or analysis.
 */
public class FertilizerAiProfile {

    private final String role;
    private final List<String> suitableStages;
    private final String suitability;
    private final String reason;
    private final String fruitStageAdvice;
    private final String safetyNote;

    public FertilizerAiProfile(
            String role,
            List<String> suitableStages,
            String suitability,
            String reason,
            String fruitStageAdvice,
            String safetyNote
    ) {
        this.role = role;
        this.suitableStages = suitableStages;
        this.suitability = suitability;
        this.reason = reason;
        this.fruitStageAdvice = fruitStageAdvice;
        this.safetyNote = safetyNote;
    }

    public String getRole() { return role; }
    public List<String> getSuitableStages() { return suitableStages; }
    public String getSuitability() { return suitability; }
    public String getReason() { return reason; }
    public String getFruitStageAdvice() { return fruitStageAdvice; }
    public String getSafetyNote() { return safetyNote; }
}
