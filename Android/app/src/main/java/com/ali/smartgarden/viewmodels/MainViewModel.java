package com.ali.smartgarden.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.Sensor;
import com.ali.smartgarden.models.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ali.smartgarden.models.AdaptiveRecommendation;

public class MainViewModel extends ViewModel {

    private static final String TAG = "MainViewModel";

    private final FirebaseRepository repository;

    private final MutableLiveData<Sensor> sensorLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<Status> statusLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<Command> commandLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<String> errorLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<AdaptiveRecommendation>
            adaptiveRecommendation =
            new MutableLiveData<>();

    public MainViewModel() {

        repository = new FirebaseRepository();

        observeSensor();

        observeStatus();

        observeCommands();

        observeAdaptiveRecommendation();
    }

    /*
     * Public LiveData
     */

    public LiveData<Sensor> getSensor() {
        return sensorLiveData;
    }

    public LiveData<Status> getStatus() {
        return statusLiveData;
    }

    public LiveData<Command> getCommand() {
        return commandLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<AdaptiveRecommendation>
    getAdaptiveRecommendation() {

        return adaptiveRecommendation;
    }

    /*
     * Commands
     */

    public void setRelay(
            boolean enabled
    ) {

        repository.setRelay(
                enabled
        );
    }

    public void setAutoMode(
            boolean enabled
    ) {

        repository.setAutoMode(
                enabled
        );
    }

    /*
     * Firebase observers
     */

    private void observeSensor() {

        repository.observeSensor(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {
                            return;
                        }

                        Sensor sensor =
                                snapshot.getValue(
                                        Sensor.class
                                );

                        if (sensor == null) {

                            errorLiveData.setValue(
                                    "Sensör verisi okunamadı."
                            );

                            return;
                        }

                        sensorLiveData.setValue(
                                sensor
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        handleFirebaseError(
                                error
                        );
                    }
                }
        );
    }

    private void observeStatus() {

        repository.observeStatus(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {
                            return;
                        }

                        Status status =
                                snapshot.getValue(
                                        Status.class
                                );

                        if (status == null) {

                            errorLiveData.setValue(
                                    "Cihaz durumu okunamadı."
                            );

                            return;
                        }

                        statusLiveData.setValue(
                                status
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        handleFirebaseError(
                                error
                        );
                    }
                }
        );
    }

    private void observeCommands() {

        repository.observeCommands(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {
                            return;
                        }

                        Command command =
                                snapshot.getValue(
                                        Command.class
                                );

                        if (command == null) {

                            errorLiveData.setValue(
                                    "Komut verisi okunamadı."
                            );

                            return;
                        }

                        commandLiveData.setValue(
                                command
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        handleFirebaseError(
                                error
                        );
                    }
                }
        );
    }

    private void observeAdaptiveRecommendation() {

        repository.observeAdaptiveRecommendation(

                recommendation -> {

                    adaptiveRecommendation.setValue(
                            recommendation
                    );

                    Log.d(
                            TAG,
                            "Adaptive recommendation updated: "
                                    + recommendation.getRecommendationType()
                    );
                }
        );
    }
    private void handleFirebaseError(
            DatabaseError error
    ) {

        String message =
                error.getMessage();

        Log.e(
                TAG,
                message
        );

        errorLiveData.setValue(
                message
        );
    }
}