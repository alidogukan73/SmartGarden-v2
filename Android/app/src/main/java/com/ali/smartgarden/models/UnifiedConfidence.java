package com.ali.smartgarden.models;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UnifiedConfidence {

    private double overall_confidence;

    private String confidence_level;

    private double soil_learning_confidence;

    private double prediction_accuracy;

    private double sensor_confidence;

    private double trend_confidence;

    private double weighted_score;

    private String status;

    private String generated_at;


    public UnifiedConfidence() {
        /*
         * Firebase Realtime Database requires
         * an empty public constructor.
         */
    }


    public double getOverall_confidence() {
        return overall_confidence;
    }

    public void setOverall_confidence(
            double overall_confidence
    ) {
        this.overall_confidence =
                overall_confidence;
    }


    public String getConfidence_level() {
        return confidence_level;
    }

    public void setConfidence_level(
            String confidence_level
    ) {
        this.confidence_level =
                confidence_level;
    }


    public double getSoil_learning_confidence() {
        return soil_learning_confidence;
    }

    public void setSoil_learning_confidence(
            double soil_learning_confidence
    ) {
        this.soil_learning_confidence =
                soil_learning_confidence;
    }


    public double getPrediction_accuracy() {
        return prediction_accuracy;
    }

    public void setPrediction_accuracy(
            double prediction_accuracy
    ) {
        this.prediction_accuracy =
                prediction_accuracy;
    }


    public double getSensor_confidence() {
        return sensor_confidence;
    }

    public void setSensor_confidence(
            double sensor_confidence
    ) {
        this.sensor_confidence =
                sensor_confidence;
    }


    public double getTrend_confidence() {
        return trend_confidence;
    }

    public void setTrend_confidence(
            double trend_confidence
    ) {
        this.trend_confidence =
                trend_confidence;
    }


    public double getWeighted_score() {
        return weighted_score;
    }

    public void setWeighted_score(
            double weighted_score
    ) {
        this.weighted_score =
                weighted_score;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }


    public String getGenerated_at() {
        return generated_at;
    }

    public void setGenerated_at(
            String generated_at
    ) {
        this.generated_at = generated_at;
    }
}
