package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.IrrigationTimingSettings;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

/** Settings state whose Firebase reads follow the observing screen lifecycle. */
public class SettingsViewModel extends AndroidViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final LiveData<IrrigationTimingSettings> irrigationTimingSettings;
    private final LiveData<List<GardenZone>> activeGardenZones;
    private final MediatorLiveData<Command> command = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        irrigationTimingSettings = repository.observeIrrigationTimingSettings();
        activeGardenZones = Transformations.map(
                repository.observeGardenZones(),
                ZoneCapacityPolicy::activeZones);
        LiveData<Command> commandSource = repository.observeCommands(databaseError -> {
            loading.setValue(false);
            error.setValue(AvoraLanguageManager.localizedContext(
                    getApplication()).getString(R.string.settings_read_error));
        });
        command.addSource(commandSource, value -> {
            command.setValue(value);
            error.setValue(null);
            loading.setValue(false);
        });
    }

    public void saveSettings(
            long moistureLimit,
            long pumpDuration,
            long cooldownSeconds,
            long restartDelta,
            boolean enabled,
            boolean autoMode,
            IrrigationTimingSettings timingSettings
    ) {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        saving.setValue(true);
        saveSuccess.setValue(false);
        Task<Void> globalSettingsTask = repository.saveGlobalSettingsAndSyncZones(
                moistureLimit, pumpDuration, cooldownSeconds, restartDelta,
                enabled, autoMode);
        Task<Void> timingSettingsTask =
                repository.saveIrrigationTimingSettings(timingSettings);
        Tasks.whenAll(globalSettingsTask, timingSettingsTask)
                .addOnSuccessListener(unused -> {
                    saving.setValue(false);
                    saveSuccess.setValue(true);
                })
                .addOnFailureListener(exception -> {
                    saving.setValue(false);
                    saveSuccess.setValue(false);
                    error.setValue(AvoraLanguageManager.localizedContext(
                            getApplication()).getString(
                            R.string.settings_save_error));
                });
    }

    public void resetToDefaults() {
        saveSettings(40, 120, 600, 10, true, true,
                IrrigationTimingSettings.defaults());
    }

    public LiveData<IrrigationTimingSettings> getIrrigationTimingSettings() {
        return irrigationTimingSettings;
    }
    public LiveData<List<GardenZone>> getActiveGardenZones() {
        return activeGardenZones;
    }
    public Task<Void> restartIrrigationAssistant(String zoneId) {
        return repository.requestIrrigationAssistantRestart(zoneId);
    }
    public LiveData<Command> getCommand() { return command; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSaving() { return saving; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public void clearSaveSuccess() { saveSuccess.setValue(null); }
    public LiveData<String> getError() { return error; }
}
