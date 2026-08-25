package com.ali.smartgarden.notifications;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

/** Pure notification rules shared by settings, background scans and unit tests. */
public final class NotificationPolicy {
    public static final long DEVICE_HEARTBEAT_MAX_AGE_SECONDS = 5L * 60L;
    /** A single stale Firebase read is not enough to declare the Pi offline. */
    public static final long DEVICE_OFFLINE_CONFIRMATION_MILLIS = 2L * 60L * 1000L;
    /** Recovery must also remain stable before a recovery notification is emitted. */
    public static final long DEVICE_RECOVERY_CONFIRMATION_MILLIS = 60_000L;
    /** A brief sensor-data recovery must not split one outage into many incidents. */
    public static final long DEVICE_ERROR_RECOVERY_CONFIRMATION_MILLIS = 2L * 60L * 1000L;

    private NotificationPolicy() { }

    public static String categoryFor(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "IRRIGATION":
                return "irrigation";
            case "FERTILIZATION":
                return "fertilization";
            case "STOCK":
                return "stock";
            case "WEATHER":
                return "weather";
            case "DEVICE":
            case "SYSTEM":
                return "device";
            case "PLANT":
            case "PLANT_ASSISTANT":
            case "PHOTO_FOLLOW_UP":
            default:
                return "plant";
        }
    }

    public static boolean isQuietHour(boolean enabled, int hour, int startHour, int endHour) {
        if (!enabled) return false;
        int current = normalizeHour(hour);
        int start = normalizeHour(startHour);
        int end = normalizeHour(endHour);
        // Equal boundaries mean that no quiet interval was selected, not a 24-hour silence.
        if (start == end) return false;
        return start < end
                ? current >= start && current < end
                : current >= start || current < end;
    }

    public static long parseTimestampMillis(String raw, ZoneId fallbackZone) {
        if (raw == null || raw.isBlank()) return 0L;
        String value = raw.trim();
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) { }
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (Exception ignored) { }
        try {
            ZoneId zone = fallbackZone == null ? ZoneId.systemDefault() : fallbackZone;
            return LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static boolean isFreshEpochSeconds(long updatedAtEpoch, long nowEpoch,
                                               long maximumAgeSeconds) {
        return updatedAtEpoch > 0L
                && nowEpoch >= updatedAtEpoch
                && nowEpoch - updatedAtEpoch <= Math.max(0L, maximumAgeSeconds);
    }

    /**
     * A device is offline only when its explicit state is offline or its heartbeat
     * is older than the accepted age. Future timestamps are treated as fresh so a
     * small phone/Pi clock difference cannot create a false outage.
     */
    public static boolean isDeviceOffline(boolean online, long lastSeenEpoch,
                                          long nowEpoch, long maximumAgeSeconds) {
        if (!online || lastSeenEpoch <= 0L) return true;
        if (lastSeenEpoch > nowEpoch) return false;
        return nowEpoch - lastSeenEpoch > Math.max(0L, maximumAgeSeconds);
    }

    /** Cached heartbeat age is actionable only while Firebase is live. */
    public static boolean isDeviceOfflineObservation(
            boolean firebaseConnected, boolean online, long lastSeenEpoch,
            long nowEpoch, long maximumAgeSeconds) {
        return firebaseConnected && isDeviceOffline(
                online, lastSeenEpoch, nowEpoch, maximumAgeSeconds);
    }

    /**
     * Rejects a cached/transaction snapshot that moves the device heartbeat
     * backwards. This keeps temporary Firebase parent transactions from
     * manufacturing an offline/online transition.
     */
    public static boolean shouldAcceptDeviceSnapshot(long incomingHeartbeatEpoch,
                                                     long newestHeartbeatEpoch,
                                                     long nowEpoch) {
        if (incomingHeartbeatEpoch <= 0L) return newestHeartbeatEpoch <= 0L;
        if (newestHeartbeatEpoch <= 0L) return true;
        // If the phone clock was moved backwards, do not keep an impossible
        // future watermark forever.
        if (newestHeartbeatEpoch > nowEpoch + 5L * 60L) return true;
        return incomingHeartbeatEpoch >= newestHeartbeatEpoch;
    }

    /** A stale snapshot must remain stale for a short window before it is trusted. */
    public static boolean isOfflineConfirmationDue(long candidateSinceMillis,
                                                   long nowMillis,
                                                   long confirmationMillis) {
        return candidateSinceMillis > 0L
                && nowMillis >= candidateSinceMillis
                && nowMillis - candidateSinceMillis >= Math.max(0L, confirmationMillis);
    }

    public static boolean shouldNotifyIrrigationAi(
            boolean zoneEnabled,
            boolean irrigationEnabled,
            boolean shouldWater,
            String updatedAt,
            long nowMillis,
            long maximumAgeMillis
    ) {
        if (!zoneEnabled || !irrigationEnabled || !shouldWater) return false;
        long updatedAtMillis = parseTimestampMillis(updatedAt, ZoneId.systemDefault());
        return updatedAtMillis > 0L
                && nowMillis >= updatedAtMillis
                && nowMillis - updatedAtMillis <= Math.max(0L, maximumAgeMillis);
    }

    public static boolean isActionableFertilizerAdvice(String status) {
        if (status == null) return false;
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "BUGÜNKÜ ÖNERİ":
            case "ORGANİK ÜRÜN GEREKİYOR":
            case "ÖNCE SULAMA":
            case "HAZIRLIK GEREKİYOR":
                return true;
            default:
                return false;
        }
    }

    /** Interrupted watering is actionable only if the physical cycle actually ran. */
    public static boolean shouldNotifyInterruptedWatering(
            boolean completed,
            long durationSeconds,
            String stopReason
    ) {
        if (completed || durationSeconds <= 0) return false;
        String reason = stopReason == null
                ? ""
                : stopReason.trim().toUpperCase(Locale.ROOT);
        return !"VALVE_SIMULATION".equals(reason)
                && !"SHARED_PUMP_BUSY".equals(reason)
                && !"ZERO_DURATION".equals(reason);
    }

    private static int normalizeHour(int hour) {
        int normalized = hour % 24;
        return normalized < 0 ? normalized + 24 : normalized;
    }
}