package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.FertilizerStageGuide;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Data and mutation boundary for one zone's fertilization workflow. */
public final class FertilizationZoneDetailViewModel extends ViewModel {
    private final FertilizationRepository repository = new FertilizationRepository();
    private final MutableLiveData<String> zoneId = new MutableLiveData<>();
    private final LiveData<GardenZone> zone =
            Transformations.switchMap(zoneId, repository::observeZone);
    private final LiveData<List<FertilizerProduct>> products = repository.observeProducts();
    private final LiveData<List<FertilizerRecommendation>> recommendations =
            repository.observeRecommendations();
    private final LiveData<List<FertilizerStageGuide>> stageGuides =
            repository.observeStageGuides();
    private final LiveData<List<FertilizerApplication>> history = repository.observeHistory();
    private final LiveData<WeatherForecast> weather = repository.observeWeather();

    public void setZoneId(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.equals(zoneId.getValue())) zoneId.setValue(normalized);
    }

    public LiveData<GardenZone> getZone() { return zone; }
    public LiveData<List<FertilizerProduct>> getProducts() { return products; }
    public LiveData<List<FertilizerRecommendation>> getRecommendations() {
        return recommendations;
    }
    public LiveData<List<FertilizerStageGuide>> getStageGuides() { return stageGuides; }
    public LiveData<List<FertilizerApplication>> getHistory() { return history; }
    public LiveData<WeatherForecast> getWeather() { return weather; }

    public Task<Void> updateWaterAnalysis(String zoneId, double ph, double ecMs) {
        return repository.updateWaterAnalysis(zoneId, ph, ecMs);
    }

    public Task<Void> updateProfile(
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
        return repository.updateProfile(
                zoneId, enabled, plantingDate, growthStage, reminderEnabled,
                productId, intervalDays, nextApplicationEpoch, areaM2, tankLiters);
    }

    public Task<Void> recordApplication(
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
        return repository.recordApplication(
                zoneId, zoneName, product, appliedDose, appliedUnit, areaM2,
                tankLiters, recommendedDoseMin, recommendedDoseMax, deductStock,
                applicationMethod, notes, appliedAt, applicationType);
    }
}
