package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Health;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class DeviceHealthViewModel extends ViewModel {

    private final FirebaseRepository repository;

    private final MutableLiveData<Health> health =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(true);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private ValueEventListener healthListener;


    public DeviceHealthViewModel() {

        repository = new FirebaseRepository();

        observeHealth();
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


    public LiveData<Health> getHealth() {

        return health;
    }


    public LiveData<Boolean> getLoading() {

        return loading;
    }


    public LiveData<String> getError() {

        return error;
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
    }
}