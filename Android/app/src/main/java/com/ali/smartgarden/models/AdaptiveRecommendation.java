package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

public class AdaptiveRecommendation {

    private String recommendationType;

    private String reason;

    private boolean shouldApply;

    private double confidence;

    private String confidenceLevel;

    private long currentPumpDurationSeconds;

    private long recommendedPumpDurationSeconds;

    private long currentCooldownSeconds;

    private long recommendedCooldownSeconds;

    private long wateringCountAnalyzed;

    private double averageMoistureDelta;

    private double averageWateringDurationSeconds;

    private String updatedAt;


    public AdaptiveRecommendation() {
        // Firebase Realtime Database için boş constructor zorunlu.
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


    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason
    ) {
        this.reason = reason;
    }


    @PropertyName("should_apply")
    public boolean isShouldApply() {
        return shouldApply;
    }

    @PropertyName("should_apply")
    public void setShouldApply(
            boolean shouldApply
    ) {
        this.shouldApply = shouldApply;
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
        this.confidenceLevel =
                confidenceLevel;
    }


    @PropertyName("current_pump_duration_seconds")
    public long getCurrentPumpDurationSeconds() {
        return currentPumpDurationSeconds;
    }

    @PropertyName("current_pump_duration_seconds")
    public void setCurrentPumpDurationSeconds(
            long currentPumpDurationSeconds
    ) {
        this.currentPumpDurationSeconds =
                currentPumpDurationSeconds;
    }


    @PropertyName("recommended_pump_duration_seconds")
    public long getRecommendedPumpDurationSeconds() {
        return recommendedPumpDurationSeconds;
    }

    @PropertyName("recommended_pump_duration_seconds")
    public void setRecommendedPumpDurationSeconds(
            long recommendedPumpDurationSeconds
    ) {
        this.recommendedPumpDurationSeconds =
                recommendedPumpDurationSeconds;
    }


    @PropertyName("current_cooldown_seconds")
    public long getCurrentCooldownSeconds() {
        return currentCooldownSeconds;
    }

    @PropertyName("current_cooldown_seconds")
    public void setCurrentCooldownSeconds(
            long currentCooldownSeconds
    ) {
        this.currentCooldownSeconds =
                currentCooldownSeconds;
    }


    @PropertyName("recommended_cooldown_seconds")
    public long getRecommendedCooldownSeconds() {
        return recommendedCooldownSeconds;
    }

    @PropertyName("recommended_cooldown_seconds")
    public void setRecommendedCooldownSeconds(
            long recommendedCooldownSeconds
    ) {
        this.recommendedCooldownSeconds =
                recommendedCooldownSeconds;
    }


    @PropertyName("watering_count_analyzed")
    public long getWateringCountAnalyzed() {
        return wateringCountAnalyzed;
    }

    @PropertyName("watering_count_analyzed")
    public void setWateringCountAnalyzed(
            long wateringCountAnalyzed
    ) {
        this.wateringCountAnalyzed =
                wateringCountAnalyzed;
    }


    @PropertyName("average_moisture_delta")
    public double getAverageMoistureDelta() {
        return averageMoistureDelta;
    }

    @PropertyName("average_moisture_delta")
    public void setAverageMoistureDelta(
            double averageMoistureDelta
    ) {
        this.averageMoistureDelta =
                averageMoistureDelta;
    }


    @PropertyName("average_watering_duration_seconds")
    public double getAverageWateringDurationSeconds() {
        return averageWateringDurationSeconds;
    }

    @PropertyName("average_watering_duration_seconds")
    public void setAverageWateringDurationSeconds(
            double averageWateringDurationSeconds
    ) {
        this.averageWateringDurationSeconds =
                averageWateringDurationSeconds;
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
