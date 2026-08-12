package com.ali.smartgarden.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.NotificationDetailActivity;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenNotification;
import java.util.List;
import java.util.function.Consumer;

/** Single entry point for all future AVORA notification sources. */
public final class GardenNotificationManager {
    private static final String PHONE_CHANNEL = "avora_garden_alerts";
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
    /** Called by FCM so remote events share the same durable AVORA notification flow. */
    public GardenNotification receiveRemote(String type, String priority, String zoneId, String title, String description, String sourceKey) {
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
        Intent intent = new Intent(context, NotificationDetailActivity.class)
                .putExtra("id", value.getId()).putExtra("type", value.getType()).putExtra("priority", value.getPriority())
                .putExtra("zone_id", value.getZone_id()).putExtra("title", value.getTitle()).putExtra("description", value.getDescription())
                .putExtra("created_at_epoch", value.getCreated_at_epoch()).putExtra("read", value.isRead()).putExtra("saved", value.isSaved())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
