package com.ali.smartgarden.ui.irrigationassistant;

import android.content.Context;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;

/** AI Sulama Asistanı genel biçimlendirme ve veri tazelik kuralları. */
public final class IrrigationAssistantFormatter {
    private static final long ZONE_FRESHNESS_SECONDS = 90L;
    private static final long WEATHER_FRESHNESS_SECONDS = 3L * 60L * 60L;

    private final Context context;

    public IrrigationAssistantFormatter(Context context) {
        this.context = context.getApplicationContext();
    }

    public String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public boolean isZoneFresh(GardenZone zone) {
        if (zone == null || zone.getUpdated_at_epoch() <= 0L) {
            return false;
        }
        long ageSeconds = Math.max(
                0L,
                System.currentTimeMillis() / 1000L - zone.getUpdated_at_epoch()
        );
        return ageSeconds <= ZONE_FRESHNESS_SECONDS;
    }

    public boolean isWeatherForecastFresh(WeatherForecast forecast) {
        if (forecast == null || forecast.getUpdatedAtEpoch() <= 0L) {
            return false;
        }
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long updatedAt = forecast.getUpdatedAtEpoch();
        if (updatedAt > 10_000_000_000L) {
            updatedAt /= 1000L;
        }
        long ageSeconds = nowSeconds - updatedAt;
        return ageSeconds >= -60L && ageSeconds <= WEATHER_FRESHNESS_SECONDS;
    }

    public String formatZoneDuration(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        if (safeSeconds < 60) {
            return context.getString(R.string.duration_seconds_format, safeSeconds);
        }
        int minutes = safeSeconds / 60;
        int remainingSeconds = safeSeconds % 60;
        if (remainingSeconds == 0) {
            return context.getString(R.string.settings_minutes_format, minutes);
        }
        return context.getString(
                R.string.duration_minutes_seconds_format,
                minutes,
                remainingSeconds
        );
    }

    public int clampProgress(double progress) {
        return (int) Math.max(0, Math.min(Math.round(progress), 100));
    }

    public double normalizePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double normalizedValue = value;
        if (normalizedValue >= 0.0 && normalizedValue <= 1.0) {
            normalizedValue *= 100.0;
        }
        return Math.max(0.0, Math.min(normalizedValue, 100.0));
    }

    public String unavailableValue() {
        return context.getString(R.string.ai_value_unavailable);
    }
}
