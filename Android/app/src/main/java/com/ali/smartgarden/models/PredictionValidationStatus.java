package com.ali.smartgarden.models;

public class PredictionValidationStatus {

    private String validation_status;

    private long pending_count;

    private long target_minutes;

    private String next_validation_at;

    private long remaining_seconds;

    private String updated_at;


    public PredictionValidationStatus() {
        // Required for Firebase
    }


    public String getValidation_status() {
        return validation_status;
    }

    public void setValidation_status(
            String validation_status
    ) {
        this.validation_status = validation_status;
    }


    public long getPending_count() {
        return pending_count;
    }

    public void setPending_count(
            long pending_count
    ) {
        this.pending_count = pending_count;
    }


    public long getTarget_minutes() {
        return target_minutes;
    }

    public void setTarget_minutes(
            long target_minutes
    ) {
        this.target_minutes = target_minutes;
    }


    public String getNext_validation_at() {
        return next_validation_at;
    }

    public void setNext_validation_at(
            String next_validation_at
    ) {
        this.next_validation_at = next_validation_at;
    }


    public long getRemaining_seconds() {
        return remaining_seconds;
    }

    public void setRemaining_seconds(
            long remaining_seconds
    ) {
        this.remaining_seconds = remaining_seconds;
    }


    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(
            String updated_at
    ) {
        this.updated_at = updated_at;
    }
}
