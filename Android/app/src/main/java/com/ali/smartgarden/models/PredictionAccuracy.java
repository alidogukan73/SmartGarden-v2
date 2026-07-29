package com.ali.smartgarden.models;

public class PredictionAccuracy {

    private long prediction_count;

    private long successful_predictions;

    private double average_error;

    private double maximum_error;

    private double minimum_error;

    private double accuracy_percent;

    private double confidence_multiplier;

    private String status;

    private String generated_at;


    public PredictionAccuracy() {
        /*
         * Firebase Realtime Database requires
         * an empty public constructor.
         */
    }


    public long getPrediction_count() {
        return prediction_count;
    }

    public void setPrediction_count(
            long prediction_count
    ) {
        this.prediction_count = prediction_count;
    }


    public long getSuccessful_predictions() {
        return successful_predictions;
    }

    public void setSuccessful_predictions(
            long successful_predictions
    ) {
        this.successful_predictions =
                successful_predictions;
    }


    public double getAverage_error() {
        return average_error;
    }

    public void setAverage_error(
            double average_error
    ) {
        this.average_error = average_error;
    }


    public double getMaximum_error() {
        return maximum_error;
    }

    public void setMaximum_error(
            double maximum_error
    ) {
        this.maximum_error = maximum_error;
    }


    public double getMinimum_error() {
        return minimum_error;
    }

    public void setMinimum_error(
            double minimum_error
    ) {
        this.minimum_error = minimum_error;
    }


    public double getAccuracy_percent() {
        return accuracy_percent;
    }

    public void setAccuracy_percent(
            double accuracy_percent
    ) {
        this.accuracy_percent = accuracy_percent;
    }


    public double getConfidence_multiplier() {
        return confidence_multiplier;
    }

    public void setConfidence_multiplier(
            double confidence_multiplier
    ) {
        this.confidence_multiplier =
                confidence_multiplier;
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