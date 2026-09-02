package com.alidogukan.avora.notifications;

import java.util.concurrent.TimeUnit;

/** Decides when the app may ask for notification permission again. */
public final class NotificationPermissionPromptPolicy {
    public static final long RETRY_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(7);

    private NotificationPermissionPromptPolicy() { }

    public static boolean shouldPrompt(boolean permissionGranted,
                                       boolean previouslyPrompted,
                                       long lastPromptAtMillis,
                                       long nowMillis) {
        if (permissionGranted) return false;
        if (!previouslyPrompted) return true;
        if (lastPromptAtMillis <= 0L) return true;
        if (nowMillis < lastPromptAtMillis) return false;
        return nowMillis - lastPromptAtMillis >= RETRY_INTERVAL_MILLIS;
    }
}
