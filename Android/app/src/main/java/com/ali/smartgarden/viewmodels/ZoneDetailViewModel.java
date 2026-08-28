package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import com.google.android.gms.tasks.Task;

/** Data and command boundary for one zone settings screen. */
public final class ZoneDetailViewModel extends ViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();
    private LiveData<GardenZone> zone;
    private LiveData<Boolean> testActive;

    public void initialize(String zoneId) {
        if (zone != null) return;
        zone = repository.observeGardenZone(zoneId);
        testActive = repository.observeZoneTestActive(error -> { });
    }

    public LiveData<GardenZone> getZone() {
        if (zone == null) throw new IllegalStateException("initialize must be called first");
        return zone;
    }

    public LiveData<Boolean> getTestActive() {
        if (testActive == null) throw new IllegalStateException("initialize must be called first");
        return testActive;
    }

    public Task<Void> startTest(GardenZone zone, int durationSeconds) {
        return repository.requestZoneValveTest(zone, durationSeconds);
    }

    public Task<Void> cancelTest() {
        return repository.cancelZoneValveTest();
    }

    public Task<Void> saveSettings(String zoneId, boolean irrigationEnabled,
                                   int moistureLimit, int pumpDuration,
                                   int cooldownSeconds, int restartDelta,
                                   boolean sensorEnabled, int sensorDryRaw,
                                   int sensorWetRaw) {
        return repository.updateGardenZoneSettings(
                zoneId, irrigationEnabled, moistureLimit, pumpDuration,
                cooldownSeconds, restartDelta, sensorEnabled,
                sensorDryRaw, sensorWetRaw);
    }
}
