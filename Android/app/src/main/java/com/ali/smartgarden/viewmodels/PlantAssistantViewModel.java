package com.ali.smartgarden.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.R;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.ali.smartgarden.plantassistant.PlantAssistantAdvisor;
import com.ali.smartgarden.plantassistant.PlantAssistantRecommendationStore;
import com.ali.smartgarden.plantassistant.PlantAssistantResult;
import com.ali.smartgarden.plantassistant.PlantAssistantVisionClient;
import com.ali.smartgarden.plantassistant.PlantFollowUpStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Analysis and persistence boundary for the plant assistant screen. */
public final class PlantAssistantViewModel extends AndroidViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final LocalGardenPhotoStore photos;
    private final PlantFollowUpStore followUps;
    private final LocalGardenEventStore events;
    private final GardenNotificationManager notifications;
    private final LiveData<List<GardenZone>> zones = repository.observeGardenZones();
    private final LiveData<WeatherForecast> weather = repository.observeWeatherForecast();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlantAssistantViewModel(@NonNull Application application) {
        super(application);
        photos = new LocalGardenPhotoStore(application);
        followUps = new PlantFollowUpStore(application);
        events = new LocalGardenEventStore(application);
        notifications = new GardenNotificationManager(application);
    }

    public LiveData<List<GardenZone>> getZones() { return zones; }
    public LiveData<WeatherForecast> getWeather() { return weather; }

    public PlantAssistantResult assess(GardenZone zone, List<String> symptoms,
                                       String note, WeatherForecast weather,
                                       boolean hasPhoto) {
        return PlantAssistantAdvisor.assess(zone, symptoms, note, weather, hasPhoto);
    }

    public void saveRecommendation(String zoneId, String urgency,
                                   String title, String advice) {
        PlantAssistantRecommendationStore.save(
                getApplication(), zoneId, urgency, title, advice);
    }

    public JSONObject analyzeVision(Bitmap bitmap, JSONObject context) throws Exception {
        return PlantAssistantVisionClient.analyze(bitmap, context);
    }

    public void analyzeVisionAsync(Bitmap bitmap, GardenZone zone, List<String> symptoms,
                                   String note, WeatherForecast forecast,
                                   Consumer<JSONObject> success, Consumer<Throwable> failure) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("plant", zone.getName());
                payload.put("zone", zone.getZone_id());
                payload.put("moisture", zone.getMoisture());
                payload.put("moisture_limit", zone.getMoisture_limit());
                JSONArray symptomValues = new JSONArray();
                for (String symptom : symptoms) symptomValues.put(symptom);
                payload.put("symptoms", symptomValues);
                payload.put("note", note);
                if (forecast != null) {
                    payload.put("temperature", forecast.getCurrentTemperature());
                    payload.put("humidity", forecast.getCurrentHumidity());
                    payload.put("rain_probability", forecast.getTodayRainProbability());
                }
                success.accept(PlantAssistantVisionClient.analyze(bitmap, payload));
            } catch (Exception error) {
                failure.accept(error);
            }
        });
    }

    public String list(JSONArray values) { return PlantAssistantVisionClient.list(values); }

    public GardenPhoto archivePhoto(Uri uri, Bitmap bitmap, String zoneId,
                                    String note) throws Exception {
        if (uri != null) return photos.save(uri, zoneId, note, "plant_assistant");
        if (bitmap != null) return photos.save(bitmap, zoneId, note, "plant_assistant");
        return null;
    }

    public void archivePhotoAsync(Uri uri, Bitmap bitmap, String zoneId, String note,
                                  Consumer<GardenPhoto> success,
                                  Consumer<Throwable> failure) {
        executor.execute(() -> {
            try {
                success.accept(archivePhoto(uri, bitmap, zoneId, note));
            } catch (Exception error) {
                failure.accept(error);
            }
        });
    }

    public void finalizeAnalysisAsync(String photoId, String zoneId, String title,
                                      String meta, String context, String advice) {
        executor.execute(() -> {
            FollowUpResult followUp = registerFollowUp(zoneId, photoId, title);
            String contextText = context;
            if ("SCHEDULED".equals(followUp.type)
                    || "SCHEDULED_EXISTING".equals(followUp.type)) {
                contextText += "\n\n" + getApplication().getString(
                        R.string.runtime_follow_up_task);
            } else if ("COMPLETED".equals(followUp.type)) {
                contextText += "\n\n" + getApplication().getString(
                        R.string.runtime_follow_up_comparison, followUp.previousTitle);
            }
            GardenPhoto updated = updateAnalysis(photoId, title, meta, contextText, advice);
            if (updated == null) return;
            syncPhoto(updated);
            publish("HIGH", zoneId, title, getApplication().getString(
                    R.string.notification_plant_analysis_saved_description),
                    "plant_analysis:" + photoId);
            if ("SCHEDULED".equals(followUp.type)) {
                addFollowUpEvent(zoneId, "Takip fotoğrafı önerisi",
                        "Bitki Asistanı analizinden 3 gün sonra aynı bölgeden yeni fotoğraf ekleyin.",
                        "follow_up_" + photoId);
            } else if ("COMPLETED".equals(followUp.type)) {
                addFollowUpEvent(zoneId, "Takip değerlendirmesi",
                        "Yeni analiz, önceki Bitki Asistanı analiziyle karşılaştırılmak üzere kaydedildi.",
                        "follow_up_" + photoId);
                publish("NORMAL", zoneId, getApplication().getString(
                                R.string.notification_plant_follow_up_ready_title),
                        getApplication().getString(
                                R.string.notification_plant_follow_up_ready_description),
                        "follow_up_complete:" + photoId);
            }
        });
    }

    public FollowUpResult registerFollowUp(String zoneId, String photoId, String title) {
        PlantFollowUpStore.Result value = followUps.registerAnalysis(zoneId, photoId, title);
        return new FollowUpResult(value.type, value.previousTitle);
    }

    public GardenPhoto updateAnalysis(String photoId, String title, String meta,
                                      String context, String advice) {
        return photos.updateAnalysis(photoId, title, meta, context, advice);
    }

    public void syncPhoto(GardenPhoto photo) {
        if (photo == null) return;
        repository.saveGardenPhotoMetadata(photo).addOnSuccessListener(unused ->
                photos.updateSeasonId(photo.getId(), photo.getSeason_id()));
    }

    public void publish(String priority, String zoneId, String title,
                        String description, String sourceKey) {
        notifications.publishOnce("PLANT_ASSISTANT", priority, zoneId,
                title, description, sourceKey);
    }

    public void addFollowUpEvent(String zoneId, String type, String note, String sourceKey) {
        GardenEvent event = events.addAutomaticOncePerDay(zoneId, type, note, sourceKey);
        if (event != null) repository.saveGardenEvent(event);
    }

    public static final class FollowUpResult {
        public final String type;
        public final String previousTitle;

        FollowUpResult(String type, String previousTitle) {
            this.type = type == null ? "" : type;
            this.previousTitle = previousTitle == null ? "" : previousTitle;
        }
    }

    @Override protected void onCleared() {
        executor.shutdown();
    }
}
