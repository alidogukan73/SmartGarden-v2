package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.Map;

public final class FertilizationSettingsViewModel extends ViewModel {
    private final FertilizationRepository repository = new FertilizationRepository();
    private final LiveData<List<GardenZone>> zones = repository.observeZones();
    private final LiveData<List<FertilizerProduct>> products = repository.observeProducts();
    private final MutableLiveData<Map<String, Object>> notificationBackup =
            new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> preferenceBackup =
            new MutableLiveData<>();

    public FertilizationSettingsViewModel() {
        repository.loadNotificationSettings(notificationBackup::postValue);
        repository.loadPreferences(preferenceBackup::postValue);
    }

    public LiveData<List<GardenZone>> getZones() { return zones; }
    public LiveData<List<FertilizerProduct>> getProducts() { return products; }
    public LiveData<Map<String, Object>> getNotificationBackup() {
        return notificationBackup;
    }
    public LiveData<Map<String, Object>> getPreferenceBackup() {
        return preferenceBackup;
    }

    public Task<Void> save(
            Map<String, Object> notificationValues,
            Map<String, Object> preferenceValues
    ) {
        return Tasks.whenAll(
                repository.saveNotificationSettings(notificationValues),
                repository.savePreferences(preferenceValues)
        );
    }
}
