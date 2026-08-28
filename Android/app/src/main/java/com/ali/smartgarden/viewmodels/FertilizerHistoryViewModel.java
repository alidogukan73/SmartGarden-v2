package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenZone;
import com.google.android.gms.tasks.Task;

import java.util.List;

public final class FertilizerHistoryViewModel extends ViewModel {
    private final FertilizationRepository repository = new FertilizationRepository();
    private final LiveData<List<FertilizerApplication>> history = repository.observeHistory();
    private final LiveData<List<GardenZone>> zones = repository.observeZones();

    public LiveData<List<FertilizerApplication>> getHistory() { return history; }
    public LiveData<List<GardenZone>> getZones() { return zones; }

    public Task<Void> update(FertilizerApplication application) {
        return repository.updateApplication(application);
    }

    public Task<Void> delete(FertilizerApplication application) {
        return repository.deleteApplication(application);
    }
}
