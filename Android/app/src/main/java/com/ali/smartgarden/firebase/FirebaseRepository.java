package com.ali.smartgarden.firebase;

import android.util.Log;
import android.content.Context;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.FertilizerStageGuide;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.models.SoilLearningProfile;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.models.WeatherDay;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.models.WeatherLocation;
import com.ali.smartgarden.models.RainSettings;
import com.ali.smartgarden.models.GardenProfile;
import com.ali.smartgarden.models.DisplayUnitSettings;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FirebaseRepository {
   private static final String TAG = "FirebaseRepository";
   private static final String DEVICE_ID = "smartgarden-001";
   private final DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("devices").child("smartgarden-001");
   private final DatabaseReference primaryZoneRef;
   private final DatabaseReference statusRef;
   private final DatabaseReference commandsRef;
   private final DatabaseReference historyRef;
   private final DatabaseReference healthRef;
   private final DatabaseReference statisticsRef;
   private final DatabaseReference adaptiveRecommendationRef;
   private final DatabaseReference aiDecisionRef;
   private final DatabaseReference aiExplanationRef;
   private final DatabaseReference predictionValidationRef;
   private final DatabaseReference moisturePredictionRef;
   private final DatabaseReference predictionAccuracyRef;
   private final DatabaseReference unifiedConfidenceRef;
   private final DatabaseReference soilLearningProfileRef;
   private final DatabaseReference zonesRef;
   private final DatabaseReference fertilizerProductsRef;
   private final DatabaseReference journalEventsRef;
   private final DatabaseReference seasonOutcomesRef;
   private final DatabaseReference journalPhotoMetadataRef;
   private final DatabaseReference notificationsRef;
   private final DatabaseReference notificationSettingsRef;
   private final DatabaseReference pushTokensRef;

   public FirebaseRepository() {
      DatabaseReference zonesRef = this.deviceRef.child("zones");
      this.primaryZoneRef = zonesRef.child("zone-001");
      this.statusRef = this.deviceRef.child("status");
      this.commandsRef = this.deviceRef.child("commands");
      this.historyRef = this.deviceRef.child("watering_history");
      this.healthRef = this.deviceRef.child("health");
      this.statisticsRef = this.deviceRef.child("statistics");
      this.adaptiveRecommendationRef = this.deviceRef.child("adaptive_recommendation");
      this.aiDecisionRef = this.deviceRef.child("ai_decision");
      this.aiExplanationRef = this.deviceRef.child("ai_explanation");
      this.moisturePredictionRef = this.deviceRef.child("moisture_prediction");
      this.predictionAccuracyRef = this.deviceRef.child("prediction_accuracy");
      this.unifiedConfidenceRef = this.deviceRef.child("unified_confidence");
      this.soilLearningProfileRef = this.deviceRef.child("soil_learning_profile");
      this.predictionValidationRef = this.deviceRef.child("ai").child("prediction_validation");
      this.zonesRef = zonesRef;
      this.fertilizerProductsRef = this.deviceRef.child("fertilizer_products");
      this.journalEventsRef = this.deviceRef.child("garden_journal").child("events");
      this.seasonOutcomesRef = this.deviceRef.child("garden_journal").child("season_outcomes");
      this.journalPhotoMetadataRef = this.deviceRef.child("garden_journal").child("photo_metadata");
      this.notificationsRef = this.deviceRef.child("notifications");
      this.notificationSettingsRef = this.deviceRef.child("notification_settings");
      this.pushTokensRef = this.deviceRef.child("push_tokens");
   }

   public DatabaseReference getStatusRef() {
      return this.statusRef;
   }

   public DatabaseReference getCommandsRef() {
      return this.commandsRef;
   }

   public DatabaseReference getHistoryRef() {
      return this.historyRef;
   }

   public DatabaseReference getHealthRef() {
      return this.healthRef;
   }

   public DatabaseReference getStatisticsRef() {
      return this.statisticsRef;
   }

   public DatabaseReference getAdaptiveRecommendationRef() {
      return this.adaptiveRecommendationRef;
   }

   public DatabaseReference getAIDecisionRef() {
      return this.aiDecisionRef;
   }

   public DatabaseReference getAIExplanationRef() {
      return this.aiExplanationRef;
   }

   public void observeSensor(ValueEventListener listener) {
      this.primaryZoneRef.addValueEventListener(listener);
   }

   public LiveData<List<GardenZone>> observeGardenZones() {
      final MutableLiveData<List<GardenZone>> liveData = new MutableLiveData();
      this.zonesRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<GardenZone> zones = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               GardenZone zone = (GardenZone)child.getValue(GardenZone.class);
               if (zone != null) {
                  if (zone.getZone_id() == null || zone.getZone_id().isBlank()) {
                     zone.setZone_id(child.getKey());
                  }

                  zones.add(zone);
               }
            }

            zones.sort(Comparator.comparingInt(GardenZone::getOrder));
            liveData.setValue(zones);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Garden zones read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<GardenZone> observeGardenZone(final String zoneId) {
      final MutableLiveData<GardenZone> liveData = new MutableLiveData();
      this.zonesRef.child(zoneId).addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            GardenZone zone = (GardenZone)snapshot.getValue(GardenZone.class);
            if (zone != null) {
               zone.setZone_id(zoneId);
            }

            liveData.setValue(zone);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Garden zone read failed", error.toException());
         }
      });
      return liveData;
   }

   public Task<Void> updateGardenZoneSettings(String zoneId, boolean irrigationEnabled, int moistureLimit, int pumpDuration, int cooldownSeconds, int restartDelta, boolean sensorEnabled, int sensorDryRaw, int sensorWetRaw) {
      Map<String, Object> updates = new HashMap();
      updates.put("irrigation_enabled", irrigationEnabled);
      updates.put("moisture_limit", moistureLimit);
      updates.put("pump_duration", pumpDuration);
      updates.put("cooldown_seconds", cooldownSeconds);
      updates.put("restart_delta", restartDelta);
      updates.put("sensor_enabled", sensorEnabled);
      updates.put("sensor_calibration_dry_raw", sensorDryRaw);
      updates.put("sensor_calibration_wet_raw", sensorWetRaw);
      updates.put("sensor_config_updated_at_epoch", ServerValue.TIMESTAMP);
      return this.zonesRef.child(zoneId).updateChildren(updates);
   }

   public Task<Void> saveWeatherLocation(String city, String district) {
      return this.saveWeatherLocation(city, district, (Double)null, (Double)null, "auto");
   }

   public Task<Void> saveWeatherLocation(String city, String district, Double latitude, Double longitude) {
      return this.saveWeatherLocation(city, district, latitude, longitude, "auto");
   }

   public Task<Void> saveWeatherLocation(String city, String district, Double latitude, Double longitude, String forecastSource) {
      Map<String, Object> values = new HashMap();
      values.put("weather/location/city", city.trim());
      values.put("weather/location/district", district.trim());
      values.put("weather/location/latitude", latitude);
      values.put("weather/location/longitude", longitude);
      values.put("weather/location/forecast_source", forecastSource == null ? "auto" : forecastSource);
      values.put("weather/location/updated_at_epoch", System.currentTimeMillis() / 1000L);
      return this.deviceRef.updateChildren(values);
   }

   public LiveData<WeatherLocation> observeWeatherLocation() {
      final MutableLiveData<WeatherLocation> liveData = new MutableLiveData();
      this.deviceRef.child("weather").child("location").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            liveData.setValue(new WeatherLocation((String)snapshot.child("city").getValue(String.class), (String)snapshot.child("district").getValue(String.class), (Double)snapshot.child("latitude").getValue(Double.class), (Double)snapshot.child("longitude").getValue(Double.class), (String)snapshot.child("forecast_source").getValue(String.class)));
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.w("FirebaseRepository", "Weather location could not be read", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<WeatherForecast> observeWeatherForecast() {
      final MutableLiveData<WeatherForecast> liveData = new MutableLiveData();
      this.deviceRef.child("weather").child("forecast").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (!snapshot.exists()) {
               liveData.setValue(null);
            } else {
               List<WeatherDay> days = new ArrayList();

               for(DataSnapshot day : snapshot.child("days").getChildren()) {
                  days.add(new WeatherDay((String)day.child("date").getValue(String.class), (Double)day.child("temperature_max").getValue(Double.class), (Double)day.child("temperature_min").getValue(Double.class), (Double)day.child("rain_probability").getValue(Double.class), (Double)day.child("rain_mm").getValue(Double.class), (Double)day.child("wind_max").getValue(Double.class)));
               }

               WeatherForecast weatherForecast = new WeatherForecast((String)snapshot.child("city").getValue(String.class), (String)snapshot.child("district").getValue(String.class), (Double)snapshot.child("today_temperature_max").getValue(Double.class), (Double)snapshot.child("today_rain_probability").getValue(Double.class), (Double)snapshot.child("today_rain_mm").getValue(Double.class), (Double)snapshot.child("today_wind_max").getValue(Double.class), (Double)snapshot.child("tomorrow_temperature_max").getValue(Double.class), (Double)snapshot.child("tomorrow_rain_probability").getValue(Double.class), (Double)snapshot.child("tomorrow_rain_mm").getValue(Double.class), (Double)snapshot.child("tomorrow_wind_max").getValue(Double.class), days, (Long)snapshot.child("today_weather_code").getValue(Long.class), (Long)snapshot.child("tomorrow_weather_code").getValue(Long.class), (Double)snapshot.child("current_temperature").getValue(Double.class), (Double)snapshot.child("current_humidity").getValue(Double.class), (Double)snapshot.child("current_wind").getValue(Double.class), (Double)snapshot.child("current_pressure").getValue(Double.class), (Long)snapshot.child("current_weather_code").getValue(Long.class));
               weatherForecast.setSource((String)snapshot.child("source").getValue(String.class));
               weatherForecast.setUpdatedAtEpoch(Math.round(
                       snapshotNumber(snapshot.child("updated_at_epoch"), 0d)));
               liveData.setValue(weatherForecast);
            }
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.w("FirebaseRepository", "Weather forecast could not be read", error.toException());
         }
      });
      return liveData;
   }

   public Task<Void> saveRainSettings(RainSettings settings) {
      Map<String, Object> values = new HashMap<>();
      values.put("weather/irrigation_settings/rain_delay_enabled", settings.isRainDelayEnabled());
      values.put("weather/irrigation_settings/rain_probability_threshold", settings.getRainProbability());
      values.put("weather/irrigation_settings/rain_mm_threshold", settings.getRainMm());
      values.put("weather/irrigation_settings/updated_at_epoch", System.currentTimeMillis() / 1000L);
      return this.deviceRef.updateChildren(values);
   }

   public LiveData<RainSettings> observeRainSettings() {
      MutableLiveData<RainSettings> liveData = new MutableLiveData<>();
      this.deviceRef.child("weather").child("irrigation_settings")
            .addValueEventListener(new ValueEventListener() {
               @Override
               public void onDataChange(@NonNull DataSnapshot snapshot) {
                  if (!snapshot.exists()) {
                     liveData.setValue(RainSettings.defaults());
                     return;
                  }
                  Boolean enabled = snapshot.child("rain_delay_enabled").getValue(Boolean.class);
                  liveData.setValue(new RainSettings(
                        enabled == null || enabled,
                        snapshotNumber(snapshot.child("rain_probability_threshold"), RainSettings.DEFAULT_RAIN_PROBABILITY),
                        snapshotNumber(snapshot.child("rain_mm_threshold"), RainSettings.DEFAULT_RAIN_MM),
                        Math.round(snapshotNumber(snapshot.child("updated_at_epoch"), 0d))));
               }

               @Override
               public void onCancelled(@NonNull DatabaseError error) {
                  Log.w(TAG, "Rain settings could not be read", error.toException());
               }
            });
      return liveData;
   }

   private static double snapshotNumber(DataSnapshot snapshot, double fallback) {
      Object value = snapshot.getValue();
      return value instanceof Number ? ((Number) value).doubleValue() : fallback;
   }
   public Task<Void> saveGlobalSettingsAndSyncZones(long moistureLimit, long pumpDuration, long cooldownSeconds, long restartDelta, boolean enabled, boolean autoMode) {
      return this.zonesRef.get().continueWithTask((task) -> {
         if (task.isSuccessful() && task.getResult() != null) {
            Map<String, Object> updates = new HashMap();
            updates.put("commands/moisture_limit", moistureLimit);
            updates.put("commands/pump_duration", pumpDuration);
            updates.put("commands/cooldown_seconds", cooldownSeconds);
            updates.put("commands/restart_delta", restartDelta);
            updates.put("commands/enabled", enabled);
            updates.put("commands/auto_mode", autoMode);

            for(DataSnapshot zoneSnapshot : ((DataSnapshot)task.getResult()).getChildren()) {
               String zoneId = zoneSnapshot.getKey();
               if (zoneId != null && !zoneId.isBlank()) {
                  String path = "zones/" + zoneId + "/";
                  updates.put(path + "moisture_limit", moistureLimit);
                  updates.put(path + "pump_duration", pumpDuration);
                  updates.put(path + "cooldown_seconds", cooldownSeconds);
                  updates.put(path + "restart_delta", restartDelta);
               }
            }

            return this.deviceRef.updateChildren(updates);
         } else {
            Exception error = task.getException();
            return Tasks.forException((Exception)(error == null ? new IllegalStateException("Bölgeler okunamadı.") : error));
         }
      });
   }

   public Task<Void> updateGardenZoneValveMode(String zoneId, boolean physical) {
      Map<String, Object> updates = new HashMap();
      updates.put("valve_mode", physical ? "PHYSICAL" : "SIMULATION");
      updates.put("valve_mode_updated_at_epoch", ServerValue.TIMESTAMP);
      return this.zonesRef.child(zoneId).updateChildren(updates);
   }

   public Task<Void> createGardenZone(GardenZone zone) {
      if (zone.getZone_id() != null && !zone.getZone_id().isBlank()) {
         long now = System.currentTimeMillis() / 1000L;
         Map<String, Object> updates = new HashMap();
         String path = "zones/" + zone.getZone_id() + "/";
         updates.put(path + "zone_id", zone.getZone_id());
         updates.put(path + "name", zone.getName());
         updates.put(path + "plant_type", zone.getPlant_type());
         updates.put(path + "emoji", zone.getEmoji());
         updates.put(path + "sensor_id", zone.getSensor_id());
         updates.put(path + "valve_id", zone.getValve_id());
         updates.put(path + "enabled", zone.isEnabled());
         updates.put(path + "irrigation_enabled", zone.isIrrigation_enabled());
         updates.put(path + "moisture_limit", zone.getMoisture_limit());
         updates.put(path + "pump_duration", zone.getPump_duration());
         updates.put(path + "cooldown_seconds", zone.getCooldown_seconds());
         updates.put(path + "restart_delta", zone.getRestart_delta());
         updates.put(path + "order", zone.getOrder());
         updates.put(path + "updated_at_epoch", now);
         return this.deviceRef.updateChildren(updates);
      } else {
         return Tasks.forException(new IllegalArgumentException("Bölge kimliği gerekli."));
      }
   }

   public Task<Void> updateFertilizationWaterAnalysis(String zoneId, double ph, double ecMs) {
      String path = "zones/" + zoneId + "/fertilization/";
      Map<String, Object> updates = new HashMap();
      updates.put(path + "water_ph", ph);
      updates.put(path + "water_ec_ms", ecMs);
      updates.put(path + "water_analysis_updated_at_epoch", System.currentTimeMillis() / 1000L);
      return this.deviceRef.updateChildren(updates);
   }

   public Task<Void> updateFertilizationProfile(String zoneId, boolean enabled, String plantingDate, String growthStage, boolean reminderEnabled, String productId, int intervalDays, long nextApplicationEpoch, double areaM2, double tankLiters) {
      Map<String, Object> updates = new HashMap();
      String profilePath = "zones/" + zoneId + "/fertilization/";
      String planId = "plan-" + zoneId;
      long updatedAt = System.currentTimeMillis() / 1000L;
      updates.put(profilePath + "enabled", enabled);
      updates.put(profilePath + "planting_date", plantingDate);
      updates.put(profilePath + "growth_stage", growthStage);
      updates.put(profilePath + "reminder_enabled", reminderEnabled);
      updates.put(profilePath + "active_product_id", productId);
      updates.put(profilePath + "area_m2", areaM2);
      updates.put(profilePath + "tank_liters", tankLiters);
      updates.put(profilePath + "active_plan_id", enabled ? planId : "");
      updates.put(profilePath + "next_application_at_epoch", enabled ? nextApplicationEpoch : 0L);
      updates.put(profilePath + "updated_at_epoch", updatedAt);
      String planPath = "fertilizer_plans/" + planId + "/";
      updates.put(planPath + "plan_id", planId);
      updates.put(planPath + "zone_id", zoneId);
      updates.put(planPath + "product_id", productId);
      updates.put(planPath + "interval_days", intervalDays);
      updates.put(planPath + "area_m2", areaM2);
      updates.put(planPath + "tank_liters", tankLiters);
      updates.put(planPath + "enabled", enabled);
      updates.put(planPath + "next_application_at_epoch", enabled ? nextApplicationEpoch : 0L);
      updates.put(planPath + "updated_at_epoch", updatedAt);
      return this.deviceRef.updateChildren(updates);
   }

   public LiveData<List<FertilizerProduct>> observeFertilizerProducts() {
      final MutableLiveData<List<FertilizerProduct>> liveData = new MutableLiveData();
      this.fertilizerProductsRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<FertilizerProduct> products = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               FertilizerProduct product = (FertilizerProduct)child.getValue(FertilizerProduct.class);
               if (product != null) {
                  if (product.getProduct_id() == null || product.getProduct_id().isBlank()) {
                     product.setProduct_id(child.getKey());
                  }

                  products.add(product);
               }
            }

            products.sort(Comparator.comparing((productx) -> productx.getName() == null ? "" : productx.getName(), String.CASE_INSENSITIVE_ORDER));
            liveData.setValue(products);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Fertilizer products read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<List<FertilizerRecommendation>> observeFertilizerRecommendations() {
      final MutableLiveData<List<FertilizerRecommendation>> liveData = new MutableLiveData();
      this.deviceRef.child("fertilization").child("recommendations").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<FertilizerRecommendation> values = new ArrayList();

            for(DataSnapshot plant : snapshot.getChildren()) {
               for(DataSnapshot stage : plant.getChildren()) {
                  for(DataSnapshot entry : stage.getChildren()) {
                     FertilizerRecommendation value = (FertilizerRecommendation)entry.getValue(FertilizerRecommendation.class);
                     if (value != null) {
                        value.setPlant_type(plant.getKey());
                        value.setGrowth_stage(stage.getKey());
                        value.setRecommendation_id(entry.getKey());
                        values.add(value);
                     }
                  }
               }
            }

            liveData.setValue(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Fertilizer recommendations read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<List<FertilizerStageGuide>> observeFertilizerStageGuides() {
      final MutableLiveData<List<FertilizerStageGuide>> liveData = new MutableLiveData();
      this.deviceRef.child("fertilization").child("stage_guides").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<FertilizerStageGuide> values = new ArrayList();

            for(DataSnapshot plant : snapshot.getChildren()) {
               for(DataSnapshot stage : plant.getChildren()) {
                  FertilizerStageGuide value = (FertilizerStageGuide)stage.getValue(FertilizerStageGuide.class);
                  if (value != null) {
                     value.setPlant_type(plant.getKey());
                     value.setGrowth_stage(stage.getKey());
                     values.add(value);
                  }
               }
            }

            liveData.setValue(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Fertilizer stage guides read failed", error.toException());
         }
      });
      return liveData;
   }

   public Task<Void> saveFertilizerProduct(FertilizerProduct product) {
      String productId = product.getProduct_id();
      if (productId == null || productId.isBlank()) {
         productId = "product-" + UUID.randomUUID();
         product.setProduct_id(productId);
      }

      product.setUpdated_at_epoch(System.currentTimeMillis() / 1000L);
      return this.fertilizerProductsRef.child(productId).setValue(product);
   }

   public Task<List<String>> findActiveZonesUsingFertilizer(String productId) {
      return this.zonesRef.get().continueWith((task) -> {
         if (!task.isSuccessful()) {
            throw (Exception)(task.getException() == null ? new IllegalStateException("Garden zones could not be read") : task.getException());
         } else {
            List<String> zoneNames = new ArrayList();

            for(DataSnapshot child : ((DataSnapshot)task.getResult()).getChildren()) {
               GardenZone zone = (GardenZone)child.getValue(GardenZone.class);
               if (zone != null && zone.getFertilization() != null && zone.getFertilization().isEnabled() && productId.equals(zone.getFertilization().getActive_product_id())) {
                  String name = zone.getName();
                  zoneNames.add(name != null && !name.isBlank() ? name : child.getKey());
               }
            }

            return zoneNames;
         }
      });
   }

   public Task<Void> deactivateFertilizerProduct(String productId) {
      Map<String, Object> updates = new HashMap();
      updates.put("enabled", false);
      updates.put("updated_at_epoch", System.currentTimeMillis() / 1000L);
      return this.fertilizerProductsRef.child(productId).updateChildren(updates);
   }

   public Task<Void> deleteFertilizerProduct(String productId) {
      return this.fertilizerProductsRef.child(productId).removeValue();
   }



   public LiveData<List<FertilizerApplication>> observeFertilizerHistory() {
      final MutableLiveData<List<FertilizerApplication>> liveData = new MutableLiveData();
      this.deviceRef.child("fertilizer_history").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<FertilizerApplication> values = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               FertilizerApplication value = (FertilizerApplication)child.getValue(FertilizerApplication.class);
               if (value != null) {
                  values.add(value);
               }
            }

            values.sort((left, right) -> Long.compare(right.getApplied_at_epoch(), left.getApplied_at_epoch()));
            liveData.setValue(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Fertilizer history read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<List<WateringHistory>> observeWateringHistory() {
      final MutableLiveData<List<WateringHistory>> liveData = new MutableLiveData();
      this.historyRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<WateringHistory> values = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               WateringHistory value = (WateringHistory)child.getValue(WateringHistory.class);
               if (value != null) {
                  value.setRecordId(child.getKey());
                  values.add(value);
               }
            }

            values.sort((left, right) -> right.getFinishedAt().compareTo(left.getFinishedAt()));
            liveData.setValue(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Watering history read failed", error.toException());
         }
      });
      return liveData;
   }



   public static final class BulkFertilizerApplication {
      private final String zoneId;
      private final String zoneName;
      private final double appliedDose;
      private final double areaM2;
      private final double tankLiters;
      private final double recommendedDoseMin;
      private final double recommendedDoseMax;
      private final String applicationMethod;
      private final String notes;
      private final long appliedAt;
      private final String applicationType;
      private final String mixGroupId;
      private final String mixPartnerProductId;
      private final String mixPartnerProductName;
      private final String mixRiskLevel;

      public BulkFertilizerApplication(String zoneId, String zoneName, double appliedDose, double areaM2, double tankLiters, double recommendedDoseMin, double recommendedDoseMax, String applicationMethod, String notes, long appliedAt, String applicationType) {
         this(zoneId, zoneName, appliedDose, areaM2, tankLiters, recommendedDoseMin, recommendedDoseMax, applicationMethod, notes, appliedAt, applicationType, "", "", "", "");
      }

      public BulkFertilizerApplication(String zoneId, String zoneName, double appliedDose, double areaM2, double tankLiters, double recommendedDoseMin, double recommendedDoseMax, String applicationMethod, String notes, long appliedAt, String applicationType, String mixGroupId, String mixPartnerProductId, String mixPartnerProductName, String mixRiskLevel) {
         this.zoneId = zoneId;
         this.zoneName = zoneName;
         this.appliedDose = appliedDose;
         this.areaM2 = areaM2;
         this.tankLiters = tankLiters;
         this.recommendedDoseMin = recommendedDoseMin;
         this.recommendedDoseMax = recommendedDoseMax;
         this.applicationMethod = applicationMethod;
         this.notes = notes;
         this.appliedAt = appliedAt;
         this.applicationType = applicationType;
         this.mixGroupId = mixGroupId == null ? "" : mixGroupId;
         this.mixPartnerProductId = mixPartnerProductId == null ? "" : mixPartnerProductId;
         this.mixPartnerProductName = mixPartnerProductName == null ? "" : mixPartnerProductName;
         this.mixRiskLevel = mixRiskLevel == null ? "" : mixRiskLevel;
      }
   }

   public static final class FertilizerApplicationBatch {
      private final FertilizerProduct product;
      private final List<BulkFertilizerApplication> applications;
      private final String appliedUnit;
      private final boolean deductStock;

      public FertilizerApplicationBatch(FertilizerProduct product, List<BulkFertilizerApplication> applications, String appliedUnit, boolean deductStock) {
         this.product = product;
         this.applications = applications;
         this.appliedUnit = appliedUnit;
         this.deductStock = deductStock;
      }
   }

   public Task<Void> recordFertilizerApplicationSafely(String zoneId, String zoneName, FertilizerProduct product, double appliedDose, String appliedUnit, double areaM2, double tankLiters, double recommendedDoseMin, double recommendedDoseMax, boolean deductStock, String applicationMethod, String notes, long appliedAt, String applicationType) {
      long effectiveAppliedAt = appliedAt > 0L ? appliedAt : System.currentTimeMillis() / 1000L;
      BulkFertilizerApplication application = new BulkFertilizerApplication(zoneId, zoneName, appliedDose, areaM2, tankLiters, recommendedDoseMin, recommendedDoseMax, applicationMethod, notes, effectiveAppliedAt, applicationType);
      List<BulkFertilizerApplication> applications = new ArrayList<>();
      applications.add(application);
      return recordBulkFertilizerApplicationsSafely(product, applications, appliedUnit, deductStock);
   }

   public Task<Void> recordBulkFertilizerApplicationsSafely(FertilizerProduct product, List<BulkFertilizerApplication> applications, String appliedUnit, boolean deductStock) {
      List<FertilizerApplicationBatch> batches = new ArrayList<>();
      batches.add(new FertilizerApplicationBatch(product, applications, appliedUnit, deductStock));
      return recordFertilizerApplicationBatchesSafely(batches);
   }

   public Task<Void> recordFertilizerApplicationBatchesSafely(List<FertilizerApplicationBatch> batches) {
      if (batches == null || batches.isEmpty()) {
         return Tasks.forException(new IllegalArgumentException("Kaydedilecek gübre uygulaması bulunamadı."));
      }
      final List<List<String>> applicationIds = new ArrayList<>();
      for (FertilizerApplicationBatch batch : batches) {
         if (batch == null || batch.product == null || batch.product.getProduct_id() == null || batch.product.getProduct_id().isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Gübre ürünü kimliği eksik."));
         }
         if (batch.applications == null || batch.applications.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Kaydedilecek bölge bulunamadı."));
         }
         List<String> batchIds = new ArrayList<>();
         for (BulkFertilizerApplication ignored : batch.applications) {
            batchIds.add("application-" + UUID.randomUUID());
         }
         applicationIds.add(batchIds);
      }
      return runAtomicDeviceUpdate("Gübre uygulaması kaydedilemedi.", root -> {
         long recordedAt = System.currentTimeMillis() / 1000L;
         for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            FertilizerApplicationBatch batch = batches.get(batchIndex);
            double totalDose = 0.0;
            for (BulkFertilizerApplication application : batch.applications) {
               if (application.appliedDose <= 0.0) {
                  throw new IllegalStateException("Uygulama miktarı sıfırdan büyük olmalıdır.");
               }
               totalDose += application.appliedDose;
            }
            if (batch.deductStock) {
               changeStock(root, batch.product.getProduct_id(), batch.appliedUnit, totalDose, recordedAt);
            }
            for (int index = 0; index < batch.applications.size(); index++) {
               writeFertilizerApplication(root, applicationIds.get(batchIndex).get(index), batch.applications.get(index), batch.product, batch.appliedUnit, batch.deductStock, recordedAt);
            }
         }
      });
   }
   public Task<Void> updateFertilizerApplicationSafely(FertilizerApplication value) {
      if (value == null || value.getApplication_id() == null || value.getApplication_id().isBlank()) {
         return Tasks.forException(new IllegalArgumentException("Gübre uygulama kaydı bulunamadı."));
      }
      return runAtomicDeviceUpdate("Gübre uygulama kaydı güncellenemedi.", root -> {
         MutableData history = root.child("fertilizer_history").child(value.getApplication_id());
         if (history.getValue() == null) {
            throw new IllegalStateException("Düzenlenecek gübre uygulama kaydı artık mevcut değil.");
         }
         String oldZoneId = stringValue(history.child("zone_id"));
         String oldType = normalizedApplicationType(stringValue(history.child("application_type")));
         String oldProductId = stringValue(history.child("product_id"));
         String oldUnit = stringValue(history.child("dose_unit"));
         double oldDose = numberValue(history.child("applied_dose"));
         boolean stockDeducted = booleanValue(history.child("stock_deducted"));
         long recordedAt = System.currentTimeMillis() / 1000L;
         if (stockDeducted) {
            double difference = value.getApplied_dose() - oldDose;
            if (Math.abs(difference) > 0.000001) {
               changeStock(root, oldProductId, oldUnit, difference, recordedAt);
            }
         }
         history.child("applied_dose").setValue(value.getApplied_dose());
         history.child("applied_at_epoch").setValue(value.getApplied_at_epoch());
         history.child("next_application_at_epoch").setValue(value.getNext_application_at_epoch());
         history.child("application_method").setValue(value.getApplication_method());
         history.child("notes").setValue(value.getNotes());
         history.child("outcome_follow_up_due_at_epoch").setValue(
               value.getApplied_at_epoch() + FertilizerOutcomeFollowUpPolicy.FOLLOW_UP_DELAY_SECONDS
         );
         history.child("outcome_observed_at_epoch").setValue(value.getOutcome_observed_at_epoch());
         history.child("outcome_status").setValue(value.getOutcome_status());
         history.child("outcome_vigor_score").setValue(value.getOutcome_vigor_score());
         history.child("outcome_notes").setValue(value.getOutcome_notes());
         history.child("updated_at_epoch").setValue(recordedAt);
         recalculateApplicationSchedule(root, oldZoneId, oldType, recordedAt);
      });
   }

   public Task<Void> deleteFertilizerApplicationSafely(FertilizerApplication target) {
      if (target == null || target.getApplication_id() == null || target.getApplication_id().isBlank()) {
         return Tasks.forException(new IllegalArgumentException("Silinecek gübre uygulama kaydı bulunamadı."));
      }
      return runAtomicDeviceUpdate("Gübre uygulama kaydı silinemedi.", root -> {
         MutableData history = root.child("fertilizer_history").child(target.getApplication_id());
         if (history.getValue() == null) {
            throw new IllegalStateException("Silinecek gübre uygulama kaydı artık mevcut değil.");
         }
         String zoneId = stringValue(history.child("zone_id"));
         String type = normalizedApplicationType(stringValue(history.child("application_type")));
         long recordedAt = System.currentTimeMillis() / 1000L;
         if (booleanValue(history.child("stock_deducted"))) {
            changeStock(root, stringValue(history.child("product_id")), stringValue(history.child("dose_unit")), -numberValue(history.child("applied_dose")), recordedAt);
         }
         history.setValue(null);
         recalculateApplicationSchedule(root, zoneId, type, recordedAt);
      });
   }

   private void writeFertilizerApplication(MutableData root, String applicationId, BulkFertilizerApplication application, FertilizerProduct product, String appliedUnit, boolean stockDeducted, long recordedAt) {
      String type = normalizedApplicationType(application.applicationType);
      int intervalDays = Math.max(0, product.getMinimum_interval_days());
      long nextAt = intervalDays == 0 ? 0L
            : application.appliedAt + (long)intervalDays * 86400L;
      MutableData history = root.child("fertilizer_history").child(applicationId);
      history.child("application_id").setValue(applicationId);
      history.child("zone_id").setValue(application.zoneId);
      history.child("zone_name").setValue(application.zoneName);
      history.child("product_id").setValue(product.getProduct_id());
      history.child("product_name").setValue(product.getName());
      history.child("applied_dose").setValue(application.appliedDose);
      history.child("dose_unit").setValue(appliedUnit);
      history.child("area_m2").setValue(application.areaM2);
      history.child("tank_liters").setValue(application.tankLiters);
      history.child("recommended_dose_min").setValue(application.recommendedDoseMin);
      history.child("recommended_dose_max").setValue(application.recommendedDoseMax);
      history.child("applied_at_epoch").setValue(application.appliedAt);
      history.child("next_application_at_epoch").setValue(nextAt);
      history.child("outcome_follow_up_due_at_epoch").setValue(
            application.appliedAt + FertilizerOutcomeFollowUpPolicy.FOLLOW_UP_DELAY_SECONDS
      );
      history.child("source").setValue("MANUAL");
      history.child("application_type").setValue(type);
      history.child("application_method").setValue(application.applicationMethod);
      history.child("notes").setValue(application.notes);
      history.child("stock_deducted").setValue(stockDeducted);
      history.child("recorded_at_epoch").setValue(recordedAt);
      if (!application.mixGroupId.isBlank()) {
         history.child("mix_group_id").setValue(application.mixGroupId);
         history.child("mix_partner_product_id").setValue(application.mixPartnerProductId);
         history.child("mix_partner_product_name").setValue(application.mixPartnerProductName);
         history.child("mix_risk_level").setValue(application.mixRiskLevel);
      }
      updateScheduleIfNewer(root, history, recordedAt);
   }

   private void updateScheduleIfNewer(MutableData root, MutableData history, long recordedAt) {
      String zoneId = stringValue(history.child("zone_id"));
      String type = normalizedApplicationType(stringValue(history.child("application_type")));
      long appliedAt = longValue(history.child("applied_at_epoch"));
      MutableData schedule = root.child("zones").child(zoneId).child("fertilization").child("application_schedules").child(type);
      long scheduledAt = longValue(schedule.child("last_application_at_epoch"));
      long scheduledNextAt = longValue(schedule.child("next_application_at_epoch"));
      long candidateNextAt = longValue(history.child("next_application_at_epoch"));
      if (appliedAt < scheduledAt
            || (appliedAt == scheduledAt && candidateNextAt < scheduledNextAt)) {
         return;
      }
      copyHistoryToSchedule(schedule, history, recordedAt);
      if ("NUTRITION".equals(type)) {
         updateNutritionPointers(root, zoneId, history, recordedAt);
      }
   }

   private void recalculateApplicationSchedule(MutableData root, String zoneId, String type, long recordedAt) {
      MutableData latest = null;
      long latestAt = Long.MIN_VALUE;
      for (MutableData candidate : root.child("fertilizer_history").getChildren()) {
         if (!zoneId.equals(stringValue(candidate.child("zone_id")))) {
            continue;
         }
         if (!type.equals(normalizedApplicationType(stringValue(candidate.child("application_type"))))) {
            continue;
         }
         long candidateAt = longValue(candidate.child("applied_at_epoch"));
         if (candidateAt > latestAt) {
            latest = candidate;
            latestAt = candidateAt;
         }
      }
      MutableData schedule = root.child("zones").child(zoneId).child("fertilization").child("application_schedules").child(type);
      if (latest == null) {
         schedule.setValue(null);
         if ("NUTRITION".equals(type)) {
            clearNutritionPointers(root, zoneId, recordedAt);
         }
         return;
      }
      copyHistoryToSchedule(schedule, latest, recordedAt);
      if ("NUTRITION".equals(type)) {
         updateNutritionPointers(root, zoneId, latest, recordedAt);
      }
   }

   private void copyHistoryToSchedule(MutableData schedule, MutableData history, long recordedAt) {
      long appliedAt = longValue(history.child("applied_at_epoch"));
      long nextAt = longValue(history.child("next_application_at_epoch"));
      schedule.child("product_id").setValue(stringValue(history.child("product_id")));
      schedule.child("product_name").setValue(stringValue(history.child("product_name")));
      schedule.child("last_application_at_epoch").setValue(appliedAt);
      schedule.child("next_application_at_epoch").setValue(nextAt);
      schedule.child("interval_days").setValue(nextAt <= 0L ? 0L
            : Math.max(0L, (nextAt - appliedAt) / 86400L));
      schedule.child("updated_at_epoch").setValue(recordedAt);
   }

   private void updateNutritionPointers(MutableData root, String zoneId, MutableData history, long recordedAt) {
      long appliedAt = longValue(history.child("applied_at_epoch"));
      long nextAt = longValue(history.child("next_application_at_epoch"));
      MutableData profile = root.child("zones").child(zoneId).child("fertilization");
      profile.child("last_application_at_epoch").setValue(appliedAt);
      profile.child("next_application_at_epoch").setValue(nextAt);
      profile.child("updated_at_epoch").setValue(recordedAt);
      MutableData plan = root.child("fertilizer_plans").child("plan-" + zoneId);
      plan.child("last_application_at_epoch").setValue(appliedAt);
      plan.child("next_application_at_epoch").setValue(nextAt);
      plan.child("updated_at_epoch").setValue(recordedAt);
   }

   private void clearNutritionPointers(MutableData root, String zoneId, long recordedAt) {
      MutableData profile = root.child("zones").child(zoneId).child("fertilization");
      profile.child("last_application_at_epoch").setValue(0L);
      profile.child("next_application_at_epoch").setValue(0L);
      profile.child("updated_at_epoch").setValue(recordedAt);
      MutableData plan = root.child("fertilizer_plans").child("plan-" + zoneId);
      plan.child("last_application_at_epoch").setValue(0L);
      plan.child("next_application_at_epoch").setValue(0L);
      plan.child("updated_at_epoch").setValue(recordedAt);
   }

   private void changeStock(MutableData root, String productId, String appliedUnit, double amountToDeduct, long recordedAt) {
      if (productId == null || productId.isBlank()) {
         throw new IllegalStateException("Stok güncellemesi için ürün kimliği eksik.");
      }
      MutableData product = root.child("fertilizer_products").child(productId);
      if (product.getValue() == null) {
         throw new IllegalStateException("Stok güncellenecek gübre ürünü bulunamadı.");
      }
      String stockUnit = stringValue(product.child("stock_unit"));
      if (stockUnit.isBlank() || appliedUnit == null || !stockUnit.equalsIgnoreCase(appliedUnit)) {
         throw new IllegalStateException("Gübre stok birimi uygulama birimiyle uyuşmuyor.");
      }
      double currentStock = numberValue(product.child("stock_amount"));
      double updatedStock = currentStock - amountToDeduct;
      if (updatedStock < -0.000001) {
         throw new IllegalStateException("Gübre stoğu bu uygulama için yetersiz.");
      }
      product.child("stock_amount").setValue(Math.max(0.0, updatedStock));
      product.child("updated_at_epoch").setValue(recordedAt);
   }

   private Task<Void> runAtomicDeviceUpdate(String fallbackMessage, DeviceMutation mutation) {
      TaskCompletionSource<Void> completion = new TaskCompletionSource<>();
      AtomicReference<String> failure = new AtomicReference<>(fallbackMessage);
      this.deviceRef.runTransaction(new Transaction.Handler() {
         @NonNull
         @Override
         public Transaction.Result doTransaction(@NonNull MutableData currentData) {
            try {
               mutation.apply(currentData);
               return Transaction.success(currentData);
            } catch (RuntimeException error) {
               if (error.getMessage() != null && !error.getMessage().isBlank()) {
                  failure.set(error.getMessage());
               }
               return Transaction.abort();
            }
         }

         @Override
         public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
            if (error != null) {
               completion.setException(error.toException());
            } else if (!committed) {
               completion.setException(new IllegalStateException(failure.get()));
            } else {
               completion.setResult(null);
            }
         }
      });
      return completion.getTask();
   }

   private static String normalizedApplicationType(String value) {
      return value == null || value.isBlank()
            ? "NUTRITION"
            : value.trim().toUpperCase(Locale.ROOT);
   }

   private static String stringValue(MutableData data) {
      Object value = data.getValue();
      return value == null ? "" : String.valueOf(value);
   }

   private static double numberValue(MutableData data) {
      Object value = data.getValue();
      return value instanceof Number ? ((Number)value).doubleValue() : 0.0;
   }

   private static long longValue(MutableData data) {
      Object value = data.getValue();
      return value instanceof Number ? ((Number)value).longValue() : 0L;
   }

   private static boolean booleanValue(MutableData data) {
      Object value = data.getValue();
      return value instanceof Boolean && (Boolean)value;
   }

   @FunctionalInterface
   private interface DeviceMutation {
      void apply(MutableData root);
   }
   public Task<Void> requestZoneValveTest(GardenZone zone, int durationSeconds) {
      Map<String, Object> command = new HashMap();
      command.put("requested", true);
      command.put("request_id", UUID.randomUUID().toString());
      command.put("zone_id", zone.getZone_id());
      command.put("valve_id", zone.getValve_id());
      command.put("duration", Math.max(1, Math.min(10800, durationSeconds)));
      command.put("cancel_requested", false);
      command.put("requested_at", ServerValue.TIMESTAMP);
      return this.commandsRef.child("zone_test").setValue(command);
   }

   public Task<Void> cancelZoneValveTest() {
      return this.commandsRef.child("zone_test").child("cancel_requested").setValue(true);
   }

   public void observeStatus(ValueEventListener listener) {
      this.statusRef.addValueEventListener(listener);
   }

   public void removeStatusObserver(ValueEventListener listener) {
      this.statusRef.removeEventListener(listener);
   }

   public void observeCommands(ValueEventListener listener) {
      this.commandsRef.addValueEventListener(listener);
   }

   public void observeHealth(ValueEventListener listener) {
      this.healthRef.addValueEventListener(listener);
   }

   public void observeStatistics(ValueEventListener listener) {
      this.statisticsRef.addValueEventListener(listener);
   }

   public void observeAdaptiveRecommendation(final Consumer<AdaptiveRecommendation> consumer) {
      this.adaptiveRecommendationRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            AdaptiveRecommendation recommendation = (AdaptiveRecommendation)snapshot.getValue(AdaptiveRecommendation.class);
            if (recommendation != null) {
               consumer.accept(recommendation);
            }

         }

         public void onCancelled(@NonNull DatabaseError error) {
         }
      });
   }

   public void observeAIDecision(ValueEventListener listener) {
      this.aiDecisionRef.addValueEventListener(listener);
   }

   public void observeAIExplanation(ValueEventListener listener) {
      this.aiExplanationRef.addValueEventListener(listener);
   }

   public void setRelay(boolean value) {
      Map<String, Object> updates = new HashMap();
      updates.put("relay", value);
      updates.put("relay_requested_at", ServerValue.TIMESTAMP);
      this.commandsRef.updateChildren(updates);
   }

   public void setAutoMode(boolean value) {
      this.commandsRef.child("auto_mode").setValue(value);
   }

   public void restartDevice() {
      this.commandsRef.child("restart_device").setValue(true);
   }

   public void startManualWatering() {
      Map<String, Object> updates = new HashMap();
      updates.put("auto_mode", false);
      updates.put("relay", true);
      updates.put("relay_requested_at", ServerValue.TIMESTAMP);
      this.commandsRef.updateChildren(updates);
   }

   public void stopManualWatering() {
      this.setRelay(false);
   }

   public LiveData<PredictionValidationStatus> observePredictionValidationStatus() {
      final MutableLiveData<PredictionValidationStatus> liveData = new MutableLiveData();
      this.predictionValidationRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            PredictionValidationStatus status = (PredictionValidationStatus)snapshot.getValue(PredictionValidationStatus.class);
            liveData.setValue(status);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Prediction validation observation failed: " + error.getMessage());
         }
      });
      return liveData;
   }

   public LiveData<MoisturePrediction> observeMoisturePrediction() {
      final MutableLiveData<MoisturePrediction> liveData = new MutableLiveData();
      this.moisturePredictionRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            MoisturePrediction value = (MoisturePrediction)snapshot.getValue(MoisturePrediction.class);
            liveData.setValue(value);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Moisture prediction read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<PredictionAccuracy> observePredictionAccuracy() {
      final MutableLiveData<PredictionAccuracy> liveData = new MutableLiveData();
      this.predictionAccuracyRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            PredictionAccuracy value = (PredictionAccuracy)snapshot.getValue(PredictionAccuracy.class);
            liveData.setValue(value);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Prediction accuracy read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<SoilLearningProfile> observeSoilLearningProfile() {
      final MutableLiveData<SoilLearningProfile> liveData = new MutableLiveData();
      this.soilLearningProfileRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            SoilLearningProfile profile = (SoilLearningProfile)snapshot.getValue(SoilLearningProfile.class);
            liveData.setValue(profile);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Soil learning profile read failed", error.toException());
         }
      });
      return liveData;
   }

   public LiveData<UnifiedConfidence> observeUnifiedConfidence() {
      final MutableLiveData<UnifiedConfidence> liveData = new MutableLiveData();
      this.unifiedConfidenceRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            UnifiedConfidence value = (UnifiedConfidence)snapshot.getValue(UnifiedConfidence.class);
            liveData.setValue(value);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.e("FirebaseRepository", "Unified confidence read failed", error.toException());
         }
      });
      return liveData;
   }

   public Task<Void> saveGardenEvent(GardenEvent event) {
      return event != null && !event.getId().isBlank() ? this.journalEventsRef.child(event.getId()).setValue(event) : Tasks.forException(new IllegalArgumentException("Journal event id is required"));
   }

   public Task<Void> deleteGardenEvent(String eventId) {
      return eventId != null && !eventId.isBlank() ? this.journalEventsRef.child(eventId).removeValue() : Tasks.forResult(null);
   }

   public Task<Void> saveSeasonOutcome(SeasonOutcome outcome) {
      return outcome != null && !outcome.getId().isBlank() ? this.seasonOutcomesRef.child(outcome.getId()).setValue(outcome) : Tasks.forException(new IllegalArgumentException("Season outcome id is required"));
   }

   public Task<Void> saveGardenPhotoMetadata(GardenPhoto photo) {
      if (photo != null && photo.getId() != null && !photo.getId().isBlank()) {
         Map<String, Object> values = new HashMap();
         values.put("id", photo.getId());
         values.put("zone_id", photo.getZone_id());
         values.put("note", photo.getNote() == null ? "" : photo.getNote());
         values.put("related_application_id", photo.getRelated_application_id() == null ? "" : photo.getRelated_application_id());
         values.put("analysis_title", photo.getAnalysis_title() == null ? "" : photo.getAnalysis_title());
         values.put("analysis_meta", photo.getAnalysis_meta() == null ? "" : photo.getAnalysis_meta());
         values.put("analysis_context", photo.getAnalysis_context() == null ? "" : photo.getAnalysis_context());
         values.put("analysis_advice", photo.getAnalysis_advice() == null ? "" : photo.getAnalysis_advice());
         values.put("captured_at_epoch", photo.getCaptured_at_epoch());
         values.put("photo_kept_on_owner_phone", true);
         values.put("metadata_updated_at_epoch", System.currentTimeMillis() / 1000L);
         return this.journalPhotoMetadataRef.child(photo.getId()).setValue(values);
      } else {
         return Tasks.forException(new IllegalArgumentException("Photo id is required"));
      }
   }

   public Task<Void> deleteGardenPhotoMetadata(String photoId) {
      return photoId != null && !photoId.isBlank() ? this.journalPhotoMetadataRef.child(photoId).removeValue() : Tasks.forException(new IllegalArgumentException("Photo id is required"));
   }

   public Task<Void> saveGardenNotification(GardenNotification notification) {
      return notification != null && !notification.getId().isBlank() ? this.notificationsRef.child(notification.getId()).setValue(notification) : Tasks.forException(new IllegalArgumentException("Notification id is required"));
   }

   public Task<Void> deleteGardenNotifications(List<String> ids) {
      if (ids == null || ids.isEmpty()) {
         return Tasks.forResult(null);
      }

      Map<String, Object> updates = new HashMap<>();

      for (String id : ids) {
         if (id != null && !id.isBlank()) {
            updates.put(id, null);
         }
      }

      if (updates.isEmpty()) {
         return Tasks.forResult(null);
      }

      return this.notificationsRef.updateChildren(updates);
   }
   public Task<Void> updateGardenNotificationState(String id, boolean read, boolean saved) {
      if (id != null && !id.isBlank()) {
         Map<String, Object> values = new HashMap();
         values.put("read", read);
         values.put("saved", saved);
         return this.notificationsRef.child(id).updateChildren(values);
      } else {
         return Tasks.forResult(null);
      }
   }

   public LiveData<List<GardenNotification>> observeGardenNotifications() {
      final MutableLiveData<List<GardenNotification>> live = new MutableLiveData();
      this.notificationsRef.addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<GardenNotification> values = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               GardenNotification value = (GardenNotification)child.getValue(GardenNotification.class);
               if (value != null) {
                  values.add(value);
               }
            }

            values.sort((a, b) -> Long.compare(b.getCreated_at_epoch(), a.getCreated_at_epoch()));
            live.postValue(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
         }
      });
      return live;
   }

   public void loadGardenNotifications(final Consumer<List<GardenNotification>> consumer) {
      this.notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<GardenNotification> values = new ArrayList();

            for(DataSnapshot child : snapshot.getChildren()) {
               GardenNotification value = (GardenNotification)child.getValue(GardenNotification.class);
               if (value != null && !value.getId().isBlank()) {
                  values.add(value);
               }
            }

            values.sort((a, b) -> Long.compare(b.getCreated_at_epoch(), a.getCreated_at_epoch()));
            consumer.accept(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            consumer.accept(new ArrayList());
         }
      });
   }

   public Task<Void> saveNotificationSettings(Map<String, Object> values) {
      return values != null && !values.isEmpty() ? this.notificationSettingsRef.setValue(values) : Tasks.forResult(null);
   }

   public void loadNotificationSettings(final Consumer<Map<String, Object>> consumer) {
      this.notificationSettingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            Map<String, Object> values = new HashMap();

            for(DataSnapshot child : snapshot.getChildren()) {
               values.put(child.getKey(), child.getValue());
            }

            consumer.accept(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
         }
      });
   }

   public Task<Void> saveFertilizationPreferences(Map<String, Object> values) {
      return values != null && !values.isEmpty()
              ? this.deviceRef.child("settings").child("fertilization").setValue(values)
              : Tasks.forResult(null);
   }

   public void loadFertilizationPreferences(final Consumer<Map<String, Object>> consumer) {
      this.deviceRef.child("settings").child("fertilization")
              .addListenerForSingleValueEvent(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            Map<String, Object> values = new HashMap<>();
            for (DataSnapshot child : snapshot.getChildren()) {
               values.put(child.getKey(), child.getValue());
            }
            consumer.accept(values);
         }

         public void onCancelled(@NonNull DatabaseError error) {
            consumer.accept(new HashMap<>());
         }
      });
   }
   public Task<Void> savePushToken(Context context, String token) {
      if (token == null || token.isBlank()) {
         return Tasks.forException(
                 new IllegalArgumentException("Push token is required")
         );
      }

      Map<String, Object> values = new HashMap<>();
      values.put("token", token);
      values.put("updated_at_epoch", ServerValue.TIMESTAMP);
      values.put("platform", "android");

      String key = stableDeviceKey(context);

      return this.pushTokensRef
              .child(key)
              .setValue(values);
   }

   private static String stableDeviceKey(Context context) {
      try {
         String androidId = Settings.Secure.getString(
                 context.getContentResolver(),
                 Settings.Secure.ANDROID_ID
         );

         if (androidId == null || androidId.isBlank()) {
            throw new IllegalStateException("ANDROID_ID unavailable");
         }

         byte[] digest = MessageDigest
                 .getInstance("SHA-256")
                 .digest(androidId.getBytes(StandardCharsets.UTF_8));

         StringBuilder key = new StringBuilder("android_");

         for (byte value : digest) {
            key.append(String.format("%02x", value));
         }

         return key.toString();

      } catch (Exception e) {
         return "android_device_unknown";
      }
   }

   public Task<Void> saveGardenProfile(GardenProfile profile) {
      Map<String, Object> values = new HashMap<>();
      values.put("profile/garden_name", profile.getGarden_name());
      values.put("profile/garden_type", profile.getGarden_type());
      values.put("profile/area_square_meters", profile.getArea_square_meters());
      values.put("profile/notes", profile.getNotes());
      values.put("profile/updated_at_epoch", profile.getUpdated_at_epoch());
      return this.deviceRef.updateChildren(values);
   }

   public LiveData<GardenProfile> observeGardenProfile() {
      MutableLiveData<GardenProfile> liveData = new MutableLiveData<>();
      this.deviceRef.child("profile").addValueEventListener(new ValueEventListener() {
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            liveData.setValue(snapshot.getValue(GardenProfile.class));
         }

         public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "Garden profile could not be read", error.toException());
         }
      });
      return liveData;
   }

   public Task<Void> saveDisplayUnitSettings(DisplayUnitSettings settings) {
      Map<String, Object> values = new HashMap<>();
      values.put("profile/display_units/temperature", settings.getTemperature());
      values.put("profile/display_units/area", settings.getArea());
      values.put("profile/display_units/length", settings.getLength());
      values.put("profile/display_units/volume", settings.getVolume());
      values.put("profile/display_units/weight", settings.getWeight());
      values.put("profile/display_units/updated_at_epoch", System.currentTimeMillis() / 1000L);
      return this.deviceRef.updateChildren(values);
   }

   public LiveData<DisplayUnitSettings> observeDisplayUnitSettings() {
      MutableLiveData<DisplayUnitSettings> liveData = new MutableLiveData<>();
      this.deviceRef.child("profile").child("display_units")
            .addValueEventListener(new ValueEventListener() {
               public void onDataChange(@NonNull DataSnapshot snapshot) {
                  liveData.setValue(snapshot.getValue(DisplayUnitSettings.class));
               }

               public void onCancelled(@NonNull DatabaseError error) {
                  Log.w(TAG, "Display units could not be read", error.toException());
               }
            });
      return liveData;
   }

}
