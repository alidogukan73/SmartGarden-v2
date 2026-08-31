package com.alidogukan.avora.models;

import com.google.firebase.database.IgnoreExtraProperties;

/** Bölge için backend tarafından hesaplanan canlı sulama zamanı kararı. */
@IgnoreExtraProperties
public class IrrigationTimingPlan {

    private String status;
    private boolean postpone;
    private long recommended_at_epoch;
    private long window_start_epoch;
    private long window_end_epoch;
    private String reason;
    private String detail;
    private int score;
    private boolean emergency;
    private boolean weather_based;
    private boolean recheck_before_watering;

    public IrrigationTimingPlan() {
        // Required by Firebase.
    }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public boolean isPostpone() { return postpone; }
    public void setPostpone(boolean value) { postpone = value; }
    public long getRecommended_at_epoch() { return recommended_at_epoch; }
    public void setRecommended_at_epoch(long value) { recommended_at_epoch = Math.max(0, value); }
    public long getWindow_start_epoch() { return window_start_epoch; }
    public void setWindow_start_epoch(long value) { window_start_epoch = Math.max(0, value); }
    public long getWindow_end_epoch() { return window_end_epoch; }
    public void setWindow_end_epoch(long value) { window_end_epoch = Math.max(0, value); }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public String getDetail() { return detail; }
    public void setDetail(String value) { detail = value; }
    public int getScore() { return score; }
    public void setScore(int value) { score = Math.max(0, Math.min(100, value)); }
    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean value) { emergency = value; }
    public boolean isWeather_based() { return weather_based; }
    public void setWeather_based(boolean value) { weather_based = value; }
    public boolean isRecheck_before_watering() { return recheck_before_watering; }
    public void setRecheck_before_watering(boolean value) { recheck_before_watering = value; }
}
