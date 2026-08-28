package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.models.AIDecision;
import com.ali.smartgarden.models.AIExplanation;
import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.GardenAISummary;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.Sensor;
import com.ali.smartgarden.models.SoilLearningProfile;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.models.WeatherForecast;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;

import java.util.List;

/**
 * Main screen data boundary. Every Firebase stream below is lifecycle-aware:
 * it owns a listener only while at least one UI observer is active.
 */
public class MainViewModel extends AndroidViewModel {

    private final FirebaseRepository repository;
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final LiveData<Sensor> sensorLiveData;
    private final LiveData<Status> statusLiveData;
    private final LiveData<Command> commandLiveData;
    private final LiveData<AdaptiveRecommendation> adaptiveRecommendation;
    private final LiveData<AIDecision> aiDecisionLiveData;
    private final LiveData<AIExplanation> aiExplanationLiveData;
    private final LiveData<PredictionValidationStatus> predictionValidationStatus;
    private final LiveData<MoisturePrediction> moisturePrediction;
    private final LiveData<PredictionAccuracy> predictionAccuracy;
    private final LiveData<UnifiedConfidence> unifiedConfidence;
    private final LiveData<SoilLearningProfile> soilLearningProfile;
    private final LiveData<List<GardenZone>> gardenZones;
    private final LiveData<GardenAISummary> gardenAISummary;
    private final LiveData<WeatherForecast> weatherForecast;
    private final LiveData<List<WateringHistory>> wateringHistory;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new FirebaseRepository();
        sensorLiveData = repository.observeSensor(this::handleFirebaseError);
        statusLiveData = repository.observeStatus(this::handleFirebaseError);
        commandLiveData = repository.observeCommands(this::handleFirebaseError);
        adaptiveRecommendation = repository.observeAdaptiveRecommendationData(
                this::handleFirebaseError);
        aiDecisionLiveData = repository.observeAIDecision(this::handleFirebaseError);
        aiExplanationLiveData = repository.observeAIExplanation(this::handleFirebaseError);
        predictionValidationStatus = repository.observePredictionValidationStatus();
        moisturePrediction = repository.observeMoisturePrediction();
        predictionAccuracy = repository.observePredictionAccuracy();
        unifiedConfidence = repository.observeUnifiedConfidence();
        soilLearningProfile = repository.observeSoilLearningProfile();
        gardenZones = repository.observeGardenZones();
        gardenAISummary = repository.observeGardenAISummary();
        weatherForecast = repository.observeWeatherForecast();
        wateringHistory = repository.observeWateringHistory();
    }

    public LiveData<Sensor> getSensor() { return sensorLiveData; }
    public LiveData<Status> getStatus() { return statusLiveData; }
    public LiveData<Command> getCommand() { return commandLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<AdaptiveRecommendation> getAdaptiveRecommendation() {
        return adaptiveRecommendation;
    }
    public LiveData<AIDecision> getAIDecision() { return aiDecisionLiveData; }
    public LiveData<AIExplanation> getAIExplanation() { return aiExplanationLiveData; }
    public LiveData<PredictionValidationStatus> getPredictionValidationStatus() {
        return predictionValidationStatus;
    }
    public LiveData<MoisturePrediction> getMoisturePrediction() {
        return moisturePrediction;
    }
    public LiveData<PredictionAccuracy> getPredictionAccuracy() {
        return predictionAccuracy;
    }
    public LiveData<UnifiedConfidence> getUnifiedConfidence() { return unifiedConfidence; }
    public LiveData<SoilLearningProfile> getSoilLearningProfile() {
        return soilLearningProfile;
    }
    public LiveData<List<GardenZone>> getGardenZones() { return gardenZones; }
    public LiveData<GardenAISummary> getGardenAISummary() { return gardenAISummary; }
    public LiveData<WeatherForecast> getWeatherForecast() { return weatherForecast; }
    public LiveData<List<WateringHistory>> getWateringHistory() { return wateringHistory; }

    public void setRelay(boolean enabled) { repository.setRelay(enabled); }
    public void setAutoMode(boolean enabled) { repository.setAutoMode(enabled); }
    public void restartDevice() { repository.restartDevice(); }
    public Task<Void> restartIrrigationAssistant(String zoneId) {
        return repository.requestIrrigationAssistantRestart(zoneId);
    }
    public void openManualValve(GardenZone zone) {
        repository.requestZoneValveTest(zone, 10800);
    }
    public void closeManualValve() { repository.cancelZoneValveTest(); }
    public void setZoneValvePhysicalMode(GardenZone zone, boolean physical) {
        if (zone == null || zone.getZone_id() == null) return;
        repository.updateGardenZoneValveMode(zone.getZone_id(), physical);
    }

    private void handleFirebaseError(DatabaseError error) {
        errorLiveData.setValue(AvoraLanguageManager.localizedContext(
                getApplication()).getString(
                R.string.firebase_connection_error));
    }
}
