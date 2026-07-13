package com.ali.smartgarden.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseRepository {

    private static final String DEVICE_ID = "smartgarden-001";

    private final DatabaseReference deviceRef;

    public FirebaseRepository() {

        deviceRef = FirebaseDatabase
                .getInstance()
                .getReference("devices")
                .child(DEVICE_ID);
    }


    // ---------------------------------------------------------
    // DATABASE REFERENCES
    // ---------------------------------------------------------

    public DatabaseReference getSensorRef() {
        return deviceRef.child("sensor");
    }

    public DatabaseReference getStatusRef() {
        return deviceRef.child("status");
    }

    public DatabaseReference getCommandsRef() {
        return deviceRef.child("commands");
    }

    public DatabaseReference getHistoryRef() {
        return deviceRef.child("watering_history");
    }

    public DatabaseReference getHealthRef() {
        return deviceRef.child("health");
    }

    public DatabaseReference getStatisticsRef() {
        return deviceRef.child("statistics");
    }


    // ---------------------------------------------------------
    // REAL-TIME OBSERVERS
    // ---------------------------------------------------------

    public void observeSensor(ValueEventListener listener) {
        getSensorRef().addValueEventListener(listener);
    }

    public void observeStatus(ValueEventListener listener) {
        getStatusRef().addValueEventListener(listener);
    }

    public void observeCommands(ValueEventListener listener) {
        getCommandsRef().addValueEventListener(listener);
    }

    public void observeHealth(ValueEventListener listener) {
        getHealthRef().addValueEventListener(listener);
    }

    public void observeStatistics(ValueEventListener listener) {
        getStatisticsRef().addValueEventListener(listener);
    }


    // ---------------------------------------------------------
    // COMMANDS
    // ---------------------------------------------------------

    public void setRelay(boolean value) {

        getCommandsRef()
                .child("relay")
                .setValue(value);
    }

    public void setAutoMode(boolean value) {

        getCommandsRef()
                .child("auto_mode")
                .setValue(value);
    }

    public void startManualWatering() {

        getCommandsRef()
                .child("auto_mode")
                .setValue(false);

        getCommandsRef()
                .child("relay")
                .setValue(true);
    }

    public void stopManualWatering() {

        getCommandsRef()
                .child("relay")
                .setValue(false);
    }
}