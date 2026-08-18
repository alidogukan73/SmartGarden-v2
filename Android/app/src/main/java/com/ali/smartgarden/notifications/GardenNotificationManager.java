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
import androidx.core.app.TaskStackBuilder;
import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.FertilizerHistoryActivity;
import com.ali.smartgarden.activities.NotificationDetailActivity;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.activities.MainActivity;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Single entry point for all future AVORA notification sources. */
public final class GardenNotificationManager {
    private static final String PHONE_CHANNEL = "avora_garden_alerts";
    private static final String PHONE_CHANNEL_URGENT = "avora_garden_alerts_urgent";
    private static final String INCIDENT_PREFS = "avora_notification_incidents";
    public static final String ACTION_NOTIFICATIONS_CHANGED = "com.ali.smartgarden.NOTIFICATIONS_CHANGED";
    static final long DEVICE_INCIDENT_REMINDER_MILLIS = 6L * 60L * 60L * 1000L;
    private static final Object INCIDENT_LOCK = new Object();
    private final Context context;
    private final LocalGardenNotificationStore store;
    private final FirebaseRepository repository;
    public GardenNotificationManager(Context context) { this.context = context.getApplicationContext(); store = new LocalGardenNotificationStore(this.context); repository = new FirebaseRepository(); }

    public GardenNotification publish(String type, String priority, String zoneId,
                                      String title, String description, String sourceKey) {
        if (!new NotificationSettingsStore(context).isCategoryEnabled(type)) return null;

        GardenNotification value =
                store.add(type, priority, zoneId, title, description, sourceKey);

        repository.saveGardenNotification(value);

        notifyNotificationsChanged();

        showPhoneAlert(value);

        return value;
    }
    public GardenNotification publishOnce(String type, String priority, String zoneId,
                                          String title, String description, String sourceKey) {
        if (!new NotificationSettingsStore(context).isCategoryEnabled(type)) return null;

        GardenNotification value =
                store.addOnce(type, priority, zoneId, title, description, sourceKey);

        if (value != null) {
            repository.saveGardenNotification(value);

            notifyNotificationsChanged();

            showPhoneAlert(value);
        }

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
            value.setRead(read);
            value.setSaved(saved);

            repository.updateGardenNotificationState(
                    value.getId(),
                    read,
                    saved
            );

            notifyNotificationsChanged();
        }
    }
    public List<GardenNotification> localNotifications() { return store.load(); }

    public GardenNotification findLocalById(String id) {

        if (id == null || id.isBlank()) {
            return null;
        }

        for (GardenNotification value : store.load()) {

            if (value != null && id.equals(value.getId())) {
                return value;
            }
        }

        return null;
    }

    public void markRead(String id) {

        GardenNotification value = findLocalById(id);

        if (value == null || value.isRead()) {
            return;
        }

        setState(
                value,
                true,
                value.isSaved()
        );
    }
    public void deleteNotification(
            GardenNotification value,
            Consumer<Boolean> completed
    ) {
        if (value == null
                || value.getId() == null
                || value.getId().isBlank()) {

            if (completed != null) {
                completed.accept(false);
            }

            return;
        }

        List<String> ids = new ArrayList<>();
        ids.add(value.getId());

        List<GardenNotification> removable = new ArrayList<>();
        removable.add(value);

        repository.deleteGardenNotifications(ids)
                .addOnSuccessListener(unused -> {

                    // Aynı olay tekrar üretilmesin.
                    store.rememberDismissed(removable);

                    /*
                     * Firebase listener bildirimi bizden önce
                     * local store'dan kaldırmış olabilir.
                     * Bu nedenle removed == 0 hata değildir.
                     */
                    store.removeAll(ids);

                    NotificationManagerCompat
                            .from(context)
                            .cancel(value.getId().hashCode());

                    notifyNotificationsChanged();

                    /*
                     * Firebase silme işlemi başarılıysa
                     * operasyon başarılı kabul edilir.
                     */
                    if (completed != null) {
                        completed.accept(true);
                    }
                })
                .addOnFailureListener(error -> {

                    if (completed != null) {
                        completed.accept(false);
                    }
                });
    }
    public void clearUnsavedNotifications(Consumer<Integer> completed) {

        List<GardenNotification> current = store.load();
        List<GardenNotification> removable = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        for (GardenNotification value : current) {
            if (value == null) continue;

            if (!value.isSaved()
                    && value.getId() != null
                    && !value.getId().isBlank()) {

                removable.add(value);
                ids.add(value.getId());
            }
        }

        if (ids.isEmpty()) {
            if (completed != null) completed.accept(0);
            return;
        }

        repository.deleteGardenNotifications(ids)
                .addOnSuccessListener(unused -> {

                    // Kullanıcının sildiği olayları hatırla.
                    store.rememberDismissed(removable);

                    int removed = store.removeAll(ids);

                    NotificationManagerCompat notificationManager =
                            NotificationManagerCompat.from(context);

                    for (GardenNotification value : removable) {
                        notificationManager.cancel(
                                value.getId().hashCode()
                        );
                    }

                    notifyNotificationsChanged();

                    if (completed != null) {
                        completed.accept(removed);
                    }
                })
                .addOnFailureListener(error -> {
                    if (completed != null) {
                        completed.accept(-1);
                    }
                });
    }
    public void syncLocalBackup() { for (GardenNotification value : store.load()) repository.saveGardenNotification(value); }
    /** Restores backed-up history on another phone, then keeps any local-only alerts. */
    public void restoreCloudBackup(Consumer<Integer> completed) {
        repository.loadGardenNotifications(values -> {
            int imported = store.mergeFromCloud(values);

            if (imported > 0) {
                notifyNotificationsChanged();
            }

            if (completed != null) {
                completed.accept(imported);
            }
        });
    }

    /** Phone alerts are optional; the durable in-app record is always written first. */
    private void showPhoneAlert(GardenNotification value) {
        if (value == null || !new NotificationSettingsStore(context).shouldShowPhoneAlert(value.getType())) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String applicationId = FertilizerOutcomeFollowUpPolicy.applicationIdFromSource(
                value.getSource_key()
        );
        Intent intent;
        if (!applicationId.isBlank()) {
            intent = new Intent(context, FertilizerHistoryActivity.class)
                    .putExtra("outcome_application_id", applicationId)
                    .putExtra("zone_id", value.getZone_id())
                    .putExtra("notification_id", value.getId());
        } else {
            intent = new Intent(context, NotificationDetailActivity.class)
                    .putExtra("id", value.getId()).putExtra("type", value.getType()).putExtra("priority", value.getPriority())
                    .putExtra("zone_id", value.getZone_id()).putExtra("title", value.getTitle()).putExtra("description", value.getDescription())
                    .putExtra("source_key", value.getSource_key())
                    .putExtra("created_at_epoch", value.getCreated_at_epoch()).putExtra("read", value.isRead()).putExtra("saved", value.isSaved());
        }
        Intent mainIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(mainIntent);
        stackBuilder.addNextIntent(intent);

        PendingIntent pending = stackBuilder.getPendingIntent(
                value.getId().hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        boolean urgent =
                "HIGH".equalsIgnoreCase(value.getPriority());

        ensurePhoneChannels();

        String channelId = urgent
                ? PHONE_CHANNEL_URGENT
                : PHONE_CHANNEL;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_avora_notification_small)
                        .setColor(
                                ContextCompat.getColor(
                                        context,
                                        R.color.homeGardenPlanIcon
                                )
                        )
                        .setContentTitle(value.getTitle())
                        .setContentText(value.getDescription())
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(value.getDescription())
                        )
                        .setAutoCancel(true)
                        .setContentIntent(pending);

        NotificationManagerCompat.from(context).notify(
                value.getId().hashCode(),
                builder.build()
        );
    }

    private void ensurePhoneChannels() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (manager == null) {
            return;
        }

        NotificationChannel normal =
                new NotificationChannel(
                        PHONE_CHANNEL,
                        "AVORA bahçe bildirimleri",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        normal.setDescription(
                "Sulama, gübreleme, Bitki Asistanı, stok ve sistem bildirimleri"
        );

        manager.createNotificationChannel(normal);

        NotificationChannel urgent =
                new NotificationChannel(
                        PHONE_CHANNEL_URGENT,
                        "AVORA kritik uyarıları",
                        NotificationManager.IMPORTANCE_HIGH
                );

        urgent.setDescription(
                "Acil cihaz, kritik stok ve gecikmiş işlem uyarıları"
        );

        urgent.enableVibration(true);

        manager.createNotificationChannel(urgent);
    }

    private void notifyNotificationsChanged() {
        Intent intent = new Intent(ACTION_NOTIFICATIONS_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public void applyCloudSnapshot(
            List<GardenNotification> values
    ) {
        store.reconcileCloudSnapshot(values);
        notifyNotificationsChanged();
    }
}
