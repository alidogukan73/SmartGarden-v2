package com.ali.smartgarden.notifications;

import android.content.Context;
import com.ali.smartgarden.R;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.language.AvoraLanguageManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Receives direct AVORA alerts even when the main activity is not running. */
public final class AvoraFirebaseMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        new FirebaseRepository().savePushToken(getApplicationContext(), token);
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Context context = AvoraLanguageManager.localizedContext(getApplicationContext());
        String type = value(message, "type", "SYSTEM");
        String priority = value(message, "priority", "NORMAL");
        String zoneId = value(message, "zone_id", "");
        String title = value(
                message, "title", context.getString(R.string.notification_remote_fallback_title));
        String description = value(
                message, "description",
                context.getString(R.string.notification_remote_fallback_description));
        String sourceKey = value(
                message, "source_key", message.getMessageId() == null ? "" : message.getMessageId());
        new GardenNotificationManager(context)
                .receiveRemote(type, priority, zoneId, title, description, sourceKey);
    }

    private static String value(RemoteMessage message, String key, String fallback) {
        String value = message.getData().get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
