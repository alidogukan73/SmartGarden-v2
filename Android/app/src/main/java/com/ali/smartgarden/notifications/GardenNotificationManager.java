package com.ali.smartgarden.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.FertilizerHistoryActivity;
import com.ali.smartgarden.activities.NotificationDetailActivity;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenNotification;
import java.util.List;
import java.util.function.Consumer;

/** Single entry point for all future AVORA notification sources. */
public final class GardenNotificationManager {
    private static final String PHONE_CHANNEL = "avora_garden_alerts";
    private static final String INCIDENT_PREFS = "avora_notification_incidents";
    static final long DEVICE_INCIDENT_REMINDER_MILLIS = 6L * 60L * 60L * 1000L;
    private static final Object INCIDENT_LOCK = new Object();
    private final Context context;
    private final LocalGardenNotificationStore store;
    private final FirebaseRepository repository;
    public GardenNotificationManager(Context context) { this.context = context.getApplicationContext(); store = new LocalGardenNotificationStore(this.context); repository = new FirebaseRepository(); }

    public GardenNotification publish(String type, String priority, String zoneId, String title, String description, String sourceKey) {
        if (!new NotificationSettingsStore(context).isCategoryEnabled(type)) return null;
        GardenNotification value = store.add(type, priority, zoneId, title, description, sourceKey);
        repository.saveGardenNotification(value);
        showPhoneAlert(value);
        return value;
    }
    public GardenNotification publishOnce(String type, String priority, String zoneId, String title, String description, String sourceKey) {
        if (!new NotificationSettingsStore(context).isCategoryEnabled(type)) return null;
        GardenNotification value = store.addOnce(type, priority, zoneId, title, description, sourceKey);
        if (value != null) { repository.saveGardenNotification(value); showPhoneAlert(value); }
        return value;
    }
    /**
     * Publishes the first alert of an incident immediately, then at most one reminder
     * per interval. State is persisted so the foreground listener, WorkManager and
     * FCM cannot produce duplicate alerts for the same continuing outage.
     */
    public GardenNotification publishIncident(String incidentKey, String type, String priority,
                                               String zoneId, String title, String description,
                                               String sourceKey, long reminderIntervalMillis) {
        if (incidentKey == null || incidentKey.isBlank()
                || !new NotificationSettingsStore(context).isCategoryEnabled(type)) return null;
        synchronized (INCIDENT_LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(INCIDENT_PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            long lastSentAt = preferences.getLong("last:" + incidentKey, 0L);
            boolean active = preferences.getBoolean("active:" + incidentKey, false);
            String previousSource = preferences.getString("source:" + incidentKey, "");
            boolean sameIncident = sourceKey == null || sourceKey.isBlank()
                    || sourceKey.equals(previousSource);
            if (active && sameIncident && lastSentAt > 0L
                    && now - lastSentAt < reminderIntervalMillis) return null;

            GardenNotification value = publish(type, priority, zoneId, title, description, sourceKey);
            if (value != null) {
                preferences.edit()
                        .putBoolean("active:" + incidentKey, true)
                        .putLong("last:" + incidentKey, now)
                        .putString("source:" + incidentKey, sourceKey == null ? "" : sourceKey)
                        .apply();
            }
            return value;
        }
    }

    /** A recovered signal starts a fresh incident, so a later outage is reported immediately. */
    public void resetIncident(String incidentKey) {
        if (incidentKey == null || incidentKey.isBlank()) return;
        synchronized (INCIDENT_LOCK) {
            context.getSharedPreferences(INCIDENT_PREFS, Context.MODE_PRIVATE).edit()
                    .remove("active:" + incidentKey)
                    .remove("last:" + incidentKey)
                    .remove("source:" + incidentKey)
                    .apply();
        }
    }

    /** Called by FCM so remote events share the same durable AVORA notification flow. */
    public GardenNotification receiveRemote(String type, String priority, String zoneId, String title, String description, String sourceKey) {
        if (sourceKey != null && sourceKey.startsWith("device-error:")) {
            String stableSource = sourceKey.startsWith("device-error:incident:")
                    ? sourceKey : "device-error:legacy";
            return publishIncident("device_error", type, priority, zoneId, title, description,
                    stableSource, DEVICE_INCIDENT_REMINDER_MILLIS);
        }
        return publishOnce(type, priority, zoneId, title, description, sourceKey);
    }
    public void setState(GardenNotification value, boolean read, boolean saved) {
        if (value == null) return;
        if (store.updateState(value.getId(), read, saved)) {
            value.setRead(read); value.setSaved(saved); repository.updateGardenNotificationState(value.getId(), read, saved);
        }
    }
    public List<GardenNotification> localNotifications() { return store.load(); }
    public void syncLocalBackup() { for (GardenNotification value : store.load()) repository.saveGardenNotification(value); }
    /** Restores backed-up history on another phone, then keeps any local-only alerts. */
    public void restoreCloudBackup(Consumer<Integer> completed) {
        repository.loadGardenNotifications(values -> {
            int imported = store.mergeFromCloud(values);
            if (completed != null) completed.accept(imported);
        });
    }

    /** Phone alerts are optional; the durable in-app record is always written first. */
    private void showPhoneAlert(GardenNotification value) {
        if (value == null || !new NotificationSettingsStore(context).shouldShowPhoneAlert(value.getType())) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensurePhoneChannel();
        String applicationId = FertilizerOutcomeFollowUpPolicy.applicationIdFromSource(
                value.getSource_key()
        );
        Intent intent;
        if (!applicationId.isBlank()) {
            intent = new Intent(context, FertilizerHistoryActivity.class)
                    .putExtra("outcome_application_id", applicationId)
                    .putExtra("zone_id", value.getZone_id());
        } else {
            intent = new Intent(context, NotificationDetailActivity.class)
                    .putExtra("id", value.getId()).putExtra("type", value.getType()).putExtra("priority", value.getPriority())
                    .putExtra("zone_id", value.getZone_id()).putExtra("title", value.getTitle()).putExtra("description", value.getDescription())
                    .putExtra("source_key", value.getSource_key())
                    .putExtra("created_at_epoch", value.getCreated_at_epoch()).putExtra("read", value.isRead()).putExtra("saved", value.isSaved());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, value.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean urgent = "HIGH".equals(value.getPriority());
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, PHONE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(value.getTitle()).setContentText(value.getDescription())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(value.getDescription())).setAutoCancel(true)
                .setPriority(urgent ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT).setContentIntent(pending);
        NotificationManagerCompat.from(context).notify(value.getId().hashCode(), notification.build());
    }

    private void ensurePhoneChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(PHONE_CHANNEL, "AVORA bahçe bildirimleri", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Sulama, gübreleme, Bitki Asistanı, stok ve cihaz bildirimleri");
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }
}
