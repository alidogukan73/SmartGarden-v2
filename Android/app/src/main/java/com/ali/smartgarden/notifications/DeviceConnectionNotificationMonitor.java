package com.ali.smartgarden.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public final class DeviceConnectionNotificationMonitor {

    private static final long CHECK_INTERVAL_MILLIS = 15_000L;
    private static final long STARTUP_SETTLE_MILLIS = 15_000L;

    private final Context context;
    private final FirebaseRepository repository;
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Status latestStatus;
    private boolean receivedStatus;
    private boolean started;
    private boolean connectionStateInitialized;
    private boolean startupEvaluationScheduled;

    public DeviceConnectionNotificationMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new FirebaseRepository();
    }

    private final ValueEventListener statusListener =
            new ValueEventListener() {

                @Override
                public void onDataChange(
                        @NonNull DataSnapshot snapshot
                ) {

                    Status status =
                            snapshot.getValue(Status.class);

                    if (status == null) {
                        return;
                    }

                    latestStatus = status;
                    receivedStatus = true;

                    /*
                     * Firebase uygulama açılışında önce yerel önbellekteki eski
                     * status kaydını verebilir. İlk bağlantı kararını kısa süre
                     * erteleyerek cache ve sunucu verisinin uzlaşmasını bekle.
                     */
                    if (!connectionStateInitialized) {
                        scheduleStartupEvaluation();
                        return;
                    }

                    NotificationSignalCoordinator
                            .evaluateDeviceConnection(
                                    context,
                                    latestStatus
                            );

                    NotificationSignalCoordinator
                            .evaluateDevice(
                                    context,
                                    latestStatus,
                                    null
                            );
                }

                @Override
                public void onCancelled(
                        @NonNull DatabaseError error
                ) {
                    // Firebase bağlantı hatası Raspberry Pi
                    // offline olayı olarak değerlendirilmez.
                }
            };

    private final Runnable startupEvaluator =
            new Runnable() {

                @Override
                public void run() {
                    startupEvaluationScheduled = false;

                    if (!receivedStatus || latestStatus == null) {
                        return;
                    }

                    NotificationSignalCoordinator
                            .synchronizeDeviceConnection(
                                    context,
                                    latestStatus
                            );

                    connectionStateInitialized = true;

                    NotificationSignalCoordinator
                            .evaluateDevice(
                                    context,
                                    latestStatus,
                                    null
                            );
                }
            };

    private final Runnable heartbeatChecker =
            new Runnable() {

                @Override
                public void run() {

                    if (receivedStatus
                            && connectionStateInitialized) {

                        NotificationSignalCoordinator
                                .evaluateDeviceConnection(
                                        context,
                                        latestStatus
                                );
                    }

                    handler.postDelayed(
                            this,
                            CHECK_INTERVAL_MILLIS
                    );
                }
            };

    private void scheduleStartupEvaluation() {

        if (startupEvaluationScheduled) {
            return;
        }

        startupEvaluationScheduled = true;

        handler.postDelayed(
                startupEvaluator,
                STARTUP_SETTLE_MILLIS
        );
    }

    public void start() {

        if (started) {
            return;
        }

        started = true;

        repository.observeStatus(statusListener);

        handler.postDelayed(
                heartbeatChecker,
                CHECK_INTERVAL_MILLIS
        );
    }
}