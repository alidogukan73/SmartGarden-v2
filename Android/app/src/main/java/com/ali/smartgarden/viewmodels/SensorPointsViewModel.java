package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import java.util.ArrayList;
import java.util.List;

public class SensorPointsViewModel extends ViewModel {

    private final MediatorLiveData<List<GardenZone>> zones =
            new MediatorLiveData<>();

    private List<GardenZone> firebaseZones =
            new ArrayList<>();

    public SensorPointsViewModel() {

        FirebaseRepository repository =
                new FirebaseRepository();

        LiveData<List<GardenZone>> zoneSource =
                repository.observeGardenZones();

        zones.addSource(
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

    public LiveData<List<GardenZone>> getZones() {

        return zones;
    }

    private void publishZones() {

        if (!firebaseZones.isEmpty()) {
            zones.setValue(
                    new ArrayList<>(
                            firebaseZones
                    )
            );
            return;
        }

        List<GardenZone> fallbackZones =
                new ArrayList<>();

        fallbackZones.add(
                new GardenZone(
                        "zone-001",
                        "Domates",
                        "tomato",
                        "🍅",
                        "soil-001",
                        true,
                        1
                )
        );

        fallbackZones.add(
                new GardenZone(
                        "zone-002",
                        "Biber",
                        "pepper",
                        "🌶️",
                        "soil-002",
                        true,
                        2
                )
        );

        fallbackZones.add(
                new GardenZone(
                        "zone-003",
                        "Salatalık",
                        "cucumber",
                        "🥒",
                        "soil-003",
                        true,
                        3
                )
        );

        fallbackZones.add(
                new GardenZone(
                        "zone-004",
                        "Fasulye",
                        "bean",
                        "🫘",
                        "soil-004",
                        true,
                        4
                )
        );

        zones.setValue(fallbackZones);
    }
}
