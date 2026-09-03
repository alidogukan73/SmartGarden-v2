package com.alidogukan.avora.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.alidogukan.avora.fertilization.FertilizationRepository;
import com.alidogukan.avora.fertilization.FertilizationPreferenceStore;
import com.alidogukan.avora.fertilization.FertilizerAdvice;
import com.alidogukan.avora.fertilization.FertilizerApplicationSafety;
import com.alidogukan.avora.fertilization.FertilizerDecisionEngine;
import com.alidogukan.avora.fertilization.FertilizerMixAdvisor;
import com.alidogukan.avora.fertilization.FertilizerMixResult;
import com.alidogukan.avora.fertilization.FertilizerSafetyPolicy;
import com.alidogukan.avora.fertilization.OrganicFertilizerAiAdvisor;
import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.FertilizerApplication;
import com.alidogukan.avora.models.FertilizerProduct;
import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.WeatherForecast;
import com.alidogukan.avora.season.SeasonRepository;
import com.alidogukan.avora.season.SeasonScope;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.ArrayList;

public class FertilizationCalendarViewModel extends AndroidViewModel {

    private final LiveData<List<GardenZone>> zones;
    private final LiveData<List<GardenSeason>> seasons;
    private final LiveData<List<FertilizerProduct>> products;
    private final LiveData<List<FertilizerApplication>> history;
    private final LiveData<WeatherForecast> weather;
    private final FertilizationRepository repository;
    private final FertilizationPreferenceStore preferences;

    public FertilizationCalendarViewModel(@NonNull Application application) {
        super(application);
        repository = new FertilizationRepository();
        preferences = new FertilizationPreferenceStore(application);
        zones = repository.observeZones();
        seasons = new SeasonRepository().observeAllSeasons();
        products = repository.observeProducts();
        history = repository.observeHistory();
        weather = repository.observeWeather();
    }

    public boolean preferOrganicInputs() {
        return preferences.preferOrganicInputs();
    }

    public FertilizerMixResult assessMix(FertilizerProduct first, FertilizerProduct second) {
        return FertilizerMixAdvisor.assess(first, second);
    }
    public boolean isEligible(FertilizerProduct product, FertilizationProfile profile) {
        return FertilizerSafetyPolicy.isEligible(product, profile);
    }
    public Dose calculateDose(
            FertilizerProduct product, FertilizationProfile profile) {
        FertilizerApplicationSafety.Dose value =
                FertilizerApplicationSafety.calculateDose(product, profile);
        return new Dose(value.getMinAmount(), value.getMaxAmount(),
                value.getUnit(), value.isTankBased(), value.isSupported());
    }
    public String applicationType(FertilizerProduct product) {
        return FertilizerApplicationSafety.applicationType(product);
    }
    public boolean isRepeatIntervalBlocked(FertilizationProfile profile, String type,
                                           long appliedAt) {
        return FertilizerApplicationSafety.isRepeatIntervalBlocked(
                profile, type, appliedAt);
    }
    public boolean isStockUnitCompatible(FertilizerProduct product, String unit) {
        return FertilizerApplicationSafety.isStockUnitCompatible(product, unit);
    }
    public boolean hasEnoughStock(FertilizerProduct product, double amount) {
        return FertilizerApplicationSafety.hasEnoughStock(product, amount);
    }
    public FertilizerAdvice advise(GardenZone zone, List<FertilizerProduct> products,
                                   WeatherForecast weather,
                                   List<FertilizerApplication> history, long now) {
        return FertilizerDecisionEngine.advise(zone, products, weather, history, now,
                preferOrganicInputs());
    }
    public boolean requiresOrganicAi(FertilizerAdvice advice) {
        return OrganicFertilizerAiAdvisor.isRequired(advice);
    }
    public boolean requiresOrganicProduct(FertilizationProfile profile) {
        return FertilizerSafetyPolicy.requiresOrganicProduct(profile);
    }
    public void requestOrganicAdvice(GardenZone zone, boolean compact,
                                     OrganicAdviceCallback callback) {
        OrganicFertilizerAiAdvisor.request(zone, new OrganicFertilizerAiAdvisor.Callback() {
            @Override public void onResult(OrganicFertilizerAiAdvisor.Result result) {
                callback.onResult(compact ? result.compactText()
                        : result.fullText(getApplication()));
            }
            @Override public void onUnavailable() { callback.onUnavailable(); }
        });
    }

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public LiveData<List<GardenSeason>> getSeasons() {
        return seasons;
    }

    public List<GardenZone> activeZones(List<GardenZone> values) {
        return SeasonScope.activeSeasonZones(values);
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
            List<FertilizerApplicationBatch> batches
    ) {
        List<FertilizationRepository.FertilizerApplicationBatch> values = new ArrayList<>();
        for (FertilizerApplicationBatch batch : batches) {
            values.add(new FertilizationRepository.FertilizerApplicationBatch(
                    batch.product, repositoryApplications(batch.applications),
                    batch.appliedUnit, batch.deductStock));
        }
        return repository.recordBatches(values);
    }

    public Task<Void> recordBulk(
            FertilizerProduct product,
            List<BulkFertilizerApplication> applications,
            String appliedUnit,
            boolean deductStock
    ) {
        return repository.recordBulk(product, repositoryApplications(applications),
                appliedUnit, deductStock);
    }

    private List<FertilizationRepository.BulkFertilizerApplication> repositoryApplications(
            List<BulkFertilizerApplication> applications) {
        List<FertilizationRepository.BulkFertilizerApplication> values = new ArrayList<>();
        for (BulkFertilizerApplication value : applications) {
            values.add(new FertilizationRepository.BulkFertilizerApplication(
                    value.zoneId, value.zoneName, value.appliedDose, value.areaM2,
                    value.tankLiters, value.recommendedDoseMin, value.recommendedDoseMax,
                    value.applicationMethod, value.notes, value.appliedAt,
                    value.applicationType, value.mixGroupId, value.mixPartnerProductId,
                    value.mixPartnerProductName, value.mixRiskLevel));
        }
        return values;
    }

    public static final class BulkFertilizerApplication {
        private final String zoneId;
        private final String zoneName;
        private final double appliedDose;
        private final double areaM2;
        private final double tankLiters;
        private final double recommendedDoseMin;
        private final double recommendedDoseMax;
        private final String applicationMethod;
        private final String notes;
        private final long appliedAt;
        private final String applicationType;
        private final String mixGroupId;
        private final String mixPartnerProductId;
        private final String mixPartnerProductName;
        private final String mixRiskLevel;

        public BulkFertilizerApplication(String zoneId, String zoneName, double appliedDose,
                double areaM2, double tankLiters, double recommendedDoseMin,
                double recommendedDoseMax, String applicationMethod, String notes,
                long appliedAt, String applicationType) {
            this(zoneId, zoneName, appliedDose, areaM2, tankLiters, recommendedDoseMin,
                    recommendedDoseMax, applicationMethod, notes, appliedAt, applicationType,
                    "", "", "", "");
        }

        public BulkFertilizerApplication(String zoneId, String zoneName, double appliedDose,
                double areaM2, double tankLiters, double recommendedDoseMin,
                double recommendedDoseMax, String applicationMethod, String notes,
                long appliedAt, String applicationType, String mixGroupId,
                String mixPartnerProductId, String mixPartnerProductName, String mixRiskLevel) {
            this.zoneId = zoneId;
            this.zoneName = zoneName;
            this.appliedDose = appliedDose;
            this.areaM2 = areaM2;
            this.tankLiters = tankLiters;
            this.recommendedDoseMin = recommendedDoseMin;
            this.recommendedDoseMax = recommendedDoseMax;
            this.applicationMethod = applicationMethod;
            this.notes = notes;
            this.appliedAt = appliedAt;
            this.applicationType = applicationType;
            this.mixGroupId = mixGroupId == null ? "" : mixGroupId;
            this.mixPartnerProductId = mixPartnerProductId == null ? "" : mixPartnerProductId;
            this.mixPartnerProductName = mixPartnerProductName == null
                    ? "" : mixPartnerProductName;
            this.mixRiskLevel = mixRiskLevel == null ? "" : mixRiskLevel;
        }
    }

    public static final class FertilizerApplicationBatch {
        private final FertilizerProduct product;
        private final List<BulkFertilizerApplication> applications;
        private final String appliedUnit;
        private final boolean deductStock;

        public FertilizerApplicationBatch(FertilizerProduct product,
                List<BulkFertilizerApplication> applications, String appliedUnit,
                boolean deductStock) {
            this.product = product;
            this.applications = applications;
            this.appliedUnit = appliedUnit;
            this.deductStock = deductStock;
        }
    }

    public interface OrganicAdviceCallback {
        void onResult(String content);
        void onUnavailable();
    }

    public static final class Dose {
        private final double minAmount;
        private final double maxAmount;
        private final String unit;
        private final boolean tankBased;
        private final boolean supported;
        Dose(double minAmount, double maxAmount, String unit,
             boolean tankBased, boolean supported) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.unit = unit == null ? "" : unit;
            this.tankBased = tankBased;
            this.supported = supported;
        }
        public double getAmount() { return minAmount; }
        public double getMinAmount() { return minAmount; }
        public double getMaxAmount() { return maxAmount; }
        public String getUnit() { return unit; }
        public boolean isTankBased() { return tankBased; }
        public boolean isSupported() { return supported; }
    }
}
