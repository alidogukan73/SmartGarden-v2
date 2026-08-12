package com.ali.smartgarden.notifications;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Receives direct AVORA alerts even when the main activity is not running. */
public final class AvoraFirebaseMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        new FirebaseRepository().savePushToken(token);
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String type = value(message, "type", "SYSTEM");
        String priority = value(message, "priority", "NORMAL");
        String zoneId = value(message, "zone_id", "");
        String title = value(message, "title", "AVORA bildirimi");
        String description = value(message, "description", "Bahçenizde yeni bir gelişme var.");
        String sourceKey = value(message, "source_key", message.getMessageId() == null ? "" : message.getMessageId());
        new GardenNotificationManager(getApplicationContext())
                .receiveRemote(type, priority, zoneId, title, description, sourceKey);
    }

    private static String value(RemoteMessage message, String key, String fallback) {
        String value = message.getData().get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
