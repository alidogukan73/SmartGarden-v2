package com.ali.smartgarden.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
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
    private boolean firebaseConnected;
    private boolean statusReadCancelled;

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

                    long newestHeartbeat = latestStatus == null
                            ? 0L : latestStatus.getLastSeenEpoch();
                    if (!NotificationPolicy.shouldAcceptDeviceSnapshot(
                            status.getLastSeenEpoch(),
                            newestHeartbeat,
                            System.currentTimeMillis() / 1000L)) {
                        return;
                    }

                    latestStatus = status;
                    receivedStatus = true;
                    statusReadCancelled = false;

                    // Cached status is not proof that the Pi is offline. Pausing
                    // here must not erase an outage window that was already
                    // observed while Firebase was connected.
                    if (!firebaseConnected) {
                        return;
                    }

                    /*
                     * Firebase uygulama açılışında önce yerel önbellekteki eski
                     * status kaydını verebilir. İlk bağlantı kararını kısa süre
                     * erteleyerek cache ve sunucu verisinin uzlaşmasını bekle.
                     */
                    if (!connectionStateInitialized) {
                        scheduleStartupEvaluation();
                        return;
                    }

                    evaluateLatestStatus();

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
                    statusReadCancelled = true;
                }
            };

    private final ValueEventListener firebaseConnectionListener =
            new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = Boolean.TRUE.equals(
                            snapshot.getValue(Boolean.class));
                    if (firebaseConnected == connected) return;

                    firebaseConnected = connected;
                    connectionStateInitialized = false;
                    handler.removeCallbacks(startupEvaluator);
                    startupEvaluationScheduled = false;

                    if (!connected) {
                        return;
                    }

                    // Allow the listener to replace disk cache with server data.
                    if (receivedStatus && !statusReadCancelled) {
                        scheduleStartupEvaluation();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    firebaseConnected = false;
                }
            };

    private final Runnable startupEvaluator =
            new Runnable() {

                @Override
                public void run() {
                    startupEvaluationScheduled = false;

                    if (!firebaseConnected || statusReadCancelled
                            || !receivedStatus || latestStatus == null) {
                        return;
                    }

                    connectionStateInitialized = true;
                    evaluateLatestStatus();

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

                    if (!firebaseConnected || statusReadCancelled) {
                        // The phone's own network loss is not proof that the Pi
                        // recovered. Preserve any pending outage verification.
                    } else if (receivedStatus && connectionStateInitialized) {
                        evaluateLatestStatus();
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

    private void evaluateLatestStatus() {
        if (latestStatus == null || !firebaseConnected || statusReadCancelled) {
            return;
        }

        if (isLatestStatusOffline()) {
            // Seed the outage window without publishing from listener cache.
            // The worker below performs the live server verification.
            NotificationSignalCoordinator.synchronizeDeviceConnection(
                    context, latestStatus);
            DeviceConnectionVerificationWorker.schedule(context);
            return;
        }

        NotificationSignalCoordinator.evaluateDeviceConnection(
                context, latestStatus);
        if (new GardenNotificationManager(context)
                .isIncidentActive("device_offline")) {
            DeviceConnectionVerificationWorker.scheduleRecovery(context);
        } else {
            DeviceConnectionVerificationWorker.cancel(context);
        }
    }

    private boolean isLatestStatusOffline() {
        if (latestStatus == null) return false;
        return NotificationPolicy.isDeviceOfflineObservation(
                firebaseConnected,
                latestStatus.isOnline(),
                latestStatus.getLastSeenEpoch(),
                System.currentTimeMillis() / 1000L,
                NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS);
    }


    public void start() {

        if (started) {
            return;
        }

        started = true;

        FirebaseDatabase.getInstance()
                .getReference(".info/connected")
                .addValueEventListener(firebaseConnectionListener);

        repository.observeStatus(statusListener);

        handler.postDelayed(
                heartbeatChecker,
                CHECK_INTERVAL_MILLIS
        );
    }
}