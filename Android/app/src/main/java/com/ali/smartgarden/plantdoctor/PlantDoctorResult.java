package com.ali.smartgarden.plantdoctor;

public class PlantDoctorResult {
    private final String title, probability, urgency, context, advice;
    public PlantDoctorResult(String title, String probability, String urgency,
                             String context, String advice) {
        this.title = title; this.probability = probability; this.urgency = urgency;
        this.context = context; this.advice = advice;
    }
    public String getTitle() { return title; }
    public String getProbability() { return probability; }
    public String getUrgency() { return urgency; }
    public String getContext() { return context; }
    public String getAdvice() { return advice; }
}
