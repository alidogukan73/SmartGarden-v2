package com.ali.smartgarden.notifications;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Schedules an immediate and periodic safety/weather signal scan. */
public final class NotificationSignalScheduler {
    private static final String PERIODIC = "avora-notification-signals";
    private static final String IMMEDIATE = "avora-notification-signals-now";
    private NotificationSignalScheduler() { }
    public static void schedule(Context context) {
        Constraints network = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());
        manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(NotificationSignalWorker.class, 15, TimeUnit.MINUTES).setConstraints(network).build());
        manager.enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(NotificationSignalWorker.class).setConstraints(network).build());
    }
}
