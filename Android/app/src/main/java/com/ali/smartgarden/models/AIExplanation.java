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
    private AIDecisionFlow decisionFlow;

    public AIExplanation() {

        reasonLines = new ArrayList<>();
        decisionFlow = new AIDecisionFlow();
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

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    public void setTitle(
            String title
    ) {
        this.title = title;
    }

    @PropertyName("summary")
    public String getSummary() {
        return summary;
    }

    @PropertyName("summary")
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
    public void setSeverity(
            String severity
    ) {
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

    @PropertyName("decision_flow")
    public AIDecisionFlow getDecisionFlow() {


        if (decisionFlow == null) {
            decisionFlow = new AIDecisionFlow();
        }

        return decisionFlow;
    }

    @PropertyName("decision_flow")
    public void setDecisionFlow(
            AIDecisionFlow decisionFlow
    ) {

        this.decisionFlow =
                decisionFlow != null
                        ? decisionFlow
                        : new AIDecisionFlow();
    }

    public static class AIDecisionFlow {

        private String sensor;
        private String moisture;
        private String soil;
        private String history;
        private String result;

        private String sensorStatus;
        private String moistureStatus;
        private String soilStatus;
        private String historyStatus;
        private String resultStatus;

        public AIDecisionFlow() {
        }

        @PropertyName("sensor")
        public String getSensor() {
            return sensor;
        }

        @PropertyName("sensor")
        public void setSensor(
                String sensor
        ) {
            this.sensor = sensor;
        }

        @PropertyName("moisture")
        public String getMoisture() {
            return moisture;
        }

        @PropertyName("moisture")
        public void setMoisture(
                String moisture
        ) {
            this.moisture = moisture;
        }

        @PropertyName("soil")
        public String getSoil() {
            return soil;
        }

        @PropertyName("soil")
        public void setSoil(
                String soil
        ) {
            this.soil = soil;
        }

        @PropertyName("history")
        public String getHistory() {
            return history;
        }

        @PropertyName("history")
        public void setHistory(
                String history
        ) {
            this.history = history;
        }

        @PropertyName("result")
        public String getResult() {
            return result;
        }

        @PropertyName("result")
        public void setResult(
                String result
        ) {
            this.result = result;
        }

        @PropertyName("sensor_status")
        public String getSensorStatus() {
            return sensorStatus;
        }

        @PropertyName("sensor_status")
        public void setSensorStatus(
                String sensorStatus
        ) {
            this.sensorStatus = sensorStatus;
        }

        @PropertyName("moisture_status")
        public String getMoistureStatus() {
            return moistureStatus;
        }

        @PropertyName("moisture_status")
        public void setMoistureStatus(
                String moistureStatus
        ) {
            this.moistureStatus = moistureStatus;
        }

        @PropertyName("soil_status")
        public String getSoilStatus() {
            return soilStatus;
        }

        @PropertyName("soil_status")
        public void setSoilStatus(
                String soilStatus
        ) {
            this.soilStatus = soilStatus;
        }

        @PropertyName("history_status")
        public String getHistoryStatus() {
            return historyStatus;
        }

        @PropertyName("history_status")
        public void setHistoryStatus(
                String historyStatus
        ) {
            this.historyStatus = historyStatus;
        }

        @PropertyName("result_status")
        public String getResultStatus() {
            return resultStatus;
        }

        @PropertyName("result_status")
        public void setResultStatus(
                String resultStatus
        ) {
            this.resultStatus = resultStatus;
        }
    }
}