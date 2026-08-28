package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class FertilizationCalendarViewModel extends ViewModel {

    private final LiveData<List<GardenZone>> zones;
    private final LiveData<List<FertilizerProduct>> products;
    private final LiveData<List<FertilizerApplication>> history;
    private final LiveData<WeatherForecast> weather;
    private final FertilizationRepository repository;

    public FertilizationCalendarViewModel() {
        repository = new FertilizationRepository();
        zones = repository.observeZones();
        products = repository.observeProducts();
        history = repository.observeHistory();
        weather = repository.observeWeather();
    }

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public LiveData<List<FertilizerProduct>> getProducts() {
        return products;
    }

    public LiveData<List<FertilizerApplication>> getHistory() {
        return history;
    }

    public LiveData<WeatherForecast> getWeather() {
        return weather;
    }

    public Task<Void> recordBatches(
            List<FertilizationRepository.FertilizerApplicationBatch> batches
    ) {
        return repository.recordBatches(batches);
    }

    public Task<Void> recordBulk(
            FertilizerProduct product,
            List<FertilizationRepository.BulkFertilizerApplication> applications,
            String appliedUnit,
            boolean deductStock
    ) {
        return repository.recordBulk(product, applications, appliedUnit, deductStock);
    }
}
