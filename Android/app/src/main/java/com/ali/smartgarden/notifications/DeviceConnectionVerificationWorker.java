package com.ali.smartgarden.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ali.smartgarden.models.Status;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

/** Rechecks a suspected Pi outage before a user-visible notification is created. */
public final class DeviceConnectionVerificationWorker extends Worker {
    private static final String UNIQUE_WORK = "avora-device-connection-verification";

    public DeviceConnectionVerificationWorker(@NonNull Context context,
                                              @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    static void schedule(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                DeviceConnectionVerificationWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setInitialDelay(
                        NotificationPolicy.DEVICE_OFFLINE_CONFIRMATION_MILLIS,
                        TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request);
    }

    static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(UNIQUE_WORK);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            DataSnapshot connection = Tasks.await(FirebaseDatabase.getInstance()
                            .getReference(".info/connected")
                            .get(),
                    10, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(connection.getValue(Boolean.class))) {
                NotificationSignalCoordinator
                        .discardDeviceConnectionCandidates(context);
                return Result.success();
            }

            DataSnapshot snapshot = Tasks.await(FirebaseDatabase.getInstance()
                            .getReference("devices")
                            .child("smartgarden-001")
                            .child("status")
                            .get(),
                    20, TimeUnit.SECONDS);
            Status status = snapshot.getValue(Status.class);
            if (status == null) return Result.retry();

            long nowEpoch = System.currentTimeMillis() / 1000L;
            boolean offline = NotificationPolicy.isDeviceOffline(
                    status.isOnline(), status.getLastSeenEpoch(), nowEpoch,
                    NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS);
            if (offline) {
                NotificationSignalCoordinator.evaluateDeviceConnection(context, status);
            } else {
                // A transient stale read recovered before confirmation. Clear it silently.
                NotificationSignalCoordinator.synchronizeDeviceConnection(context, status);
            }
            return Result.success();
        } catch (Exception ignored) {
            return Result.retry();
        }
    }
}
