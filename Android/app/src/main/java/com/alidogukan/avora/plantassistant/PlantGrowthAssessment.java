package com.alidogukan.avora.plantassistant;

/** Immutable visible-growth assessment stored with a private garden photo. */
public final class PlantGrowthAssessment {
    private final int score;
    private final int confidence;
    private final String stage;
    private final String trend;
    private final int scoreDelta;
    private final String signals;
    private final long previousCapturedAtEpoch;

    public PlantGrowthAssessment(int score, int confidence, String stage, String trend,
                                 int scoreDelta, String signals,
                                 long previousCapturedAtEpoch) {
        this.score = score;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.stage = safe(stage);
        this.trend = safe(trend);
        this.scoreDelta = scoreDelta;
        this.signals = safe(signals);
        this.previousCapturedAtEpoch = Math.max(0L, previousCapturedAtEpoch);
    }

    public boolean isTracked() { return score >= 0 && score <= 100; }
    public int getScore() { return score; }
    public int getConfidence() { return confidence; }
    public String getStage() { return stage; }
    public String getTrend() { return trend; }
    public int getScoreDelta() { return scoreDelta; }
    public String getSignals() { return signals; }
    public long getPreviousCapturedAtEpoch() { return previousCapturedAtEpoch; }
    public boolean isFirstRecord() { return isFirstRecord(trend); }
    public boolean isImproving() { return isImproving(trend); }
    public boolean isStable() { return isStable(trend); }
    public boolean isDeclining() { return isDeclining(trend); }

    public static boolean isFirstRecord(String value) {
        return PlantGrowthTrendPolicy.FIRST_RECORD.equals(value);
    }
    public static boolean isImproving(String value) {
        return PlantGrowthTrendPolicy.IMPROVING.equals(value);
    }
    public static boolean isStable(String value) {
        return PlantGrowthTrendPolicy.STABLE.equals(value);
    }
    public static boolean isDeclining(String value) {
        return PlantGrowthTrendPolicy.DECLINING.equals(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
