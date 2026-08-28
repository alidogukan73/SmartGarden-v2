package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.fertilization.FertilizationPreferenceStore;
import com.ali.smartgarden.fertilization.FertilizerAdvice;
import com.ali.smartgarden.fertilization.FertilizerAiAdvisor;
import com.ali.smartgarden.fertilization.FertilizerAiProfile;
import com.ali.smartgarden.fertilization.FertilizerDecisionEngine;
import com.ali.smartgarden.fertilization.FertilizerSafetyPolicy;
import com.ali.smartgarden.fertilization.FertilizerStagePolicy;
import com.ali.smartgarden.fertilization.OrganicFertilizerAiAdvisor;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.FertilizerStageGuide;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Data and mutation boundary for one zone's fertilization workflow. */
public final class FertilizationZoneDetailViewModel extends AndroidViewModel {
    private final FertilizationRepository repository = new FertilizationRepository();
    private final FertilizationPreferenceStore preferences;
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

    public FertilizationZoneDetailViewModel(@NonNull Application application) {
        super(application);
        preferences = new FertilizationPreferenceStore(application);
    }

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
    public boolean preferOrganicInputs() { return preferences.preferOrganicInputs(); }

    public FertilizerAdvice advise(GardenZone zone, List<FertilizerProduct> products,
                                   WeatherForecast weather,
                                   List<FertilizerApplication> history, long now) {
        return FertilizerDecisionEngine.advise(zone, products, weather, history, now,
                preferOrganicInputs());
    }
    public boolean requiresOrganicAi(FertilizerAdvice advice) {
        return OrganicFertilizerAiAdvisor.isRequired(advice);
    }
    public void requestOrganicAdvice(GardenZone zone, OrganicAdviceCallback callback) {
        OrganicFertilizerAiAdvisor.request(zone, new OrganicFertilizerAiAdvisor.Callback() {
            @Override public void onResult(OrganicFertilizerAiAdvisor.Result result) {
                callback.onResult(result.fullText(getApplication()));
            }
            @Override public void onUnavailable() { callback.onUnavailable(); }
        });
    }
    public boolean isEligibleForStage(FertilizerProduct product, String stage) {
        return FertilizerSafetyPolicy.isEligibleForStage(product, stage);
    }
    public boolean isEligible(FertilizerProduct product,
                              com.ali.smartgarden.models.FertilizationProfile profile) {
        return FertilizerSafetyPolicy.isEligible(product, profile);
    }
    public boolean isHarvestStage(String stage) {
        return FertilizerStagePolicy.HARVEST.equals(stage);
    }
    public boolean isSeasonEndStage(String stage) {
        return FertilizerStagePolicy.SEASON_END.equals(stage);
    }
    public ProductGuidance productGuidance(FertilizerProduct product) {
        FertilizerAiProfile profile = FertilizerAiAdvisor.profileFor(product);
        return new ProductGuidance(profile.getSuitability(), profile.getReason(),
                profile.getFruitStageAdvice(), profile.getSafetyNote());
    }

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

    public interface OrganicAdviceCallback {
        void onResult(String content);
        void onUnavailable();
    }

    public static final class ProductGuidance {
        public final String suitability;
        public final String reason;
        public final String fruitStageAdvice;
        public final String safetyNote;
        ProductGuidance(String suitability, String reason, String fruitStageAdvice,
                        String safetyNote) {
            this.suitability = suitability;
            this.reason = reason;
            this.fruitStageAdvice = fruitStageAdvice;
            this.safetyNote = safetyNote;
        }
    }
}
