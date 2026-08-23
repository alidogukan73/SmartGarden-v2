package com.ali.smartgarden.notifications;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Schedules the periodic background notification safety scan.
 */
public final class NotificationSignalScheduler {

    private static final String PERIODIC =
            "avora-notification-signals";

    private NotificationSignalScheduler() {
    }

    public static void schedule(Context context) {
        WorkManager manager =
                WorkManager.getInstance(
                        context.getApplicationContext()
                );

        manager.enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(
                        NotificationSignalWorker.class,
                        15,
                        TimeUnit.MINUTES
                )
                        .build()
        );
    }
}