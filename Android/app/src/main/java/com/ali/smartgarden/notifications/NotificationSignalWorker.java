package com.ali.smartgarden.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
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
            DataSnapshot device = Tasks.await(FirebaseDatabase.getInstance()
                    .getReference("devices").child("smartgarden-001").get(),
                    20, TimeUnit.SECONDS);
            DataSnapshot forecast = device.child("weather").child("forecast");
            NotificationSignalCoordinator.evaluateWeather(context,
                    number(forecast.child("tomorrow_temperature_max")),
                    number(forecast.child("tomorrow_rain_probability")),
                    number(forecast.child("tomorrow_wind_max")),
                    LocalDate.now().plusDays(1).toString());
            NotificationSignalCoordinator.evaluateDevice(context,
                    device.child("status").getValue(Status.class),
                    device.child("health").getValue(Health.class));
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
                    "Fotoğraf takip zamanı",
                    "Bitki Asistanı değerlendirmesi için aynı bölgeden yeni bir fotoğraf ekleyin.",
                    "photo_follow_up:" + task.photoId) != null) {
                followUps.markNotified(task.photoId);
            }
        }
    }

    private Double number(DataSnapshot value) {
        Number number = value.getValue(Number.class);
        return number == null ? null : number.doubleValue();
    }
}