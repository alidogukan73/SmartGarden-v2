package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.SoilSensor;

public class SensorPointsViewModel extends ViewModel {

    private final LiveData<SoilSensor> soilSensor;

    public SensorPointsViewModel() {

        FirebaseRepository repository =
                new FirebaseRepository();

        soilSensor =
                repository.observeSoilSensor();
    }

    public LiveData<SoilSensor> getSoilSensor() {

        return soilSensor;
    }
}
