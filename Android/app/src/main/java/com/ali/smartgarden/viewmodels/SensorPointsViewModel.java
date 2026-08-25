package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensorPointsViewModel extends ViewModel {

    private final MediatorLiveData<List<GardenZone>> sensorPoints =
            new MediatorLiveData<>();
    private final MediatorLiveData<List<GardenZone>> configuredZones =
            new MediatorLiveData<>();

    private List<GardenZone> firebaseZones =
            new ArrayList<>();

    public SensorPointsViewModel() {

        FirebaseRepository repository =
                new FirebaseRepository();

        LiveData<List<GardenZone>> zoneSource =
                repository.observeGardenZones();

        sensorPoints.addSource(
                zoneSource,
                value -> {
                    firebaseZones =
                            value == null
                                    ? new ArrayList<>()
                                    : value;
                    publishZones();
                }
        );
    }

    public LiveData<List<GardenZone>> getSensorPoints() {

        return sensorPoints;
    }

    public LiveData<List<GardenZone>> getConfiguredZones() {

        return configuredZones;
    }

    private void publishZones() {
        List<GardenZone> activeZones =
                ZoneCapacityPolicy.activeZones(firebaseZones);

        configuredZones.setValue(
                new ArrayList<>(activeZones)
        );

        sensorPoints.setValue(
                buildSensorPoints(activeZones)
        );
    }

    static List<GardenZone> buildSensorPoints(
            List<GardenZone> zones
    ) {
        Map<String, GardenZone> assignedZones =
                new HashMap<>();

        for (GardenZone zone : ZoneCapacityPolicy.activeZones(zones)) {
            String sensorId = safe(zone.getSensor_id())
                    .toLowerCase(Locale.US);
            if (sensorId.isEmpty()
                    || !ZoneCapacityPolicy.isValidSensorId(sensorId)) {
                continue;
            }
            assignedZones.putIfAbsent(sensorId, zone);
        }

        List<GardenZone> points =
                new ArrayList<>(ZoneCapacityPolicy.MAX_ZONES);
        for (int slot = 1; slot <= ZoneCapacityPolicy.MAX_ZONES; slot++) {
            String sensorId = ZoneCapacityPolicy.sensorId(slot);
            GardenZone assigned = assignedZones.get(sensorId);
            points.add(
                    assigned == null
                            ? unassignedPoint(sensorId, slot)
                            : assigned
            );
        }

        return points;
    }

    private static GardenZone unassignedPoint(
            String sensorId,
            int slot
    ) {
        GardenZone point = new GardenZone();
        point.setZone_id("");
        point.setName("");
        point.setEmoji("");
        point.setSensor_id(sensorId);
        point.setSensor_enabled(false);
        point.setEnabled(false);
        point.setOrder(slot);
        return point;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
