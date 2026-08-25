package com.ali.smartgarden.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.plantassistant.PlantFollowUpStore;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/** Periodically checks Firebase and due local tasks while AVORA is not open. */
public final class NotificationSignalWorker extends Worker {
    public NotificationSignalWorker(@NonNull Context context,
                                    @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        publishDuePlantFollowUps(context);
        try {
            Tasks.await(new GardenNotificationManager(context).syncPendingCloudDeletions(),
                    20, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Local tombstones already prevent deleted alerts from returning.
        }
        try {
            DataSnapshot device = Tasks.await(FirebaseDatabase.getInstance()
                    .getReference("devices").child("smartgarden-001").get(),
                    20, TimeUnit.SECONDS);
            DataSnapshot forecast = device.child("weather").child("forecast");
            NotificationSignalCoordinator.evaluateWeather(context,
                    number(forecast.child("tomorrow_temperature_max")),
                    number(forecast.child("tomorrow_rain_probability")),
                    number(forecast.child("tomorrow_wind_max")),
                    LocalDate.now().plusDays(1).toString(),
                    longNumber(forecast.child("updated_at_epoch")));
            java.util.ArrayList<GardenZone> zones = new java.util.ArrayList<>();
            for (DataSnapshot child : device.child("zones").getChildren()) {
                GardenZone zone = child.getValue(GardenZone.class);
                if (zone != null) {
                    if (zone.getZone_id() == null || zone.getZone_id().isBlank()) {
                        zone.setZone_id(child.getKey());
                    }
                    zones.add(zone);
                }
            }
            NotificationSignalCoordinator.evaluateIrrigationAi(context, zones);

            Status status =
                    device.child("status").getValue(Status.class);

            Health health =
                    device.child("health").getValue(Health.class);

            DataSnapshot connection = Tasks.await(FirebaseDatabase.getInstance()
                            .getReference(".info/connected").get(),
                    10, TimeUnit.SECONDS);
            boolean firebaseConnected = Boolean.TRUE.equals(
                    connection.getValue(Boolean.class));

            long nowEpoch = System.currentTimeMillis() / 1000L;
            boolean deviceOffline = status != null
                    && NotificationPolicy.isDeviceOfflineObservation(
                    firebaseConnected, status.isOnline(), status.getLastSeenEpoch(), nowEpoch,
                    NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS);

            /*
             * A WorkManager run can start the application process and therefore
             * the foreground monitor at the same time. Seed the shared state here
             * instead of publishing from the worker's first snapshot. A separate
             * one-shot verification publishes only if the outage remains real.
             */
            if (!firebaseConnected) {
                NotificationSignalCoordinator
                        .discardDeviceConnectionCandidates(context);
                DeviceConnectionVerificationWorker.cancel(context);
            } else if (deviceOffline) {
                NotificationSignalCoordinator.synchronizeDeviceConnection(context, status);
                DeviceConnectionVerificationWorker.schedule(context);
            } else {
                NotificationSignalCoordinator.synchronizeDeviceConnection(context, status);
                DeviceConnectionVerificationWorker.cancel(context);
            }

            NotificationSignalCoordinator.evaluateDevice(
                    context,
                    status,
                    health
            );
            java.util.ArrayList<WateringHistory> watering = new java.util.ArrayList<>();
            for (DataSnapshot child : device.child("watering_history").getChildren()) {
                WateringHistory record = child.getValue(WateringHistory.class);
                if (record != null) {
                    record.setRecordId(child.getKey());
                    watering.add(record);
                }
            }
            NotificationSignalCoordinator.evaluateWatering(context, watering);
            return Result.success();
        } catch (Exception ignored) {
            return Result.retry();
        }
    }

    private void publishDuePlantFollowUps(Context context) {
        NotificationSettingsStore settings = new NotificationSettingsStore(context);
        if (!settings.isReminderEnabled("plant")) return;
        PlantFollowUpStore followUps = new PlantFollowUpStore(context);
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        for (PlantFollowUpStore.DueTask task
                : followUps.dueUnnotified(System.currentTimeMillis() / 1000L)) {
            if (notifications.publishOnce("PHOTO_FOLLOW_UP", "NORMAL", task.zoneId,
                    context.getString(R.string.notification_photo_follow_up_title),
                    context.getString(R.string.notification_photo_follow_up_description),
                    "photo_follow_up:" + task.photoId) != null) {
                followUps.markNotified(task.photoId);
            }
        }
    }

    private Double number(DataSnapshot value) {
        Number number = value.getValue(Number.class);
        return number == null ? null : number.doubleValue();
    }

    private long longNumber(DataSnapshot value) {
        Number number = value.getValue(Number.class);
        return number == null ? 0L : number.longValue();
    }
}