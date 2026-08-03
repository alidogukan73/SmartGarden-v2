package com.ali.smartgarden.firebase;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.SoilLearningProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.FertilizerStageGuide;
import com.ali.smartgarden.models.FertilizerApplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private static final String DEVICE_ID = "smartgarden-001";
    private final DatabaseReference deviceRef;
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

    public FirebaseRepository() {

        deviceRef = FirebaseDatabase
                .getInstance()
                .getReference("devices")
                .child(DEVICE_ID);

        DatabaseReference zonesRef =
                deviceRef.child("zones");

        primaryZoneRef =
                zonesRef.child("zone-001");
        statusRef = deviceRef.child("status");
        commandsRef = deviceRef.child("commands");
        historyRef = deviceRef.child("watering_history");
        healthRef = deviceRef.child("health");
        statisticsRef = deviceRef.child("statistics");
        adaptiveRecommendationRef = deviceRef.child("adaptive_recommendation");
        aiDecisionRef = deviceRef.child("ai_decision");
        aiExplanationRef = deviceRef.child("ai_explanation");

        moisturePredictionRef =
                deviceRef.child("moisture_prediction");

        predictionAccuracyRef =
                deviceRef.child("prediction_accuracy");

        unifiedConfidenceRef =
                deviceRef.child("unified_confidence");

        soilLearningProfileRef =
                deviceRef.child("soil_learning_profile");

        predictionValidationRef = deviceRef
                .child("ai")
                .child("prediction_validation");

        this.zonesRef = zonesRef;
        fertilizerProductsRef =
                deviceRef.child("fertilizer_products");
    }

    // ---------------------------------------------------------
    // DATABASE REFERENCES
    // ---------------------------------------------------------

    public DatabaseReference getStatusRef() {
        return statusRef;
    }
    public DatabaseReference getCommandsRef() {
        return commandsRef;
    }
    public DatabaseReference getHistoryRef() {
        return historyRef;
    }
    public DatabaseReference getHealthRef() {
        return healthRef;
    }
    public DatabaseReference getStatisticsRef() {
        return statisticsRef;
    }
    public DatabaseReference getAdaptiveRecommendationRef() {
        return adaptiveRecommendationRef;
    }
    public DatabaseReference getAIDecisionRef() { return aiDecisionRef; }
    public DatabaseReference getAIExplanationRef() { return aiExplanationRef; }

    // ---------------------------------------------------------
    // REAL-TIME OBSERVERS
    // ---------------------------------------------------------

    public void observeSensor(
            ValueEventListener listener
    ) {
        primaryZoneRef.addValueEventListener(
                listener
        );
    }
    public LiveData<List<GardenZone>> observeGardenZones() {

        MutableLiveData<List<GardenZone>> liveData =
                new MutableLiveData<>();

        zonesRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        List<GardenZone> zones =
                                new ArrayList<>();

                        for (
                                DataSnapshot child
                                : snapshot.getChildren()
                        ) {

                            GardenZone zone =
                                    child.getValue(
                                            GardenZone.class
                                    );

                            if (zone == null) {
                                continue;
                            }

                            if (
                                    zone.getZone_id() == null
                                            || zone.getZone_id()
                                            .isBlank()
                            ) {
                                zone.setZone_id(
                                        child.getKey()
                                );
                            }

                            zones.add(zone);
                        }

                        zones.sort(
                                Comparator.comparingInt(
                                        GardenZone::getOrder
                                )
                        );

                        liveData.setValue(zones);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Garden zones read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
    }

    public LiveData<GardenZone> observeGardenZone(
            String zoneId
    ) {
        MutableLiveData<GardenZone> liveData =
                new MutableLiveData<>();

        zonesRef.child(zoneId).addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        GardenZone zone = snapshot.getValue(
                                GardenZone.class
                        );
                        if (zone != null) {
                            zone.setZone_id(zoneId);
                        }
                        liveData.setValue(zone);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Garden zone read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
    }

    public Task<Void> updateGardenZoneSettings(
            String zoneId,
            boolean irrigationEnabled,
            int moistureLimit,
            int pumpDuration,
            int cooldownSeconds,
            int restartDelta,
            boolean sensorEnabled,
            int sensorDryRaw,
            int sensorWetRaw
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("irrigation_enabled", irrigationEnabled);
        updates.put("moisture_limit", moistureLimit);
        updates.put("pump_duration", pumpDuration);
        updates.put("cooldown_seconds", cooldownSeconds);
        updates.put("restart_delta", restartDelta);
        updates.put("sensor_enabled", sensorEnabled);
        updates.put("sensor_calibration_dry_raw", sensorDryRaw);
        updates.put("sensor_calibration_wet_raw", sensorWetRaw);
        updates.put("sensor_config_updated_at_epoch", ServerValue.TIMESTAMP);

        return zonesRef.child(zoneId).updateChildren(updates);
    }

    public Task<Void> updateGardenZoneValveMode(
            String zoneId,
            boolean physical
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("valve_mode", physical ? "PHYSICAL" : "SIMULATION");
        updates.put("valve_mode_updated_at_epoch", ServerValue.TIMESTAMP);
        return zonesRef.child(zoneId).updateChildren(updates);
    }

    public Task<Void> createGardenZone(GardenZone zone) {
        if (zone.getZone_id() == null || zone.getZone_id().isBlank()) {
            return Tasks.forException(new IllegalArgumentException(
                    "Bölge kimliği gerekli."
            ));
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> updates = new HashMap<>();
        String path = "zones/" + zone.getZone_id() + "/";
        updates.put(path + "zone_id", zone.getZone_id());
        updates.put(path + "name", zone.getName());
        updates.put(path + "plant_type", zone.getPlant_type());
        updates.put(path + "emoji", zone.getEmoji());
        updates.put(path + "sensor_id", zone.getSensor_id());
        updates.put(path + "valve_id", zone.getValve_id());
        updates.put(path + "enabled", zone.isEnabled());
        updates.put(path + "irrigation_enabled",
                zone.isIrrigation_enabled());
        updates.put(path + "moisture_limit",
                zone.getMoisture_limit());
        updates.put(path + "pump_duration",
                zone.getPump_duration());
        updates.put(path + "cooldown_seconds",
                zone.getCooldown_seconds());
        updates.put(path + "restart_delta",
                zone.getRestart_delta());
        updates.put(path + "order", zone.getOrder());
        updates.put(path + "updated_at_epoch", now);
        return deviceRef.updateChildren(updates);
    }

    public Task<Void> updateFertilizationProfile(
            String zoneId,
            boolean enabled,
            String plantingDate,
            String growthStage,
            boolean reminderEnabled,
            String productId,
            int intervalDays,
            long nextApplicationEpoch,
            double areaM2,
            double tankLiters
    ) {
        Map<String, Object> updates = new HashMap<>();
        String profilePath =
                "zones/" + zoneId + "/fertilization/";
        String planId = "plan-" + zoneId;
        long updatedAt = System.currentTimeMillis() / 1000L;

        updates.put(profilePath + "enabled", enabled);
        updates.put(profilePath + "planting_date", plantingDate);
        updates.put(profilePath + "growth_stage", growthStage);
        updates.put(
                profilePath + "reminder_enabled",
                reminderEnabled
        );
        updates.put(profilePath + "active_product_id", productId);
        updates.put(profilePath + "area_m2", areaM2);
        updates.put(profilePath + "tank_liters", tankLiters);
        updates.put(
                profilePath + "active_plan_id",
                enabled ? planId : ""
        );
        updates.put(
                profilePath + "next_application_at_epoch",
                enabled ? nextApplicationEpoch : 0L
        );
        updates.put(profilePath + "updated_at_epoch", updatedAt);

        String planPath = "fertilizer_plans/" + planId + "/";
        updates.put(planPath + "plan_id", planId);
        updates.put(planPath + "zone_id", zoneId);
        updates.put(planPath + "product_id", productId);
        updates.put(planPath + "interval_days", intervalDays);
        updates.put(planPath + "area_m2", areaM2);
        updates.put(planPath + "tank_liters", tankLiters);
        updates.put(planPath + "enabled", enabled);
        updates.put(
                planPath + "next_application_at_epoch",
                enabled ? nextApplicationEpoch : 0L
        );
        updates.put(planPath + "updated_at_epoch", updatedAt);

        return deviceRef.updateChildren(updates);
    }

    public LiveData<List<FertilizerProduct>>
    observeFertilizerProducts() {
        MutableLiveData<List<FertilizerProduct>> liveData =
                new MutableLiveData<>();

        fertilizerProductsRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        List<FertilizerProduct> products =
                                new ArrayList<>();
                        for (DataSnapshot child
                                : snapshot.getChildren()) {
                            FertilizerProduct product =
                                    child.getValue(
                                            FertilizerProduct.class
                                    );
                            if (product == null) {
                                continue;
                            }
                            if (product.getProduct_id() == null
                                    || product.getProduct_id().isBlank()) {
                                product.setProduct_id(child.getKey());
                            }
                            products.add(product);
                        }
                        products.sort(
                                Comparator.comparing(
                                        product -> product.getName() == null
                                                ? ""
                                                : product.getName(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        );
                        liveData.setValue(products);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Fertilizer products read failed",
                                error.toException()
                        );
                    }
                }
        );
        return liveData;
    }

    public LiveData<List<FertilizerRecommendation>>
    observeFertilizerRecommendations() {
        MutableLiveData<List<FertilizerRecommendation>> liveData =
                new MutableLiveData<>();

        deviceRef.child("fertilization")
                .child("recommendations")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        List<FertilizerRecommendation> values =
                                new ArrayList<>();
                        for (DataSnapshot plant : snapshot.getChildren()) {
                            for (DataSnapshot stage
                                    : plant.getChildren()) {
                                for (DataSnapshot entry
                                        : stage.getChildren()) {
                                    FertilizerRecommendation value =
                                            entry.getValue(
                                                    FertilizerRecommendation.class
                                            );
                                    if (value == null) {
                                        continue;
                                    }
                                    value.setPlant_type(plant.getKey());
                                    value.setGrowth_stage(stage.getKey());
                                    value.setRecommendation_id(entry.getKey());
                                    values.add(value);
                                }
                            }
                        }
                        liveData.setValue(values);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Fertilizer recommendations read failed",
                                error.toException()
                        );
                    }
                });
        return liveData;
    }

    public LiveData<List<FertilizerStageGuide>>
    observeFertilizerStageGuides() {
        MutableLiveData<List<FertilizerStageGuide>> liveData =
                new MutableLiveData<>();

        deviceRef.child("fertilization")
                .child("stage_guides")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        List<FertilizerStageGuide> values =
                                new ArrayList<>();
                        for (DataSnapshot plant : snapshot.getChildren()) {
                            for (DataSnapshot stage
                                    : plant.getChildren()) {
                                FertilizerStageGuide value =
                                        stage.getValue(
                                                FertilizerStageGuide.class
                                        );
                                if (value == null) {
                                    continue;
                                }
                                value.setPlant_type(plant.getKey());
                                value.setGrowth_stage(stage.getKey());
                                values.add(value);
                            }
                        }
                        liveData.setValue(values);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Fertilizer stage guides read failed",
                                error.toException()
                        );
                    }
                });
        return liveData;
    }

    public Task<Void> saveFertilizerProduct(
            FertilizerProduct product
    ) {
        String productId = product.getProduct_id();
        if (productId == null || productId.isBlank()) {
            productId = "product-" + UUID.randomUUID();
            product.setProduct_id(productId);
        }
        product.setUpdated_at_epoch(
                System.currentTimeMillis() / 1000L
        );
        return fertilizerProductsRef
                .child(productId)
                .setValue(product);
    }

    public Task<List<String>> findActiveZonesUsingFertilizer(
            String productId
    ) {
        return zonesRef.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() == null
                        ? new IllegalStateException(
                        "Garden zones could not be read"
                )
                        : task.getException();
            }
            List<String> zoneNames = new ArrayList<>();
            for (DataSnapshot child
                    : task.getResult().getChildren()) {
                GardenZone zone = child.getValue(GardenZone.class);
                if (zone == null
                        || zone.getFertilization() == null
                        || !zone.getFertilization().isEnabled()
                        || !productId.equals(
                        zone.getFertilization()
                                .getActive_product_id()
                )) {
                    continue;
                }
                String name = zone.getName();
                zoneNames.add(
                        name == null || name.isBlank()
                                ? child.getKey()
                                : name
                );
            }
            return zoneNames;
        });
    }

    public Task<Void> deactivateFertilizerProduct(String productId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("enabled", false);
        updates.put(
                "updated_at_epoch",
                System.currentTimeMillis() / 1000L
        );
        return fertilizerProductsRef
                .child(productId)
                .updateChildren(updates);
    }

    public Task<Void> deleteFertilizerProduct(String productId) {
        return fertilizerProductsRef
                .child(productId)
                .removeValue();
    }

    public Task<Void> recordFertilizerApplication(
            String zoneId,
            String zoneName,
            FertilizerProduct product,
            double appliedDose,
            String appliedUnit,
            double areaM2,
            double tankLiters,
            double recommendedDoseMin,
            double recommendedDoseMax,
            boolean deductStock,
            String applicationMethod,
            String notes,
            long appliedAt,
            String applicationType
    ) {
        String applicationId =
                "application-" + UUID.randomUUID();
        long recordedAt = System.currentTimeMillis() / 1000L;
        if (appliedAt <= 0L) {
            appliedAt = recordedAt;
        }
        long nextAt = appliedAt
                + Math.max(1, product.getMinimum_interval_days())
                * 86400L;
        String planId = "plan-" + zoneId;

        Map<String, Object> updates = new HashMap<>();
        String historyPath =
                "fertilizer_history/" + applicationId + "/";
        updates.put(historyPath + "application_id", applicationId);
        updates.put(historyPath + "zone_id", zoneId);
        updates.put(historyPath + "zone_name", zoneName);
        updates.put(
                historyPath + "product_id",
                product.getProduct_id()
        );
        updates.put(
                historyPath + "product_name",
                product.getName()
        );
        updates.put(historyPath + "applied_dose", appliedDose);
        updates.put(
                historyPath + "dose_unit",
                appliedUnit
        );
        updates.put(historyPath + "area_m2", areaM2);
        updates.put(historyPath + "tank_liters", tankLiters);
        updates.put(
                historyPath + "recommended_dose_min",
                recommendedDoseMin
        );
        updates.put(
                historyPath + "recommended_dose_max",
                recommendedDoseMax
        );
        updates.put(historyPath + "applied_at_epoch", appliedAt);
        updates.put(
                historyPath + "next_application_at_epoch",
                nextAt
        );
        updates.put(historyPath + "source", "MANUAL");
        updates.put(historyPath + "application_type", applicationType);
        updates.put(
                historyPath + "application_method",
                applicationMethod
        );
        updates.put(historyPath + "notes", notes);
        String schedulePath = "zones/" + zoneId
                + "/fertilization/application_schedules/"
                + applicationType + "/";
        updates.put(schedulePath + "product_name", product.getName());
        updates.put(schedulePath + "last_application_at_epoch", appliedAt);
        updates.put(schedulePath + "next_application_at_epoch", nextAt);
        updates.put(schedulePath + "updated_at_epoch", recordedAt);
        if (deductStock
                && product.getStock_unit() != null
                && product.getStock_unit().equalsIgnoreCase(
                appliedUnit
        )) {
            updates.put(
                    "fertilizer_products/"
                            + product.getProduct_id()
                            + "/stock_amount",
                    Math.max(
                            0.0,
                            product.getStock_amount() - appliedDose
                    )
            );
            updates.put(
                    "fertilizer_products/"
                            + product.getProduct_id()
                            + "/updated_at_epoch",
                    recordedAt
            );
            updates.put(historyPath + "stock_deducted", true);
        } else {
            updates.put(historyPath + "stock_deducted", false);
        }

        if ("NUTRITION".equals(applicationType)) {
            String profilePath =
                    "zones/" + zoneId + "/fertilization/";
            updates.put(
                    profilePath + "last_application_at_epoch",
                    appliedAt
            );
            updates.put(
                    profilePath + "next_application_at_epoch",
                    nextAt
            );
            updates.put(profilePath + "updated_at_epoch", recordedAt);

            String planPath = "fertilizer_plans/" + planId + "/";
            updates.put(planPath + "last_application_at_epoch", appliedAt);
            updates.put(planPath + "next_application_at_epoch", nextAt);
            updates.put(planPath + "updated_at_epoch", recordedAt);
        }
        return deviceRef.updateChildren(updates);
    }

    public LiveData<List<FertilizerApplication>>
    observeFertilizerHistory() {
        MutableLiveData<List<FertilizerApplication>> liveData =
                new MutableLiveData<>();
        deviceRef.child("fertilizer_history")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        List<FertilizerApplication> values =
                                new ArrayList<>();
                        for (DataSnapshot child
                                : snapshot.getChildren()) {
                            FertilizerApplication value =
                                    child.getValue(
                                            FertilizerApplication.class
                                    );
                            if (value != null) {
                                values.add(value);
                            }
                        }
                        values.sort(
                                (left, right) -> Long.compare(
                                        right.getApplied_at_epoch(),
                                        left.getApplied_at_epoch()
                                )
                        );
                        liveData.setValue(values);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Fertilizer history read failed",
                                error.toException()
                        );
                    }
                });
        return liveData;
    }

    public Task<Void> deleteFertilizerApplication(
            FertilizerApplication target,
            List<FertilizerApplication> allApplications
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(
                "fertilizer_history/" + target.getApplication_id(),
                null
        );
        String type = target.getApplication_type() == null
                || target.getApplication_type().isBlank()
                ? "NUTRITION" : target.getApplication_type();
        FertilizerApplication latest = null;
        for (FertilizerApplication value : allApplications) {
            if (value.getApplication_id().equals(
                    target.getApplication_id()
            ) || !target.getZone_id().equals(value.getZone_id())) {
                continue;
            }
            String candidateType = value.getApplication_type() == null
                    || value.getApplication_type().isBlank()
                    ? "NUTRITION" : value.getApplication_type();
            if (!type.equals(candidateType)) {
                continue;
            }
            if (latest == null || value.getApplied_at_epoch()
                    > latest.getApplied_at_epoch()) {
                latest = value;
            }
        }
        String schedulePath = "zones/" + target.getZone_id()
                + "/fertilization/application_schedules/" + type;
        if (latest == null) {
            updates.put(schedulePath, null);
        } else {
            updates.put(schedulePath + "/product_name",
                    latest.getProduct_name());
            updates.put(schedulePath + "/last_application_at_epoch",
                    latest.getApplied_at_epoch());
            updates.put(schedulePath + "/next_application_at_epoch",
                    latest.getNext_application_at_epoch());
        }
        if ("NUTRITION".equals(type)) {
            String profilePath = "zones/" + target.getZone_id()
                    + "/fertilization/";
            updates.put(profilePath + "last_application_at_epoch",
                    latest == null ? 0L : latest.getApplied_at_epoch());
            updates.put(profilePath + "next_application_at_epoch",
                    latest == null ? 0L
                            : latest.getNext_application_at_epoch());
            String planPath = "fertilizer_plans/plan-"
                    + target.getZone_id() + "/";
            updates.put(planPath + "last_application_at_epoch",
                    latest == null ? 0L : latest.getApplied_at_epoch());
            updates.put(planPath + "next_application_at_epoch",
                    latest == null ? 0L
                            : latest.getNext_application_at_epoch());
        }
        return deviceRef.updateChildren(updates);
    }

    public Task<Void> updateFertilizerApplication(
            FertilizerApplication value
    ) {
        Map<String, Object> updates = new HashMap<>();
        String path = "fertilizer_history/"
                + value.getApplication_id() + "/";
        updates.put(path + "applied_dose", value.getApplied_dose());
        updates.put(path + "applied_at_epoch",
                value.getApplied_at_epoch());
        updates.put(path + "next_application_at_epoch",
                value.getNext_application_at_epoch());
        updates.put(path + "application_method",
                value.getApplication_method());
        updates.put(path + "notes", value.getNotes());
        updates.put(path + "updated_at_epoch",
                System.currentTimeMillis() / 1000L);
        String type = value.getApplication_type() == null
                || value.getApplication_type().isBlank()
                ? "NUTRITION" : value.getApplication_type();
        String schedulePath = "zones/" + value.getZone_id()
                + "/fertilization/application_schedules/" + type + "/";
        updates.put(schedulePath + "last_application_at_epoch",
                value.getApplied_at_epoch());
        updates.put(schedulePath + "next_application_at_epoch",
                value.getNext_application_at_epoch());
        if ("NUTRITION".equals(type)) {
            String profilePath = "zones/" + value.getZone_id()
                    + "/fertilization/";
            updates.put(profilePath + "last_application_at_epoch",
                    value.getApplied_at_epoch());
            updates.put(profilePath + "next_application_at_epoch",
                    value.getNext_application_at_epoch());
        }
        return deviceRef.updateChildren(updates);
    }

    public Task<Void> requestZoneValveTest(
            GardenZone zone,
            int durationSeconds
    ) {
        Map<String, Object> command = new HashMap<>();
        command.put("requested", true);
        command.put("request_id", UUID.randomUUID().toString());
        command.put("zone_id", zone.getZone_id());
        command.put("valve_id", zone.getValve_id());
        command.put(
                "duration",
                Math.max(1, Math.min(10800, durationSeconds))
        );
        command.put("cancel_requested", false);
        command.put("requested_at", ServerValue.TIMESTAMP);

        return commandsRef.child("zone_test").setValue(command);
    }

    public Task<Void> cancelZoneValveTest() {
        return commandsRef
                .child("zone_test")
                .child("cancel_requested")
                .setValue(true);
    }

    public void observeStatus(
            ValueEventListener listener
    ) {
        statusRef.addValueEventListener(listener);
    }

    public void observeCommands(
            ValueEventListener listener
    ) {
        commandsRef.addValueEventListener(listener);
    }

    public void observeHealth(
            ValueEventListener listener
    ) {
        healthRef.addValueEventListener(listener);
    }

    public void observeStatistics(
            ValueEventListener listener
    ) {
        statisticsRef.addValueEventListener(listener);
    }

    public void observeAdaptiveRecommendation(
            Consumer<AdaptiveRecommendation> consumer
    ) {

        adaptiveRecommendationRef.addValueEventListener(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        AdaptiveRecommendation recommendation =
                                snapshot.getValue(
                                        AdaptiveRecommendation.class
                                );

                        if (recommendation != null) {

                            consumer.accept(
                                    recommendation
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                    }
                }
        );
    }
    public void observeAIDecision(
            ValueEventListener listener
    ) {

        aiDecisionRef.addValueEventListener(
                listener
        );
    }
    public void observeAIExplanation(
            ValueEventListener listener
    ) {

        aiExplanationRef.addValueEventListener(
                listener
        );
    }


    // ---------------------------------------------------------
    // COMMANDS
    // ---------------------------------------------------------

    public void setRelay(
            boolean value
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("relay", value);
        updates.put(
                "relay_requested_at",
                ServerValue.TIMESTAMP
        );
        commandsRef.updateChildren(updates);
    }

    public void setAutoMode(
            boolean value
    ) {

        commandsRef
                .child("auto_mode")
                .setValue(value);
    }

    public void restartDevice() {

        commandsRef
                .child("restart_device")
                .setValue(true);
    }

    public void startManualWatering() {

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "auto_mode",
                false
        );

        updates.put(
                "relay",
                true
        );
        updates.put(
                "relay_requested_at",
                ServerValue.TIMESTAMP
        );

        commandsRef.updateChildren(
                updates
        );
    }

    public void stopManualWatering() {
        setRelay(false);
    }

    public LiveData<PredictionValidationStatus>
    observePredictionValidationStatus() {

        MutableLiveData<PredictionValidationStatus> liveData =
                new MutableLiveData<>();

        predictionValidationRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        PredictionValidationStatus status =
                                snapshot.getValue(
                                        PredictionValidationStatus.class
                                );

                        liveData.setValue(status);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                "FirebaseRepository",
                                "Prediction validation observation failed: "
                                        + error.getMessage()
                        );
                    }
                }
        );

        return liveData;
    }

    public LiveData<MoisturePrediction>
    observeMoisturePrediction() {

        MutableLiveData<MoisturePrediction> liveData =
                new MutableLiveData<>();

        moisturePredictionRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        MoisturePrediction value =
                                snapshot.getValue(
                                        MoisturePrediction.class
                                );

                        liveData.setValue(value);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Moisture prediction read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
    }

    public LiveData<PredictionAccuracy>
    observePredictionAccuracy() {

        MutableLiveData<PredictionAccuracy> liveData =
                new MutableLiveData<>();

        predictionAccuracyRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        PredictionAccuracy value =
                                snapshot.getValue(
                                        PredictionAccuracy.class
                                );

                        liveData.setValue(value);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Prediction accuracy read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
    }

    public LiveData<SoilLearningProfile>
    observeSoilLearningProfile() {

        MutableLiveData<SoilLearningProfile> liveData =
                new MutableLiveData<>();


        soilLearningProfileRef.addValueEventListener(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        SoilLearningProfile profile =
                                snapshot.getValue(
                                        SoilLearningProfile.class
                                );

                        liveData.setValue(profile);
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Soil learning profile read failed",
                                error.toException()
                        );
                    }
                }

        );


        return liveData;
    }

    public LiveData<UnifiedConfidence>
    observeUnifiedConfidence() {

        MutableLiveData<UnifiedConfidence> liveData =
                new MutableLiveData<>();

        unifiedConfidenceRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        UnifiedConfidence value =
                                snapshot.getValue(
                                        UnifiedConfidence.class
                                );

                        liveData.setValue(value);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Unified confidence read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
    }
}
