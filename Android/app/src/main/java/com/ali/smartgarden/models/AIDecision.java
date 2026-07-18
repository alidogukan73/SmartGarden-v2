package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

public class AIDecision {

    private String decisionCode;
    private String decisionTitle;
    private String decisionMessage;

    private String severity;

    private double confidence;
    private String confidenceLevel;

    private boolean shouldWater;

    private String recommendationType;
    private String soilClassification;
    private String trendClassification;

    private String primaryReason;
    private String secondaryReason;

    private String generatedAt;
    private String updatedAt;


    public AIDecision() {
        // Firebase Realtime Database için boş constructor zorunlu.
    }

    @PropertyName("decision_code")
    public String getDecisionCode() {
        return decisionCode;
    }

    @PropertyName("decision_code")
    public void setDecisionCode(
            String decisionCode
    ) {
        this.decisionCode = decisionCode;
    }

    @PropertyName("decision_title")
    public String getDecisionTitle() {
        return decisionTitle;
    }

    @PropertyName("decision_title")
    public void setDecisionTitle(
            String decisionTitle
    ) {
        this.decisionTitle = decisionTitle;
    }

    @PropertyName("decision_message")
    public String getDecisionMessage() {
        return decisionMessage;
    }

    @PropertyName("decision_message")
    public void setDecisionMessage(
            String decisionMessage
    ) {
        this.decisionMessage = decisionMessage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(
            String severity
    ) {
        this.severity = severity;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(
            double confidence
    ) {
        this.confidence = confidence;
    }

    @PropertyName("confidence_level")
    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    @PropertyName("confidence_level")
    public void setConfidenceLevel(
            String confidenceLevel
    ) {
        this.confidenceLevel = confidenceLevel;
    }

    @PropertyName("should_water")
    public boolean isShouldWater() {
        return shouldWater;
    }

    @PropertyName("should_water")
    public void setShouldWater(
            boolean shouldWater
    ) {
        this.shouldWater = shouldWater;
    }

    @PropertyName("recommendation_type")
    public String getRecommendationType() {
        return recommendationType;
    }

    @PropertyName("recommendation_type")
    public void setRecommendationType(
            String recommendationType
    ) {
        this.recommendationType =
                recommendationType;
    }

    @PropertyName("soil_classification")
    public String getSoilClassification() {
        return soilClassification;
    }

    @PropertyName("soil_classification")
    public void setSoilClassification(
            String soilClassification
    ) {
        this.soilClassification =
                soilClassification;
    }

    @PropertyName("trend_classification")
    public String getTrendClassification() {
        return trendClassification;
    }

    @PropertyName("trend_classification")
    public void setTrendClassification(
            String trendClassification
    ) {
        this.trendClassification =
                trendClassification;
    }

    @PropertyName("primary_reason")
    public String getPrimaryReason() {
        return primaryReason;
    }

    @PropertyName("primary_reason")
    public void setPrimaryReason(
            String primaryReason
    ) {
        this.primaryReason = primaryReason;
    }

    @PropertyName("secondary_reason")
    public String getSecondaryReason() {
        return secondaryReason;
    }

    @PropertyName("secondary_reason")
    public void setSecondaryReason(
            String secondaryReason
    ) {
        this.secondaryReason = secondaryReason;
    }

    @PropertyName("generated_at")
    public String getGeneratedAt() {
        return generatedAt;
    }

    @PropertyName("generated_at")
    public void setGeneratedAt(
            String generatedAt
    ) {
        this.generatedAt = generatedAt;
    }

    @PropertyName("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(
            String updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}