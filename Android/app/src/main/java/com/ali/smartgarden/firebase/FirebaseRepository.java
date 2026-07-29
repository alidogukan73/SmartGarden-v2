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

import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.SoilSensor;
import com.ali.smartgarden.models.SoilLearningProfile;

import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;


public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private static final String DEVICE_ID = "smartgarden-001";
    private final DatabaseReference deviceRef;
    private final DatabaseReference sensorRef;
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

    public FirebaseRepository() {

        deviceRef = FirebaseDatabase
                .getInstance()
                .getReference("devices")
                .child(DEVICE_ID);

        sensorRef = deviceRef.child("sensor");
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
    }

    // ---------------------------------------------------------
    // DATABASE REFERENCES
    // ---------------------------------------------------------

    public DatabaseReference getSensorRef() {
        return sensorRef;
    }
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
        sensorRef.addValueEventListener(listener);
    }
    public LiveData<SoilSensor> observeSoilSensor() {

        MutableLiveData<SoilSensor> liveData =
                new MutableLiveData<>();

        sensorRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        SoilSensor sensor =
                                snapshot.getValue(
                                        SoilSensor.class
                                );

                        liveData.setValue(sensor);
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Soil sensor read failed",
                                error.toException()
                        );
                    }
                }
        );

        return liveData;
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

        commandsRef
                .child("relay")
                .setValue(value);
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

        commandsRef.updateChildren(
                updates
        );
    }

    public void stopManualWatering() {

        commandsRef
                .child("relay")
                .setValue(false);
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