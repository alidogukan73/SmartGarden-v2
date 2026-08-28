package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.calibration.SensorCalibrationRepository;
import com.ali.smartgarden.calibration.SensorCalibrationSampler;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/** Owns calibration safety checks and irrigation pause/restore operations. */
public final class SensorCalibrationViewModel extends ViewModel {
    public static final String ERROR_IRRIGATION_BUSY = "CALIBRATION_IRRIGATION_BUSY";
    public static final String ERROR_INVALID_ZONE = "CALIBRATION_INVALID_ZONE";

    private final SensorCalibrationRepository repository =
            new SensorCalibrationRepository();
    private final LiveData<List<GardenZone>> zones = repository.observeZones();
    private final SensorCalibrationSampler sampler = new SensorCalibrationSampler();

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public List<GardenZone> activeSensorZones(List<GardenZone> values) {
        List<GardenZone> result = new ArrayList<>();
        for (GardenZone zone : ZoneCapacityPolicy.activeZones(values)) {
            String sensorId = safe(zone.getSensor_id()).toLowerCase(Locale.US);
            if (!sensorId.isEmpty() && ZoneCapacityPolicy.isValidSensorId(sensorId)) {
                result.add(zone);
            }
        }
        result.sort(Comparator.comparing(zone ->
                safe(zone.getSensor_id()).toLowerCase(Locale.US)));
        return result;
    }

    public void resetSamples() { sampler.reset(); }
    public boolean addSample(int raw, long updatedAtEpoch) {
        return sampler.addSample(raw, updatedAtEpoch);
    }
    public int sampleCount() { return sampler.getCount(); }
    public int requiredSamples() { return SensorCalibrationSampler.REQUIRED_SAMPLES; }
    public boolean isSampleComplete() { return sampler.isComplete(); }
    public boolean isSampleStable() { return sampler.isStable(); }
    public int sampleSpread() { return sampler.spread(); }
    public int sampleMedian() { return sampler.median(); }
    public boolean isValidCalibration(Integer dryRaw, Integer wetRaw) {
        return dryRaw != null && wetRaw != null
                && SensorCalibrationSampler.isValidCalibration(dryRaw, wetRaw);
    }
    public void restoreSamples(List<Integer> values, long lastEpoch) {
        sampler.restore(values, lastEpoch);
    }
    public ArrayList<Integer> sampleSnapshot() {
        return new ArrayList<>(sampler.snapshot());
    }
    public long lastSampleEpoch() { return sampler.getLastSampleEpoch(); }

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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
