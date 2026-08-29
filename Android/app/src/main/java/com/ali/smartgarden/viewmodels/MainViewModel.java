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
import com.ali.smartgarden.health.GardenHealthCalculator;
import com.ali.smartgarden.health.GardenHealthSummary;
import com.ali.smartgarden.health.GardenHealthZoneResult;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.notifications.NotificationPolicy;
import com.ali.smartgarden.notifications.NotificationSignalCoordinator;
import com.ali.smartgarden.plantassistant.PlantAssistantHomeRecommendation;
import com.ali.smartgarden.plantassistant.PlantAssistantRecommendationStore;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * Main screen data boundary. Every Firebase stream below is lifecycle-aware:
 * it owns a listener only while at least one UI observer is active.
 */
public class MainViewModel extends AndroidViewModel {
    public static final String ACTION_NOTIFICATIONS_CHANGED =
            GardenNotificationManager.ACTION_NOTIFICATIONS_CHANGED;

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
    private final MutableLiveData<Boolean> authenticated = new MutableLiveData<>();
    private final GardenNotificationManager notifications;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new FirebaseRepository();
        notifications = new GardenNotificationManager(application);
        sensorLiveData = repository.observeSensor(error -> handleFirebaseError());
        statusLiveData = repository.observeStatus(error -> handleFirebaseError());
        commandLiveData = repository.observeCommands(error -> handleFirebaseError());
        adaptiveRecommendation = repository.observeAdaptiveRecommendationData(
                error -> handleFirebaseError());
        aiDecisionLiveData = repository.observeAIDecision(error -> handleFirebaseError());
        aiExplanationLiveData = repository.observeAIExplanation(
                error -> handleFirebaseError());
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
    public LiveData<Boolean> getAuthenticated() { return authenticated; }

    public String getDeviceAuthorizationId() {
        return repository.getCurrentUserId();
    }

    public void authenticate() {
        repository.authenticateAnonymously().addOnCompleteListener(task ->
                authenticated.setValue(task.isSuccessful()
                        && Boolean.TRUE.equals(task.getResult())));
    }

    public void initializeNotificationSync() {
        notifications.restoreCloudBackup(imported -> { });
        repository.getPushToken().addOnSuccessListener(token ->
                repository.savePushToken(getApplication(), token));
    }

    public List<GardenZone> activeZones(List<GardenZone> zones) {
        return ZoneCapacityPolicy.activeZones(zones);
    }

    public void evaluateWateringSignals(List<WateringHistory> history, List<GardenZone> zones) {
        NotificationSignalCoordinator.evaluateWatering(getApplication(), history, zones);
    }

    public void evaluateIrrigationSignals(List<GardenZone> zones) {
        NotificationSignalCoordinator.evaluateIrrigationAi(getApplication(), zones);
    }

    public void evaluateWeatherSignals(WeatherForecast forecast) {
        if (forecast == null) return;
        NotificationSignalCoordinator.evaluateWeather(getApplication(),
                forecast.getTomorrowTemperatureMax(), forecast.getTomorrowRainProbability(),
                forecast.getTomorrowWindMax(), java.time.LocalDate.now().plusDays(1).toString(),
                forecast.getUpdatedAtEpoch());
    }

    public boolean shouldAcceptStatus(Status incoming, Status current, long nowEpoch) {
        return incoming != null && NotificationPolicy.shouldAcceptDeviceSnapshot(
                incoming.getLastSeenEpoch(), current == null ? 0L : current.getLastSeenEpoch(),
                nowEpoch);
    }

    public boolean isDeviceEffectivelyOnline(Status status, long nowEpoch) {
        return status != null && !NotificationPolicy.isDeviceOffline(status.isOnline(),
                status.getLastSeenEpoch(), nowEpoch,
                NotificationPolicy.DEVICE_HEARTBEAT_MAX_AGE_SECONDS);
    }

    public GardenHealthSummary gardenHealth(List<GardenZone> zones, long nowEpoch) {
        return GardenHealthCalculator.calculate(zones, nowEpoch,
                PlantAssistantRecommendationStore.healthSignal(getApplication()));
    }

    public GardenHealthZoneResult gardenHealthForZone(GardenZone zone, long nowEpoch) {
        return GardenHealthCalculator.evaluateZone(zone, nowEpoch,
                PlantAssistantRecommendationStore.healthSignal(getApplication()));
    }

    public PlantAssistantHomeRecommendation.Recommendation plantRecommendation(
            List<GardenZone> zones, WeatherForecast weather, long nowEpoch) {
        return PlantAssistantHomeRecommendation.evaluate(zones, weather,
                PlantAssistantRecommendationStore.healthSignal(getApplication()), nowEpoch);
    }

    public int unreadNotificationCount() {
        int unread = 0;
        for (com.ali.smartgarden.models.GardenNotification item :
                notifications.localNotifications()) {
            if (item != null && !item.isRead()) unread++;
        }
        return unread;
    }

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

    private void handleFirebaseError() {
        errorLiveData.setValue(AvoraLanguageManager.localizedContext(
                getApplication()).getString(
                R.string.firebase_connection_error));
    }
}
