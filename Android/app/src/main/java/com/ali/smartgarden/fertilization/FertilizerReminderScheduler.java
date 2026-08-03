package com.ali.smartgarden.fertilization;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class FertilizerReminderScheduler {

    private static final String PERIODIC_WORK =
            "fertilizer-reminder-periodic";
    private static final String IMMEDIATE_WORK =
            "fertilizer-reminder-immediate";

    private FertilizerReminderScheduler() {
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodic =
                new PeriodicWorkRequest.Builder(
                        FertilizerReminderWorker.class,
                        24,
                        TimeUnit.HOURS
                ).setConstraints(constraints).build();
        WorkManager manager = WorkManager.getInstance(
                context.getApplicationContext()
        );
        manager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic
        );

        OneTimeWorkRequest immediate =
                new OneTimeWorkRequest.Builder(
                        FertilizerReminderWorker.class
                ).setConstraints(constraints).build();
        manager.enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.REPLACE,
                immediate
        );
    }
}
