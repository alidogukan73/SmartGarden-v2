package com.ali.smartgarden.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.Sensor;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.AIDecision;
import com.ali.smartgarden.models.AIExplanation;

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
    private final MutableLiveData<AIDecision>
            aiDecisionLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<AIExplanation>
            aiExplanationLiveData =
            new MutableLiveData<>();

    public MainViewModel() {

        repository = new FirebaseRepository();

        observeSensor();
        observeStatus();
        observeCommands();
        observeAdaptiveRecommendation();
        observeAIDecision();
        observeAIExplanation();
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
    public LiveData<AIDecision> getAIDecision() {
        return aiDecisionLiveData;
    }

    public LiveData<AIExplanation> getAIExplanation() {
        return aiExplanationLiveData;
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
    private void observeAIDecision() {

        repository.observeAIDecision(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {
                            return;
                        }

                        AIDecision decision =
                                snapshot.getValue(
                                        AIDecision.class
                                );

                        if (decision == null) {

                            errorLiveData.setValue(
                                    "AI karar verisi okunamadı."
                            );

                            return;
                        }

                        aiDecisionLiveData.setValue(
                                decision
                        );

                        Log.d(
                                TAG,
                                "AI decision updated: "
                                        + decision.getDecisionCode()
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

    private void observeAIExplanation() {

        repository.observeAIExplanation(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {
                            return;
                        }

                        AIExplanation explanation =
                                snapshot.getValue(
                                        AIExplanation.class
                                );

                        if (explanation == null) {

                            errorLiveData.setValue(
                                    "AI açıklama verisi okunamadı."
                            );

                            return;
                        }

                        aiExplanationLiveData.setValue(
                                explanation
                        );

                        Log.d(
                                TAG,
                                "AI explanation updated: "
                                        + explanation.getExplanationCode()
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

    private void handleFirebaseError(
            DatabaseError error
    ) {

        String message = error.getMessage();

        if (message == null || message.isBlank()) {
            message = "Firebase bağlantı hatası.";
        }

        Log.e(
                TAG,
                message
        );

        errorLiveData.setValue(
                message
        );
    }
}