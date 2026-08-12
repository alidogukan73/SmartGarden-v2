package com.ali.smartgarden.firebase;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

   public Task<Void> recordFertilizerApplication(String zoneId, String zoneName, FertilizerProduct product, double appliedDose, String appliedUnit, double areaM2, double tankLiters, double recommendedDoseMin, double recommendedDoseMax, boolean deductStock, String applicationMethod, String notes, long appliedAt, String applicationType) {
      String applicationId = "application-" + UUID.randomUUID();
      long recordedAt = System.currentTimeMillis() / 1000L;
      if (appliedAt <= 0L) {
         appliedAt = recordedAt;
      }

      long nextAt = appliedAt + (long)Math.max(1, product.getMinimum_interval_days()) * 86400L;
      String planId = "plan-" + zoneId;
      Map<String, Object> updates = new HashMap();
      String historyPath = "fertilizer_history/" + applicationId + "/";
      updates.put(historyPath + "application_id", applicationId);
      updates.put(historyPath + "zone_id", zoneId);
      updates.put(historyPath + "zone_name", zoneName);
      updates.put(historyPath + "product_id", product.getProduct_id());
      updates.put(historyPath + "product_name", product.getName());
      updates.put(historyPath + "applied_dose", appliedDose);
      updates.put(historyPath + "dose_unit", appliedUnit);
      updates.put(historyPath + "area_m2", areaM2);
      updates.put(historyPath + "tank_liters", tankLiters);
      updates.put(historyPath + "recommended_dose_min", recommendedDoseMin);
      updates.put(historyPath + "recommended_dose_max", recommendedDoseMax);
      updates.put(historyPath + "applied_at_epoch", appliedAt);
      updates.put(historyPath + "next_application_at_epoch", nextAt);
      updates.put(historyPath + "source", "MANUAL");
      updates.put(historyPath + "application_type", applicationType);
      updates.put(historyPath + "application_method", applicationMethod);
      updates.put(historyPath + "notes", notes);
      String schedulePath = "zones/" + zoneId + "/fertilization/application_schedules/" + applicationType + "/";
      updates.put(schedulePath + "product_id", product.getProduct_id());
      updates.put(schedulePath + "product_name", product.getName());
      updates.put(schedulePath + "last_application_at_epoch", appliedAt);
      updates.put(schedulePath + "next_application_at_epoch", nextAt);
      updates.put(schedulePath + "interval_days", Math.max(1, product.getMinimum_interval_days()));
      updates.put(schedulePath + "updated_at_epoch", recordedAt);
      if (deductStock && product.getStock_unit() != null && product.getStock_unit().equalsIgnoreCase(appliedUnit)) {
         updates.put("fertilizer_products/" + product.getProduct_id() + "/stock_amount", Math.max((double)0.0F, product.getStock_amount() - appliedDose));
         updates.put("fertilizer_products/" + product.getProduct_id() + "/updated_at_epoch", recordedAt);
         updates.put(historyPath + "stock_deducted", true);
      } else {
         updates.put(historyPath + "stock_deducted", false);
      }

      if ("NUTRITION".equals(applicationType)) {
         String profilePath = "zones/" + zoneId + "/fertilization/";
         updates.put(profilePath + "last_application_at_epoch", appliedAt);
         updates.put(profilePath + "next_application_at_epoch", nextAt);
         updates.put(profilePath + "updated_at_epoch", recordedAt);
         String planPath = "fertilizer_plans/" + planId + "/";
         updates.put(planPath + "last_application_at_epoch", appliedAt);
         updates.put(planPath + "next_application_at_epoch", nextAt);
         updates.put(planPath + "updated_at_epoch", recordedAt);
      }

      return this.deviceRef.updateChildren(updates);
   }

   public Task<Void> deductBulkFertilizerStock(FertilizerProduct product, double totalAmount, String unit) {
      if (product.getStock_unit() != null && product.getStock_unit().equalsIgnoreCase(unit)) {
         Map<String, Object> updates = new HashMap();
         updates.put("fertilizer_products/" + product.getProduct_id() + "/stock_amount", Math.max((double)0.0F, product.getStock_amount() - totalAmount));
         updates.put("fertilizer_products/" + product.getProduct_id() + "/updated_at_epoch", System.currentTimeMillis() / 1000L);
         return this.deviceRef.updateChildren(updates);
      } else {
         return Tasks.forResult(null);
      }
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

   public Task<Void> deleteFertilizerApplication(FertilizerApplication target, List<FertilizerApplication> allApplications) {
      Map<String, Object> updates = new HashMap();
      updates.put("fertilizer_history/" + target.getApplication_id(), (Object)null);
      String type = target.getApplication_type() != null && !target.getApplication_type().isBlank() ? target.getApplication_type() : "NUTRITION";
      FertilizerApplication latest = null;

      for(FertilizerApplication value : allApplications) {
         if (!value.getApplication_id().equals(target.getApplication_id()) && target.getZone_id().equals(value.getZone_id())) {
            String candidateType = value.getApplication_type() != null && !value.getApplication_type().isBlank() ? value.getApplication_type() : "NUTRITION";
            if (type.equals(candidateType) && (latest == null || value.getApplied_at_epoch() > latest.getApplied_at_epoch())) {
               latest = value;
            }
         }
      }

      String schedulePath = "zones/" + target.getZone_id() + "/fertilization/application_schedules/" + type;
      if (latest == null) {
         updates.put(schedulePath, (Object)null);
      } else {
         updates.put(schedulePath + "/product_name", latest.getProduct_name());
         updates.put(schedulePath + "/last_application_at_epoch", latest.getApplied_at_epoch());
         updates.put(schedulePath + "/next_application_at_epoch", latest.getNext_application_at_epoch());
      }

      if ("NUTRITION".equals(type)) {
         String profilePath = "zones/" + target.getZone_id() + "/fertilization/";
         updates.put(profilePath + "last_application_at_epoch", latest == null ? 0L : latest.getApplied_at_epoch());
         updates.put(profilePath + "next_application_at_epoch", latest == null ? 0L : latest.getNext_application_at_epoch());
         String planPath = "fertilizer_plans/plan-" + target.getZone_id() + "/";
         updates.put(planPath + "last_application_at_epoch", latest == null ? 0L : latest.getApplied_at_epoch());
         updates.put(planPath + "next_application_at_epoch", latest == null ? 0L : latest.getNext_application_at_epoch());
      }

      return this.deviceRef.updateChildren(updates);
   }

   public Task<Void> updateFertilizerApplication(FertilizerApplication value) {
      Map<String, Object> updates = new HashMap();
      String path = "fertilizer_history/" + value.getApplication_id() + "/";
      updates.put(path + "applied_dose", value.getApplied_dose());
      updates.put(path + "applied_at_epoch", value.getApplied_at_epoch());
      updates.put(path + "next_application_at_epoch", value.getNext_application_at_epoch());
      updates.put(path + "application_method", value.getApplication_method());
      updates.put(path + "notes", value.getNotes());
      updates.put(path + "outcome_observed_at_epoch", value.getOutcome_observed_at_epoch());
      updates.put(path + "outcome_status", value.getOutcome_status());
      updates.put(path + "outcome_vigor_score", value.getOutcome_vigor_score());
      updates.put(path + "outcome_notes", value.getOutcome_notes());
      updates.put(path + "updated_at_epoch", System.currentTimeMillis() / 1000L);
      String type = value.getApplication_type() != null && !value.getApplication_type().isBlank() ? value.getApplication_type() : "NUTRITION";
      String schedulePath = "zones/" + value.getZone_id() + "/fertilization/application_schedules/" + type + "/";
      updates.put(schedulePath + "last_application_at_epoch", value.getApplied_at_epoch());
      updates.put(schedulePath + "next_application_at_epoch", value.getNext_application_at_epoch());
      if ("NUTRITION".equals(type)) {
         String profilePath = "zones/" + value.getZone_id() + "/fertilization/";
         updates.put(profilePath + "last_application_at_epoch", value.getApplied_at_epoch());
         updates.put(profilePath + "next_application_at_epoch", value.getNext_application_at_epoch());
      }

      return this.deviceRef.updateChildren(updates);
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

   public Task<Void> savePushToken(String token) {
      if (token != null && !token.isBlank()) {
         Map<String, Object> values = new HashMap();
         values.put("token", token);
         values.put("updated_at_epoch", ServerValue.TIMESTAMP);
         values.put("platform", "android");
         return this.pushTokensRef.child(stableTokenKey(token)).setValue(values);
      } else {
         return Tasks.forException(new IllegalArgumentException("Push token is required"));
      }
   }

   private static String stableTokenKey(String token) {
      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
         StringBuilder key = new StringBuilder("android_");

         for(byte value : digest) {
            key.append(String.format("%02x", value));
         }

         return key.toString();
      } catch (Exception var7) {
         return "android_" + Integer.toHexString(token.hashCode());
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
