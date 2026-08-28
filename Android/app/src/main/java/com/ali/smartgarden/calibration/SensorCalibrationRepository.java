package com.ali.smartgarden.calibration;

import androidx.lifecycle.LiveData;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Firebase boundary for the sensor calibration workflow. */
public final class SensorCalibrationRepository {
    private final FirebaseRepository firebase = new FirebaseRepository();

    public LiveData<List<GardenZone>> observeZones() {
        return firebase.observeGardenZones();
    }

    public Task<Void> setIrrigationEnabled(String zoneId, boolean enabled) {
        return firebase.setZoneIrrigationEnabledForCalibration(zoneId, enabled);
    }

    public Task<Void> complete(
            String zoneId,
            int dryRaw,
            int wetRaw,
            boolean restoreIrrigationEnabled
    ) {
        return firebase.completeSensorCalibration(
                zoneId, dryRaw, wetRaw, restoreIrrigationEnabled);
    }
}
