package com.alidogukan.avora.notifications;

import android.content.Context;
import android.content.SharedPreferences;

/** Local state for the notification permission prompt cadence. */
public final class NotificationPermissionPromptStore {
    private static final String PREFERENCES = "avora_notification_settings";
    private static final String PERMISSION_REQUESTED = "phone_permission_requested";
    private static final String LAST_PROMPT_AT = "phone_permission_last_prompt_at";

    private final SharedPreferences preferences;

    public NotificationPermissionPromptStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE
        );
    }

    public boolean shouldPrompt(boolean permissionGranted, long nowMillis) {
        return NotificationPermissionPromptPolicy.shouldPrompt(
                permissionGranted,
                wasPrompted(),
                preferences.getLong(LAST_PROMPT_AT, 0L),
                nowMillis
        );
    }

    public boolean wasPrompted() {
        return preferences.getBoolean(PERMISSION_REQUESTED, false);
    }

    public void markPrompted(long nowMillis) {
        preferences.edit()
                .putBoolean(PERMISSION_REQUESTED, true)
                .putLong(LAST_PROMPT_AT, nowMillis)
                .apply();
    }
}
