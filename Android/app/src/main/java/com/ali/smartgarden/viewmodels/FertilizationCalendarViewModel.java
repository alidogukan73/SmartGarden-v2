package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;

import java.util.List;

public class FertilizationCalendarViewModel extends ViewModel {

    private final LiveData<List<GardenZone>> zones;

    public FertilizationCalendarViewModel() {
        FirebaseRepository repository = new FirebaseRepository();
        zones = repository.observeGardenZones();
    }

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }
}
