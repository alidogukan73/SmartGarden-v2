package com.ali.smartgarden.models;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class ZoneAIState {

    private String zoneId;
    private String sensorId;
    private String updatedAt;
    private AIDecision decision;
    private AIExplanation explanation;
    private MoisturePrediction moisturePrediction;
    private PredictionAccuracy predictionAccuracy;
    private UnifiedConfidence confidence;
    private SoilLearningProfile learningProfile;
    private AdaptiveRecommendation adaptiveRecommendation;
    private PredictionValidationStatus predictionValidation;

    public ZoneAIState() {
    }

    @PropertyName("zone_id")
    public String getZoneId() {
        return zoneId;
    }

    @PropertyName("zone_id")
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    @PropertyName("sensor_id")
    public String getSensorId() {
        return sensorId;
    }

    @PropertyName("sensor_id")
    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    @PropertyName("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AIDecision getDecision() {
        return decision;
    }

    public void setDecision(AIDecision decision) {
        this.decision = decision;
    }

    public AIExplanation getExplanation() {
        return explanation;
    }

    public void setExplanation(AIExplanation explanation) {
        this.explanation = explanation;
    }

    @PropertyName("moisture_prediction")
    public MoisturePrediction getMoisturePrediction() {
        return moisturePrediction;
    }

    @PropertyName("moisture_prediction")
    public void setMoisturePrediction(MoisturePrediction value) {
        moisturePrediction = value;
    }

    @PropertyName("prediction_accuracy")
    public PredictionAccuracy getPredictionAccuracy() {
        return predictionAccuracy;
    }

    @PropertyName("prediction_accuracy")
    public void setPredictionAccuracy(PredictionAccuracy value) {
        predictionAccuracy = value;
    }

    public UnifiedConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(UnifiedConfidence confidence) {
        this.confidence = confidence;
    }

    @PropertyName("learning_profile")
    public SoilLearningProfile getLearningProfile() {
        return learningProfile;
    }

    @PropertyName("learning_profile")
    public void setLearningProfile(SoilLearningProfile value) {
        learningProfile = value;
    }

    @PropertyName("adaptive_recommendation")
    public AdaptiveRecommendation getAdaptiveRecommendation() {
        return adaptiveRecommendation;
    }

    @PropertyName("adaptive_recommendation")
    public void setAdaptiveRecommendation(AdaptiveRecommendation value) {
        adaptiveRecommendation = value;
    }

    @PropertyName("prediction_validation")
    public PredictionValidationStatus getPredictionValidation() {
        return predictionValidation;
    }

    @PropertyName("prediction_validation")
    public void setPredictionValidation(PredictionValidationStatus value) {
        predictionValidation = value;
    }
}
