package com.ali.smartgarden.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import org.junit.Test;

public class NotificationPolicyTest {
    @Test
    public void categoriesAreCaseInsensitiveAndSystemUsesDeviceCategory() {
        assertEquals("irrigation", NotificationPolicy.categoryFor("irrigation"));
        assertEquals("plant", NotificationPolicy.categoryFor("PHOTO_FOLLOW_UP"));
        assertEquals("device", NotificationPolicy.categoryFor("SYSTEM"));
    }

    @Test
    public void equalQuietHourBoundariesDoNotSilenceTheWholeDay() {
        assertFalse(NotificationPolicy.isQuietHour(true, 12, 8, 8));
        assertTrue(NotificationPolicy.isQuietHour(true, 23, 22, 7));
        assertTrue(NotificationPolicy.isQuietHour(true, 6, 22, 7));
        assertFalse(NotificationPolicy.isQuietHour(true, 12, 22, 7));
    }

    @Test
    public void wateringTimestampsSupportUtcOffsetAndLocalFormats() {
        ZoneId zone = ZoneId.of("Europe/Istanbul");
        assertTrue(NotificationPolicy.parseTimestampMillis("2026-08-21T10:00:00Z", zone) > 0L);
        assertTrue(NotificationPolicy.parseTimestampMillis("2026-08-21T13:00:00+03:00", zone) > 0L);
        assertTrue(NotificationPolicy.parseTimestampMillis("2026-08-21T13:00:00", zone) > 0L);
    }

    @Test
    public void staleWeatherIsRejected() {
        long now = 10_000L;
        assertTrue(NotificationPolicy.isFreshEpochSeconds(9_500L, now, 600L));
        assertFalse(NotificationPolicy.isFreshEpochSeconds(9_000L, now, 600L));
        assertFalse(NotificationPolicy.isFreshEpochSeconds(0L, now, 600L));
    }

    @Test
    public void outageNotificationUsesFourMinuteSafetyWindow() {
        assertEquals(3L * 60L, NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS);
        assertEquals(60_000L, NotificationPolicy.DEVICE_OFFLINE_CONFIRMATION_MILLIS);
    }

    @Test
    public void deviceOfflineRuleHandlesHeartbeatAgeAndClockSkew() {
        long now = 10_000L;
        assertFalse(NotificationPolicy.isDeviceOffline(true, 9_950L, now, 90L));
        assertTrue(NotificationPolicy.isDeviceOffline(true, 9_800L, now, 90L));
        assertTrue(NotificationPolicy.isDeviceOffline(false, 10_000L, now, 90L));
        assertFalse(NotificationPolicy.isDeviceOffline(true, 10_005L, now, 90L));
    }

    @Test
    public void cachedHeartbeatCannotDeclarePiOfflineWithoutFirebaseConnection() {
        long now = 10_000L;
        long staleHeartbeat = 9_000L;
        long maximumAge = NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS;

        assertFalse(NotificationPolicy.isDeviceOfflineObservation(
                false, true, staleHeartbeat, now, maximumAge));
        assertTrue(NotificationPolicy.isDeviceOfflineObservation(
                true, true, staleHeartbeat, now, maximumAge));
    }

    @Test
    public void staleDeviceSnapshotRequiresAConfirmationWindow() {
        long window = NotificationPolicy.DEVICE_OFFLINE_CONFIRMATION_MILLIS;
        assertFalse(NotificationPolicy.isOfflineConfirmationDue(
                1_000L, 1_000L + window - 1L, window));
        assertTrue(NotificationPolicy.isOfflineConfirmationDue(
                1_000L, 1_000L + window, window));
        assertFalse(NotificationPolicy.isOfflineConfirmationDue(
                0L, 1_000L + window, window));
    }

    @Test
    public void recoveryUsesTheSameStableObservationRule() {
        long window = NotificationPolicy.DEVICE_RECOVERY_CONFIRMATION_MILLIS;
        assertFalse(NotificationPolicy.isOfflineConfirmationDue(
                5_000L, 5_000L + window - 1L, window));
        assertTrue(NotificationPolicy.isOfflineConfirmationDue(
                5_000L, 5_000L + window, window));
    }

    @Test
    public void sensorErrorRecoveryRequiresTwoStableMinutes() {
        long window = NotificationPolicy.DEVICE_ERROR_RECOVERY_CONFIRMATION_MILLIS;
        assertEquals(2L * 60L * 1000L, window);
        assertFalse(NotificationPolicy.isOfflineConfirmationDue(
                10_000L, 10_000L + window - 1L, window));
        assertTrue(NotificationPolicy.isOfflineConfirmationDue(
                10_000L, 10_000L + window, window));
    }

    @Test
    public void olderDeviceSnapshotsCannotMoveHeartbeatBackwards() {
        long now = 10_000L;
        assertTrue(NotificationPolicy.shouldAcceptDeviceSnapshot(9_990L, 9_980L, now));
        assertTrue(NotificationPolicy.shouldAcceptDeviceSnapshot(9_990L, 9_990L, now));
        assertFalse(NotificationPolicy.shouldAcceptDeviceSnapshot(9_980L, 9_990L, now));
        assertFalse(NotificationPolicy.shouldAcceptDeviceSnapshot(0L, 9_990L, now));
        assertTrue(NotificationPolicy.shouldAcceptDeviceSnapshot(9_900L, 10_400L, now));
    }

    @Test
    public void verificationRetriesUntilOutageOrRecoveryTransitionCompletes() {
        assertTrue(NotificationPolicy.shouldRetryDeviceConnectionVerification(
                true, true, false));
        assertFalse(NotificationPolicy.shouldRetryDeviceConnectionVerification(
                true, true, true));
        assertTrue(NotificationPolicy.shouldRetryDeviceConnectionVerification(
                true, false, true));
        assertFalse(NotificationPolicy.shouldRetryDeviceConnectionVerification(
                true, false, false));
        assertFalse(NotificationPolicy.shouldRetryDeviceConnectionVerification(
                false, true, false));
    }

    @Test
    public void irrigationAiRequiresAnEnabledZoneAndFreshActionableDecision() {
        long now = 1_800_000L;
        String fresh = "1970-01-01T00:29:00Z";
        String stale = "1970-01-01T00:01:00Z";
        assertTrue(NotificationPolicy.shouldNotifyIrrigationAi(
                true, true, true, fresh, now, 5L * 60L * 1000L));
        assertFalse(NotificationPolicy.shouldNotifyIrrigationAi(
                true, true, false, fresh, now, 5L * 60L * 1000L));
        assertFalse(NotificationPolicy.shouldNotifyIrrigationAi(
                true, false, true, fresh, now, 5L * 60L * 1000L));
        assertFalse(NotificationPolicy.shouldNotifyIrrigationAi(
                true, true, true, stale, now, 5L * 60L * 1000L));
    }

    @Test
    public void onlyActionableFertilizerStatesCreateAiAlerts() {
        assertTrue(NotificationPolicy.isActionableFertilizerAdvice("BUGÜNKÜ ÖNERİ"));
        assertTrue(NotificationPolicy.isActionableFertilizerAdvice("ORGANİK ÜRÜN GEREKİYOR"));
        assertTrue(NotificationPolicy.isActionableFertilizerAdvice("ÖNCE SULAMA"));
        assertTrue(NotificationPolicy.isActionableFertilizerAdvice("HAZIRLIK GEREKİYOR"));
        assertFalse(NotificationPolicy.isActionableFertilizerAdvice("HENÜZ ERKEN"));
        assertFalse(NotificationPolicy.isActionableFertilizerAdvice("SEZON TAMAMLANDI"));
    }

    @Test
    public void onlyAStartedPhysicalWateringCreatesAnInterruptionAlert() {
        assertTrue(NotificationPolicy.shouldNotifyInterruptedWatering(
                false, 4, "SYSTEM_DISABLED"));
        assertFalse(NotificationPolicy.shouldNotifyInterruptedWatering(
                true, 4, "COMPLETED"));
        assertFalse(NotificationPolicy.shouldNotifyInterruptedWatering(
                false, 0, "ERROR"));
        assertFalse(NotificationPolicy.shouldNotifyInterruptedWatering(
                false, 4, "VALVE_SIMULATION"));
    }

    @Test
    public void notificationsRequireTheCurrentActiveSeason() {
        assertTrue(NotificationPolicy.recordBelongsToActiveSeason(
                true, true, "season-new", false, "season-new"));
        assertFalse(NotificationPolicy.recordBelongsToActiveSeason(
                true, false, "season-new", false, "season-new"));
        assertFalse(NotificationPolicy.recordBelongsToActiveSeason(
                true, true, "season-new", false, "season-old"));
        assertFalse(NotificationPolicy.recordBelongsToActiveSeason(
                true, true, "season-new", false, ""));
        assertTrue(NotificationPolicy.recordBelongsToActiveSeason(
                true, true, "season-legacy", true, ""));
    }
}