package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.calibration.SensorCalibrationRepository;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

/** Owns calibration safety checks and irrigation pause/restore operations. */
public final class SensorCalibrationViewModel extends ViewModel {
    public static final String ERROR_IRRIGATION_BUSY = "CALIBRATION_IRRIGATION_BUSY";
    public static final String ERROR_INVALID_ZONE = "CALIBRATION_INVALID_ZONE";

    private final SensorCalibrationRepository repository =
            new SensorCalibrationRepository();
    private final LiveData<List<GardenZone>> zones = repository.observeZones();

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public Task<CalibrationSession> beginSession(GardenZone zone) {
        ZoneIrrigationStatus status = zone == null ? null : zone.getIrrigation_status();
        if (status != null
                && (status.isWatering_active() || status.getQueue_position() > 0)) {
            return Tasks.forException(new IllegalStateException(ERROR_IRRIGATION_BUSY));
        }

        String zoneId = zone == null || zone.getZone_id() == null
                ? "" : zone.getZone_id().trim();
        if (!ZoneCapacityPolicy.isValidZoneId(zoneId)) {
            return Tasks.forException(new IllegalArgumentException(ERROR_INVALID_ZONE));
        }

        boolean restoreIrrigation = zone.isIrrigation_enabled();
        Task<Void> pauseTask = restoreIrrigation
                ? repository.setIrrigationEnabled(zoneId, false)
                : Tasks.forResult(null);
        return pauseTask.continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                throw error == null
                        ? new IllegalStateException("CALIBRATION_SAFETY_FAILED")
                        : error;
            }
            return new CalibrationSession(zoneId, restoreIrrigation);
        });
    }

    public Task<Void> save(
            CalibrationSession session,
            int dryRaw,
            int wetRaw
    ) {
        if (session == null) {
            return Tasks.forException(new IllegalStateException(ERROR_INVALID_ZONE));
        }
        return repository.complete(
                session.zoneId, dryRaw, wetRaw, session.restoreIrrigationEnabled);
    }

    public Task<Void> cancel(CalibrationSession session) {
        if (session == null || !session.restoreIrrigationEnabled) {
            return Tasks.forResult(null);
        }
        return repository.setIrrigationEnabled(session.zoneId, true);
    }

    public static final class CalibrationSession {
        public final String zoneId;
        public final boolean restoreIrrigationEnabled;

        public CalibrationSession(String zoneId, boolean restoreIrrigationEnabled) {
            this.zoneId = zoneId;
            this.restoreIrrigationEnabled = restoreIrrigationEnabled;
        }
    }
}
