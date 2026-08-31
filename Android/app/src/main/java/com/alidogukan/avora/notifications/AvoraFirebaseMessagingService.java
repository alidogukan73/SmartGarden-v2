package com.alidogukan.avora.notifications;

import com.alidogukan.avora.firebase.FirebaseRepository;
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
        new GardenNotificationManager(getApplicationContext()).receiveRemote(
                RemoteNotificationEvent.from(
                        message.getData(),
                        message.getMessageId()
                )
        );
    }
}
