package com.ali.smartgarden.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.ali.smartgarden.models.AdaptiveRecommendation;

import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

public class FirebaseRepository {

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
}