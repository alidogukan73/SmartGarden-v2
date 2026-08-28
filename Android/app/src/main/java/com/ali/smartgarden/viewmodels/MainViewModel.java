package com.ali.smartgarden.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
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
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.SoilLearningProfile;
import com.ali.smartgarden.models.GardenAISummary;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.models.WateringHistory;

import java.util.List;


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

    private final LiveData<PredictionValidationStatus>
            predictionValidationStatus;

    private final LiveData<MoisturePrediction> moisturePrediction;

    private final LiveData<PredictionAccuracy> predictionAccuracy;

    private final LiveData<UnifiedConfidence> unifiedConfidence;

    private final LiveData<SoilLearningProfile> soilLearningProfile;
    private final LiveData<List<GardenZone>> gardenZones;
    private final LiveData<GardenAISummary> gardenAISummary;
    private final LiveData<WeatherForecast> weatherForecast;
    private final LiveData<List<WateringHistory>> wateringHistory;
    private ValueEventListener sensorListener;
    private ValueEventListener statusListener;
    private ValueEventListener commandListener;
    private ValueEventListener adaptiveRecommendationListener;
    private ValueEventListener aiDecisionListener;
    private ValueEventListener aiExplanationListener;

    public MainViewModel() {

        repository = new FirebaseRepository();

        observeSensor();
        observeStatus();
        observeCommands();
        observeAdaptiveRecommendation();
        observeAIDecision();
        observeAIExplanation();

        predictionValidationStatus =
                repository.observePredictionValidationStatus();

        moisturePrediction =
                repository.observeMoisturePrediction();

        predictionAccuracy =
                repository.observePredictionAccuracy();

        unifiedConfidence =
                repository.observeUnifiedConfidence();

        soilLearningProfile =
                repository.observeSoilLearningProfile();

        gardenZones = repository.observeGardenZones();
        gardenAISummary = repository.observeGardenAISummary();
        weatherForecast = repository.observeWeatherForecast();
        wateringHistory = repository.observeWateringHistory();
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

    public LiveData<PredictionValidationStatus>
    getPredictionValidationStatus() {
        return predictionValidationStatus;
    }

    public LiveData<MoisturePrediction>
    getMoisturePrediction() {
        return moisturePrediction;
    }

    public LiveData<PredictionAccuracy>
    getPredictionAccuracy() {
        return predictionAccuracy;
    }

    public LiveData<UnifiedConfidence>
    getUnifiedConfidence() {
        return unifiedConfidence;
    }

    public LiveData<SoilLearningProfile>
    getSoilLearningProfile() {

        return soilLearningProfile;
    }

    public LiveData<List<GardenZone>> getGardenZones() {
        return gardenZones;
    }

    public LiveData<GardenAISummary> getGardenAISummary() {
        return gardenAISummary;
    }

    public LiveData<WeatherForecast> getWeatherForecast() {
        return weatherForecast;
    }

    public LiveData<List<WateringHistory>> getWateringHistory() { return wateringHistory; }


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

    public void restartDevice() {

        repository.restartDevice();

    }

    public Task<Void> restartIrrigationAssistant(String zoneId) {
        return repository.requestIrrigationAssistantRestart(zoneId);
    }

    /*
     * Firebase observers
     */

    private void observeSensor() {

        sensorListener = new ValueEventListener() {

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
                };
        repository.observeSensor(sensorListener);

    }

    public void openManualValve(GardenZone zone) {
        repository.requestZoneValveTest(
                zone,
                10800
        );
    }

    public void closeManualValve() {
        repository.cancelZoneValveTest();
    }

    public void setZoneValvePhysicalMode(
            GardenZone zone,
            boolean physical
    ) {
        if (zone == null || zone.getZone_id() == null) {
            return;
        }
        repository.updateGardenZoneValveMode(
                zone.getZone_id(),
                physical
        );
    }

    private void observeStatus() {

        statusListener = new ValueEventListener() {

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
                };
        repository.observeStatus(statusListener);

    }

    private void observeCommands() {

        commandListener = new ValueEventListener() {

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
                };
        repository.observeCommands(commandListener);
    }

    private void observeAdaptiveRecommendation() {

        adaptiveRecommendationListener = repository.observeAdaptiveRecommendation(
                recommendation -> {

                    if (recommendation == null) {

                        errorLiveData.setValue(
                                "Uyarlanabilir sulama önerisi okunamadı."
                        );

                        return;
                    }

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

        aiDecisionListener = new ValueEventListener() {

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
                };
        repository.observeAIDecision(aiDecisionListener);
    }

    private void observeAIExplanation() {

        aiExplanationListener = new ValueEventListener() {

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
                };
        repository.observeAIExplanation(aiExplanationListener);
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

    @Override
    protected void onCleared() {
        if (sensorListener != null) repository.removeSensorObserver(sensorListener);
        if (statusListener != null) repository.removeStatusObserver(statusListener);
        if (commandListener != null) repository.removeCommandsObserver(commandListener);
        if (adaptiveRecommendationListener != null) {
            repository.removeAdaptiveRecommendationObserver(adaptiveRecommendationListener);
        }
        if (aiDecisionListener != null) {
            repository.removeAIDecisionObserver(aiDecisionListener);
        }
        if (aiExplanationListener != null) {
            repository.removeAIExplanationObserver(aiExplanationListener);
        }
        super.onCleared();
    }
}
