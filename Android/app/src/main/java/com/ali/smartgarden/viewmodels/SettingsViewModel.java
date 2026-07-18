package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Command;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SettingsViewModel extends ViewModel {

    private final FirebaseRepository repository;

    private final MutableLiveData<Command> command =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(true);

    private final MutableLiveData<Boolean> saving =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> saveSuccess =
            new MutableLiveData<>();

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private ValueEventListener commandListener;


    public SettingsViewModel() {

        repository = new FirebaseRepository();

        observeCommands();
    }


    /**
     * Firebase commands düğümünü gerçek zamanlı dinler.
     */
    private void observeCommands() {

        loading.setValue(true);

        commandListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {

                Command value =
                        snapshot.getValue(
                                Command.class
                        );

                command.setValue(value);
                error.setValue(null);
                loading.setValue(false);
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError databaseError
            ) {

                loading.setValue(false);

                String message = databaseError.getMessage();

                if (message == null || message.isBlank()) {
                    message = "Ayarlar alınamadı.";
                }

                error.setValue(message);
            }
        };

        repository
                .getCommandsRef()
                .addValueEventListener(
                        commandListener
                );
    }


    /**
     * Tüm ayarları Firebase'e tek işlemde yazar.
     */
    public void saveSettings(
            long moistureLimit,
            long pumpDuration,
            long cooldownSeconds,
            long restartDelta,
            boolean enabled
    ) {

        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }

        saving.setValue(true);
        saveSuccess.setValue(false);

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "moisture_limit",
                moistureLimit
        );

        updates.put(
                "pump_duration",
                pumpDuration
        );

        updates.put(
                "cooldown_seconds",
                cooldownSeconds
        );

        updates.put(
                "restart_delta",
                restartDelta
        );

        updates.put(
                "enabled",
                enabled
        );

        Task<Void> saveTask =
                repository
                        .getCommandsRef()
                        .updateChildren(
                                updates
                        );

        saveTask
                .addOnSuccessListener(
                        unused -> {

                            saving.setValue(false);
                            saveSuccess.setValue(true);
                        }
                )
                .addOnFailureListener(
                        exception -> {

                            saving.setValue(false);
                            saveSuccess.setValue(false);

                            String message =
                                    exception.getMessage();

                            error.setValue(
                                    message == null
                                            || message.isBlank()
                                            ? "Ayarlar kaydedilemedi."
                                            : message
                            );
                        }
                );
    }


    /**
     * Varsayılan değerleri Firebase'e kaydeder.
     */
    public void resetToDefaults() {

        saveSettings(
                40,
                120,
                600,
                10,
                true
        );
    }


    public LiveData<Command> getCommand() {

        return command;
    }


    public LiveData<Boolean> getLoading() {

        return loading;
    }


    public LiveData<Boolean> getSaving() {

        return saving;
    }


    public LiveData<Boolean> getSaveSuccess() {

        return saveSuccess;
    }
    public void clearSaveSuccess() {
        saveSuccess.setValue(null);
    }
    public LiveData<String> getError() {

        return error;
    }


    @Override
    protected void onCleared() {

        super.onCleared();

        if (commandListener != null) {

            repository
                    .getCommandsRef()
                    .removeEventListener(
                            commandListener
                    );
        }
    }
}