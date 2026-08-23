package com.ali.smartgarden.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.AIDecision;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.WateringHistory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;

/** Converts meaningful weather, device and watering changes into deduplicated AVORA alerts. */
public final class NotificationSignalCoordinator {
    private static final long WEATHER_MAX_AGE_SECONDS = 12L * 60L * 60L;
    private static final long DEVICE_OFFLINE_AFTER_SECONDS = 90L;
    private static final long DEVICE_OFFLINE_CONFIRMATION_MILLIS = 15_000L;
    private static final long IRRIGATION_AI_MAX_AGE_MILLIS = 30L * 60L * 1000L;
    private static final String AI_STATE_PREFS = "avora_ai_notification_state";
    private static final String DEVICE_CONNECTION_PREFS = "avora_device_connection_state";
    private static final String DEVICE_OFFLINE_CANDIDATE_SINCE = "offline_candidate_since";
    private static final String DEVICE_OFFLINE_CANDIDATE_HEARTBEAT = "offline_candidate_heartbeat";
    private static final Object AI_STATE_LOCK = new Object();
    private static final Object DEVICE_CONNECTION_LOCK = new Object();

    private NotificationSignalCoordinator() { }

    public static void evaluateWeather(Context context, Double temperature, Double rain,
                                       Double wind, String forecastDate) {
        evaluateWeather(context, temperature, rain, wind, forecastDate,
                System.currentTimeMillis() / 1000L);
    }

    public static void evaluateWeather(Context context, Double temperature, Double rain,
                                       Double wind, String forecastDate, long updatedAtEpoch) {
        long nowEpoch = System.currentTimeMillis() / 1000L;
        if (!NotificationPolicy.isFreshEpochSeconds(
                updatedAtEpoch, nowEpoch, WEATHER_MAX_AGE_SECONDS)) {
            return;
        }
        double temp = value(temperature);
        double rainfall = value(rain);
        double windSpeed = value(wind);
        String date = forecastDate == null || forecastDate.isBlank()
                ? LocalDate.now().toString() : forecastDate;
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        if (rainfall >= 60D) {
            notifications.publishOnce("WEATHER", "HIGH", "",
                    context.getString(R.string.notification_weather_rain_title),
                    context.getString(R.string.notification_weather_rain_description,
                            Math.round(rainfall)),
                    "weather:rain:" + date);
        } else if (temp >= 35D) {
            notifications.publishOnce("WEATHER", "NORMAL", "",
                    context.getString(R.string.notification_weather_heat_title),
                    context.getString(R.string.notification_weather_heat_description,
                            Math.round(temp)),
                    "weather:heat:" + date);
        } else if (windSpeed >= 30D) {
            notifications.publishOnce("WEATHER", "NORMAL", "",
                    context.getString(R.string.notification_weather_wind_title),
                    context.getString(R.string.notification_weather_wind_description,
                            Math.round(windSpeed)),
                    "weather:wind:" + date);
        }
    }



    public static void evaluateDeviceConnection(Context context, Status status) {

        if (status == null) {
            return;
        }

        GardenNotificationManager notifications =
                new GardenNotificationManager(context);

        long nowMillis = System.currentTimeMillis();
        long nowEpoch = nowMillis / 1000L;

        long lastSeenEpoch =
                status.getLastSeenEpoch();

        long heartbeatAge = lastSeenEpoch <= 0L
                ? Long.MAX_VALUE
                : Math.max(0L, nowEpoch - lastSeenEpoch);

        boolean deviceOffline = NotificationPolicy.isDeviceOffline(
                status.isOnline(), lastSeenEpoch, nowEpoch, DEVICE_OFFLINE_AFTER_SECONDS);

        if (deviceOffline) {

            if (!confirmOfflineObservation(context, lastSeenEpoch, nowMillis)) {
                return;
            }

            String description;

            if (lastSeenEpoch <= 0L) {
                description = context.getString(
                        R.string.notification_device_offline_unknown);
            } else {
                long offlineMinutes =
                        Math.max(1L, heartbeatAge / 60L);

                description = context.getString(
                        R.string.notification_device_offline_minutes,
                        offlineMinutes);
            }

            notifications.publishIncident(
                    "device_offline",
                    "DEVICE",
                    "HIGH",
                    "",
                    context.getString(R.string.notification_device_offline_title),
                    description,
                    "device-offline:heartbeat",
                    GardenNotificationManager.DEVICE_INCIDENT_REMINDER_MILLIS
            );

            return;
        }

        clearOfflineCandidate(context);

        if (notifications.isIncidentActive("device_offline")) {

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    );

            String restoredAt =
                    formatter.format(
                            new Date(lastSeenEpoch * 1000L)
                    );

            notifications.publishOnce(
                    "DEVICE",
                    "NORMAL",
                    "",
                    context.getString(R.string.notification_device_recovered_title),
                    context.getString(
                            R.string.notification_device_recovered_description,
                            restoredAt),
                    "device-recovered:" + lastSeenEpoch
            );
        }

        notifications.resetIncident("device_offline");
    }

    /**
     * Seeds the foreground monitor after Firebase cache/server reconciliation.
     * An online launch silently clears an old incident; it never manufactures a
     * recovery alert merely because the application process was recreated.
     */
    public static void synchronizeDeviceConnection(Context context, Status status) {
        if (status == null) return;
        long nowMillis = System.currentTimeMillis();
        long nowEpoch = nowMillis / 1000L;
        boolean offline = NotificationPolicy.isDeviceOffline(
                status.isOnline(), status.getLastSeenEpoch(), nowEpoch,
                DEVICE_OFFLINE_AFTER_SECONDS);
        if (offline) {
            rememberOfflineCandidate(context, status.getLastSeenEpoch(), nowMillis);
        } else {
            clearOfflineCandidate(context);
            new GardenNotificationManager(context).resetIncident("device_offline");
        }
    }

    private static boolean confirmOfflineObservation(Context context,
                                                      long heartbeatEpoch,
                                                      long nowMillis) {
        synchronized (DEVICE_CONNECTION_LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(
                    DEVICE_CONNECTION_PREFS, Context.MODE_PRIVATE);
            long candidateSince = preferences.getLong(
                    DEVICE_OFFLINE_CANDIDATE_SINCE, 0L);
            long candidateHeartbeat = preferences.getLong(
                    DEVICE_OFFLINE_CANDIDATE_HEARTBEAT, Long.MIN_VALUE);
            if (candidateSince <= 0L || candidateHeartbeat != heartbeatEpoch) {
                preferences.edit()
                        .putLong(DEVICE_OFFLINE_CANDIDATE_SINCE, nowMillis)
                        .putLong(DEVICE_OFFLINE_CANDIDATE_HEARTBEAT, heartbeatEpoch)
                        .apply();
                return false;
            }
            return NotificationPolicy.isOfflineConfirmationDue(
                    candidateSince, nowMillis, DEVICE_OFFLINE_CONFIRMATION_MILLIS);
        }
    }

    private static void rememberOfflineCandidate(Context context,
                                                 long heartbeatEpoch,
                                                 long nowMillis) {
        synchronized (DEVICE_CONNECTION_LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(
                    DEVICE_CONNECTION_PREFS, Context.MODE_PRIVATE);
            long currentHeartbeat = preferences.getLong(
                    DEVICE_OFFLINE_CANDIDATE_HEARTBEAT, Long.MIN_VALUE);
            if (preferences.getLong(DEVICE_OFFLINE_CANDIDATE_SINCE, 0L) > 0L
                    && currentHeartbeat == heartbeatEpoch) {
                return;
            }
            preferences.edit()
                    .putLong(DEVICE_OFFLINE_CANDIDATE_SINCE, nowMillis)
                    .putLong(DEVICE_OFFLINE_CANDIDATE_HEARTBEAT, heartbeatEpoch)
                    .apply();
        }
    }

    private static void clearOfflineCandidate(Context context) {
        synchronized (DEVICE_CONNECTION_LOCK) {
            context.getSharedPreferences(DEVICE_CONNECTION_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(DEVICE_OFFLINE_CANDIDATE_SINCE)
                    .remove(DEVICE_OFFLINE_CANDIDATE_HEARTBEAT)
                    .apply();
        }
    }

    public static void evaluateDevice(Context context, Status status, Health health) {

        if (status == null) {
            return;
        }

        long nowEpoch =
                System.currentTimeMillis() / 1000L;

        long lastSeenEpoch =
                status.getLastSeenEpoch();

        long heartbeatAge =
                lastSeenEpoch <= 0L
                        ? Long.MAX_VALUE
                        : Math.max(
                        0L,
                        nowEpoch - lastSeenEpoch
                );

        boolean deviceOffline =
                !status.isOnline()
                        || heartbeatAge > DEVICE_OFFLINE_AFTER_SECONDS;

        if (deviceOffline) {

            GardenNotificationManager notifications =
                    new GardenNotificationManager(context);

            notifications.resetIncident(
                    "device_error"
            );

            return;
        }

        GardenNotificationManager notifications =
                new GardenNotificationManager(context);

        String error = status.getLastError();

        if (error != null && !error.isBlank()) {

            boolean sensorFailure = isSensorFailure(error);
            String incidentId = status.getErrorIncidentId();

            String errorKind = sensorFailure ? "sensor" : "system";
            String stableSource =
                    incidentId == null || incidentId.isBlank()
                            ? "device-error:" + errorKind + ":legacy"
                            : "device-error:" + errorKind + ":incident:" + incidentId;

            notifications.publishIncident(
                    "device_error",
                    "DEVICE",
                    "HIGH",
                    "",
                    sensorFailure
                            ? context.getString(R.string.notification_sensor_unavailable_title)
                            : context.getString(R.string.notification_device_warning_title),
                    sensorFailure
                            ? context.getString(
                                    R.string.notification_sensor_unavailable_description)
                            : error,
                    stableSource,
                    GardenNotificationManager.DEVICE_INCIDENT_REMINDER_MILLIS
            );

        } else {
            notifications.resetIncident("device_error");
        }

        if (health != null) {

            boolean warning =
                    health.isUnderVoltageNow()
                            || health.isThrottledNow()
                            || health.isFrequencyCappedNow()
                            || health.getCpuTemperature() >= 80D
                            || health.getDiskUsage() >= 90D;

            if (warning) {

                notifications.publishIncident(
                        "device_health",
                        "DEVICE",
                        "HIGH",
                        "",
                        context.getString(R.string.notification_device_health_title),
                        context.getString(R.string.notification_device_health_description),
                        "device:health",
                        GardenNotificationManager.DEVICE_INCIDENT_REMINDER_MILLIS
                );

            } else {
                notifications.resetIncident("device_health");
            }
        }
    }

    private static boolean isSensorFailure(String error) {
        String normalized = error.toLowerCase(Locale.ROOT);
        return normalized.contains("sensor") || normalized.contains("sensör")
                || normalized.contains("wireless") || normalized.contains("mqtt")
                || normalized.contains("soil moisture") || normalized.contains("measurement")
                || normalized.contains("ölçüm");
    }

    /**
     * Reports a fresh, actionable per-zone AI decision only once per watering episode.
     * Learning, waiting and low-confidence intermediate states stay visible in the
     * assistant but deliberately do not become phone notifications.
     */
    public static void evaluateIrrigationAi(Context context, List<GardenZone> zones) {
        NotificationSettingsStore settings = new NotificationSettingsStore(context);
        if (zones == null || !settings.isCategoryEnabled("irrigation")
                || !settings.isReminderEnabled("irrigation")) return;

        GardenNotificationManager notifications = new GardenNotificationManager(context);
        SharedPreferences preferences = context.getSharedPreferences(
                AI_STATE_PREFS, Context.MODE_PRIVATE);
        long nowMillis = System.currentTimeMillis();

        synchronized (AI_STATE_LOCK) {
            SharedPreferences.Editor editor = preferences.edit();
            for (GardenZone zone : zones) {
                if (zone == null) continue;
                String zoneId = safe(zone.getZone_id(), safe(zone.getSensor_id(), "unknown"));
                String stateKey = "irrigation_active:" + zoneId;
                AIDecision decision = zone.getAi() == null ? null : zone.getAi().getDecision();
                String updatedAt = decision == null ? "" : safe(decision.getUpdatedAt(), "");
                if (updatedAt.isBlank() && zone.getAi() != null) {
                    updatedAt = safe(zone.getAi().getUpdatedAt(), "");
                }
                boolean actionable = decision != null
                        && NotificationPolicy.shouldNotifyIrrigationAi(
                        zone.isEnabled(), zone.isIrrigation_enabled(),
                        decision.isShouldWater(), updatedAt, nowMillis,
                        IRRIGATION_AI_MAX_AGE_MILLIS);

                if (!actionable) {
                    editor.remove(stateKey);
                    continue;
                }
                if (preferences.getBoolean(stateKey, false)) continue;

                String zoneName = safe(zone.getName(), zoneId);
                String detail = firstNonBlank(
                        decision.getDecisionMessage(),
                        decision.getPrimaryReason(),
                        context.getString(R.string.notification_irrigation_ai_description,
                                zoneName));
                long decisionTime = NotificationPolicy.parseTimestampMillis(
                        updatedAt, ZoneId.systemDefault());
                notifications.publishOnce(
                        "IRRIGATION",
                        isHighSeverity(decision.getSeverity()) ? "HIGH" : "NORMAL",
                        zoneId,
                        context.getString(R.string.notification_irrigation_ai_title, zoneName),
                        detail,
                        "irrigation_ai:" + zoneId + ":" + decisionTime
                );
                editor.putBoolean(stateKey, true);
            }
            editor.apply();
        }
    }

    private static boolean isHighSeverity(String severity) {
        if (severity == null) return false;
        String normalized = severity.trim().toUpperCase(Locale.ROOT);
        return "HIGH".equals(normalized) || "CRITICAL".equals(normalized)
                || "ERROR".equals(normalized);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Sends only newly completed cycles; opening a journal never replays old watering alerts. */
    public static void evaluateWatering(Context context, List<WateringHistory> records) {
        if (records == null
                || !new NotificationSettingsStore(context).isReminderEnabled("irrigation")) {
            return;
        }
        long now = System.currentTimeMillis();
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        for (WateringHistory record : records) {
            if (record == null) continue;
            long completedAt = parseTime(record.getFinishedAt());
            if (completedAt <= 0L || now < completedAt
                    || now - completedAt > 20L * 60L * 1000L) continue;
            String id = record.getRecordId() == null || record.getRecordId().isBlank()
                    ? record.getFinishedAt() : record.getRecordId();
            String zoneId = record.getZoneId() == null ? "" : record.getZoneId();
            if (record.isCompleted()) {
                notifications.publishOnce("IRRIGATION", "NORMAL", zoneId,
                        context.getString(R.string.notification_watering_completed_title),
                        context.getString(R.string.notification_watering_completed_description,
                                record.getDuration()),
                        "watering:" + zoneId + ":" + id);
            } else if (NotificationPolicy.shouldNotifyInterruptedWatering(
                    false, record.getDuration(), record.getStopReason())) {
                notifications.publishOnce("IRRIGATION", "HIGH", zoneId,
                        context.getString(R.string.notification_watering_interrupted_title),
                        context.getString(R.string.notification_watering_interrupted_description,
                                record.getDuration()),
                        "watering-interrupted:" + zoneId + ":" + id);
            }
        }
    }

    private static long parseTime(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        return NotificationPolicy.parseTimestampMillis(raw, ZoneId.systemDefault());
    }

    private static double value(Double number) {
        return number == null ? 0D : number;
    }
}