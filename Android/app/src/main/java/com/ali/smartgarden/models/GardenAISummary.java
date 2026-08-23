package com.ali.smartgarden.models;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class GardenAISummary {

    private long totalZones;
    private long analyzedZones;
    private long readyPredictions;
    private long wateringRecommended;
    private long warnings;
    private double averageConfidence;
    private String confidenceLevel;
    private String status;
    private String updatedAt;

    public GardenAISummary() {
    }

    @PropertyName("total_zones")
    public long getTotalZones() {
        return totalZones;
    }

    @PropertyName("total_zones")
    public void setTotalZones(long value) {
        totalZones = value;
    }

    @PropertyName("analyzed_zones")
    public long getAnalyzedZones() {
        return analyzedZones;
    }

    @PropertyName("analyzed_zones")
    public void setAnalyzedZones(long value) {
        analyzedZones = value;
    }

    @PropertyName("ready_predictions")
    public long getReadyPredictions() {
        return readyPredictions;
    }

    @PropertyName("ready_predictions")
    public void setReadyPredictions(long value) {
        readyPredictions = value;
    }

    @PropertyName("watering_recommended")
    public long getWateringRecommended() {
        return wateringRecommended;
    }

    @PropertyName("watering_recommended")
    public void setWateringRecommended(long value) {
        wateringRecommended = value;
    }

    public long getWarnings() {
        return warnings;
    }

    public void setWarnings(long warnings) {
        this.warnings = warnings;
    }

    @PropertyName("average_confidence")
    public double getAverageConfidence() {
        return averageConfidence;
    }

    @PropertyName("average_confidence")
    public void setAverageConfidence(double value) {
        averageConfidence = value;
    }

    @PropertyName("confidence_level")
    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    @PropertyName("confidence_level")
    public void setConfidenceLevel(String value) {
        confidenceLevel = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(String value) {
        updatedAt = value;
    }
}
