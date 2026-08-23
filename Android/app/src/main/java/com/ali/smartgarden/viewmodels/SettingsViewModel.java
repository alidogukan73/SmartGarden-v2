package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.IrrigationTimingSettings;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SettingsViewModel extends ViewModel {

    private final FirebaseRepository repository;
    private final LiveData<IrrigationTimingSettings> irrigationTimingSettings;

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
        irrigationTimingSettings = repository.observeIrrigationTimingSettings();

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
            boolean enabled,
            boolean autoMode,
            IrrigationTimingSettings timingSettings
    ) {

        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }

        saving.setValue(true);
        saveSuccess.setValue(false);

        Task<Void> globalSettingsTask =
                repository
                        .saveGlobalSettingsAndSyncZones(
                                moistureLimit,
                                pumpDuration,
                                cooldownSeconds,
                                restartDelta,
                                enabled,
                                autoMode
                        );
        Task<Void> timingSettingsTask =
                repository.saveIrrigationTimingSettings(timingSettings);
        Task<Void> saveTask = Tasks.whenAll(globalSettingsTask, timingSettingsTask);

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
                true,
                true,
                IrrigationTimingSettings.defaults()
        );
    }


    public LiveData<IrrigationTimingSettings> getIrrigationTimingSettings() {
        return irrigationTimingSettings;
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
