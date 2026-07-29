package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.SoilSensor;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class DeviceHealthViewModel extends ViewModel {

    private final FirebaseRepository repository;

    private final MutableLiveData<Health> health =
            new MutableLiveData<>();

    private final MutableLiveData<SoilSensor> soilSensor =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(true);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private ValueEventListener healthListener;

    private ValueEventListener soilSensorListener;


    public DeviceHealthViewModel() {

        repository = new FirebaseRepository();

        observeHealth();
        observeSoilSensor();
    }


    /**
     * Firebase health düğümünü gerçek zamanlı dinler.
     */
    private void observeHealth() {

        loading.setValue(true);

        healthListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {

                Health value =
                        snapshot.getValue(
                                Health.class
                        );

                health.setValue(value);
                error.setValue(null);
                loading.setValue(false);
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError databaseError
            ) {

                loading.setValue(false);

                String message =
                        databaseError.getMessage();

                if (
                        message == null
                                || message.isBlank()
                ) {
                    message =
                            "Cihaz sağlık bilgileri alınamadı.";
                }

                error.setValue(message);
            }
        };

        repository
                .getHealthRef()
                .addValueEventListener(
                        healthListener
                );
    }

    private void observeSoilSensor() {

        soilSensorListener =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        SoilSensor value =
                                snapshot.getValue(
                                        SoilSensor.class
                                );

                        soilSensor.setValue(value);
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                    }
                };


        repository
                .getSensorRef()
                .addValueEventListener(
                        soilSensorListener
                );
    }


    public LiveData<Health> getHealth() {

        return health;
    }

    public LiveData<SoilSensor> getSoilSensor() {

        return soilSensor;
    }

    public LiveData<Boolean> getLoading() {

        return loading;
    }


    public LiveData<String> getError() {

        return error;
    }


    public void restartDevice() {

        repository.restartDevice();

    }

    @Override
    protected void onCleared() {

        super.onCleared();

        if (healthListener != null) {

            repository
                    .getHealthRef()
                    .removeEventListener(
                            healthListener
                    );
        }

        if (soilSensorListener != null) {

            repository
                    .getSensorRef()
                    .removeEventListener(
                            soilSensorListener
                    );
        }
    }
}