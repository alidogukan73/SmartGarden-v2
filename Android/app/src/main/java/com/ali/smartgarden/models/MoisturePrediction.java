package com.ali.smartgarden.models;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MoisturePrediction {

    private String prediction_status;

    private String prediction_method;

    private double current_moisture;

    private double moisture_limit;

    private double drying_rate_per_minute;

    private double predicted_moisture_1_hour;

    private double predicted_moisture_3_hours;

    private double predicted_moisture_6_hours;

    private double estimated_minutes_until_limit;

    private String estimated_limit_reached_at;

    private double confidence;

    private String confidence_level;

    private String generated_at;


    public MoisturePrediction() {
        /*
         * Firebase Realtime Database requires
         * an empty public constructor.
         */
    }


    public String getPrediction_status() {
        return prediction_status;
    }

    public void setPrediction_status(
            String prediction_status
    ) {
        this.prediction_status = prediction_status;
    }


    public String getPrediction_method() {
        return prediction_method;
    }

    public void setPrediction_method(
            String prediction_method
    ) {
        this.prediction_method = prediction_method;
    }


    public double getCurrent_moisture() {
        return current_moisture;
    }

    public void setCurrent_moisture(
            double current_moisture
    ) {
        this.current_moisture = current_moisture;
    }


    public double getMoisture_limit() {
        return moisture_limit;
    }

    public void setMoisture_limit(
            double moisture_limit
    ) {
        this.moisture_limit = moisture_limit;
    }


    public double getDrying_rate_per_minute() {
        return drying_rate_per_minute;
    }

    public void setDrying_rate_per_minute(
            double drying_rate_per_minute
    ) {
        this.drying_rate_per_minute =
                drying_rate_per_minute;
    }


    public double getPredicted_moisture_1_hour() {
        return predicted_moisture_1_hour;
    }

    public void setPredicted_moisture_1_hour(
            double predicted_moisture_1_hour
    ) {
        this.predicted_moisture_1_hour =
                predicted_moisture_1_hour;
    }


    public double getPredicted_moisture_3_hours() {
        return predicted_moisture_3_hours;
    }

    public void setPredicted_moisture_3_hours(
            double predicted_moisture_3_hours
    ) {
        this.predicted_moisture_3_hours =
                predicted_moisture_3_hours;
    }


    public double getPredicted_moisture_6_hours() {
        return predicted_moisture_6_hours;
    }

    public void setPredicted_moisture_6_hours(
            double predicted_moisture_6_hours
    ) {
        this.predicted_moisture_6_hours =
                predicted_moisture_6_hours;
    }


    public double getEstimated_minutes_until_limit() {
        return estimated_minutes_until_limit;
    }

    public void setEstimated_minutes_until_limit(
            double estimated_minutes_until_limit
    ) {
        this.estimated_minutes_until_limit =
                estimated_minutes_until_limit;
    }


    public String getEstimated_limit_reached_at() {
        return estimated_limit_reached_at;
    }

    public void setEstimated_limit_reached_at(
            String estimated_limit_reached_at
    ) {
        this.estimated_limit_reached_at =
                estimated_limit_reached_at;
    }


    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(
            double confidence
    ) {
        this.confidence = confidence;
    }


    public String getConfidence_level() {
        return confidence_level;
    }

    public void setConfidence_level(
            String confidence_level
    ) {
        this.confidence_level = confidence_level;
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
