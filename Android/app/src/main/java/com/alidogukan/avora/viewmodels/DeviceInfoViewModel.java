package com.alidogukan.avora.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.models.DeviceInfoSnapshot;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.Health;
import com.alidogukan.avora.models.Status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Converts the device Firebase tree into a UI-independent, immutable device summary. */
public final class DeviceInfoViewModel extends ViewModel {
    private static final long CONNECTION_FRESHNESS_MS = 180_000L;
    private final MediatorLiveData<DeviceInfoState> state = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> readError = new MutableLiveData<>(false);

    public DeviceInfoViewModel() {
        LiveData<DeviceInfoSnapshot> source =
                new FirebaseRepository().observeDeviceInfoSnapshot(error ->
                readError.setValue(true));
        state.addSource(source, snapshot -> {
            readError.setValue(false);
            state.setValue(map(snapshot, System.currentTimeMillis()));
        });
    }

    public LiveData<DeviceInfoState> getState() {
        return state;
    }

    public LiveData<Boolean> getReadError() {
        return readError;
    }

    static DeviceInfoState map(DeviceInfoSnapshot snapshot, long nowMillis) {
        Status status = snapshot == null ? null : snapshot.getStatus();
        Health health = snapshot == null ? null : snapshot.getHealth();
        long lastSeenMillis = resolveLastSeenMillis(status);
        boolean connected = status != null && status.isOnline() && lastSeenMillis > 0L
                && Math.abs(nowMillis - lastSeenMillis) <= CONNECTION_FRESHNESS_MS;
        int enabledZones = 0;
        int enabledSensors = 0;
        int physicalValves = 0;
        Set<String> firmwareVersions = new LinkedHashSet<>();
        if (snapshot != null) {
            for (GardenZone zone : snapshot.getZones()) {
                if (zone == null || !zone.isEnabled()) continue;
                enabledZones++;
                if (zone.isSensor_enabled() && meaningful(zone.getSensor_id())) enabledSensors++;
                if ("PHYSICAL".equalsIgnoreCase(zone.getValve_mode())
                        && meaningful(zone.getValve_id())) physicalValves++;
            }
            firmwareVersions.addAll(snapshot.getFirmwareVersions());
        }
        return new DeviceInfoState(connected, health, status, lastSeenMillis,
                enabledZones, enabledSensors, physicalValves, firmwareVersions);
    }

    private static long resolveLastSeenMillis(@Nullable Status status) {
        if (status == null) return 0L;
        long epoch = status.getLastSeenEpoch();
        if (epoch > 0L) return epoch < 100_000_000_000L ? epoch * 1000L : epoch;
        String raw = status.getLastSeen();
        if (!meaningful(raw)) return 0L;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            } catch (Exception ignoredAgain) {
                return 0L;
            }
        }
    }

    private static boolean meaningful(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class DeviceInfoState {
        public final boolean connected;
        public final Health health;
        public final Status status;
        public final long lastSeenMillis;
        public final int enabledZones;
        public final int enabledSensors;
        public final int physicalValves;
        public final Set<String> firmwareVersions;

        DeviceInfoState(boolean connected, Health health, Status status, long lastSeenMillis,
                        int enabledZones, int enabledSensors, int physicalValves,
                        Set<String> firmwareVersions) {
            this.connected = connected;
            this.health = health;
            this.status = status;
            this.lastSeenMillis = lastSeenMillis;
            this.enabledZones = enabledZones;
            this.enabledSensors = enabledSensors;
            this.physicalValves = physicalValves;
            this.firmwareVersions = Collections.unmodifiableSet(
                    new LinkedHashSet<>(firmwareVersions));
        }
    }
}
