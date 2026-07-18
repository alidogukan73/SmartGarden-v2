package com.ali.smartgarden.models;

import com.google.firebase.database.PropertyName;

import java.util.ArrayList;
import java.util.List;

public class AIExplanation {

    private String explanationCode;

    private String title;

    private String summary;

    private List<String> reasonLines;

    private String nextStep;

    private long progressPercent;

    private String severity;

    private String generatedAt;

    private String updatedAt;

    public AIExplanation() {

        reasonLines = new ArrayList<>();
    }

    @PropertyName("explanation_code")
    public String getExplanationCode() {
        return explanationCode;
    }

    @PropertyName("explanation_code")
    public void setExplanationCode(
            String explanationCode
    ) {
        this.explanationCode =
                explanationCode;
    }

    public String getTitle() {
        return title;
    }
    @PropertyName("title")
    public void setTitle(
            String title
    ) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(
            String summary
    ) {
        this.summary = summary;
    }

    @PropertyName("reason_lines")
    public List<String> getReasonLines() {

        if (reasonLines == null) {
            reasonLines = new ArrayList<>();
        }

        return reasonLines;
    }

    @PropertyName("reason_lines")
    public void setReasonLines(
            List<String> reasonLines
    ) {

        this.reasonLines =
                reasonLines != null
                        ? reasonLines
                        : new ArrayList<>();
    }

    @PropertyName("next_step")
    public String getNextStep() {
        return nextStep;
    }

    @PropertyName("next_step")
    public void setNextStep(
            String nextStep
    ) {
        this.nextStep = nextStep;
    }

    @PropertyName("progress_percent")
    public long getProgressPercent() {
        return progressPercent;
    }

    @PropertyName("progress_percent")
    public void setProgressPercent(
            long progressPercent
    ) {
        this.progressPercent =
                progressPercent;
    }

    @PropertyName("severity")
    public String getSeverity() {
        return severity;
    }

    @PropertyName("severity")
    public void setSeverity(String severity) {
        this.severity = severity;
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