package com.ali.smartgarden.models;


public class SoilLearningProfile {


    private String profile_status;
    private String soil_classification;

    private double confidence;

    private String confidence_level;

    private int learning_stage;

    private String next_milestone_code;
    private String next_milestone_text;


    private int remaining_sensor_samples;
    private int remaining_auto_waterings;

    private int sensor_history_count;
    private int watering_count_analyzed;


    private double average_moisture;

    private double average_drying_rate_per_minute;

    private double average_moisture_gain_per_watering;

    private double average_watering_duration_seconds;

    private double estimated_water_retention_minutes;

    private double irrigation_efficiency;


    private String learned_at;

    private String updated_at;



    /*
     * Firebase için boş constructor zorunlu.
     */
    public SoilLearningProfile() {

    }



    public String getProfile_status() {
        return profile_status;
    }


    public String getSoil_classification() {
        return soil_classification;
    }


    public double getConfidence() {
        return confidence;
    }


    public String getConfidence_level() {
        return confidence_level;
    }


    public int getLearning_stage() {
        return learning_stage;
    }


    public String getNext_milestone_code() {
        return next_milestone_code;
    }


    public String getNext_milestone_text() {
        return next_milestone_text;
    }


    public int getRemaining_sensor_samples() {
        return remaining_sensor_samples;
    }


    public int getRemaining_auto_waterings() {
        return remaining_auto_waterings;
    }


    public int getSensor_history_count() {
        return sensor_history_count;
    }


    public int getWatering_count_analyzed() {
        return watering_count_analyzed;
    }


    public double getAverage_moisture() {
        return average_moisture;
    }


    public double getAverage_drying_rate_per_minute() {
        return average_drying_rate_per_minute;
    }


    public double getAverage_moisture_gain_per_watering() {
        return average_moisture_gain_per_watering;
    }


    public double getAverage_watering_duration_seconds() {
        return average_watering_duration_seconds;
    }


    public double getEstimated_water_retention_minutes() {
        return estimated_water_retention_minutes;
    }


    public double getIrrigation_efficiency() {
        return irrigation_efficiency;
    }


    public String getLearned_at() {
        return learned_at;
    }


    public String getUpdated_at() {
        return updated_at;
    }

}
