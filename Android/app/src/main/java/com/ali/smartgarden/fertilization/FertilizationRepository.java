package com.ali.smartgarden.fertilization;

import androidx.lifecycle.LiveData;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.FertilizerStageGuide;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

/** Single Firebase boundary shared by all fertilization screens. */
public final class FertilizationRepository {
    private final FirebaseRepository firebase = new FirebaseRepository();

    public LiveData<List<GardenZone>> observeZones() {
        return firebase.observeGardenZones();
    }

    public LiveData<GardenZone> observeZone(String zoneId) {
        return firebase.observeGardenZone(zoneId);
    }

    public LiveData<List<FertilizerProduct>> observeProducts() {
        return firebase.observeFertilizerProducts();
    }

    public LiveData<List<FertilizerApplication>> observeHistory() {
        return firebase.observeFertilizerHistory();
    }

    public LiveData<List<FertilizerRecommendation>> observeRecommendations() {
        return firebase.observeFertilizerRecommendations();
    }

    public LiveData<List<FertilizerStageGuide>> observeStageGuides() {
        return firebase.observeFertilizerStageGuides();
    }

    public LiveData<WeatherForecast> observeWeather() {
        return firebase.observeWeatherForecast();
    }

    public Task<Void> updateWaterAnalysis(String zoneId, double ph, double ecMs) {
        return firebase.updateFertilizationWaterAnalysis(zoneId, ph, ecMs);
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
        return firebase.updateFertilizationProfile(
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
        return firebase.recordFertilizerApplicationSafely(
                zoneId, zoneName, product, appliedDose, appliedUnit, areaM2,
                tankLiters, recommendedDoseMin, recommendedDoseMax, deductStock,
                applicationMethod, notes, appliedAt, applicationType);
    }

    public Task<Void> recordBatches(
            List<FertilizerApplicationBatch> batches
    ) {
        List<FirebaseRepository.FertilizerApplicationBatch> values = new ArrayList<>();
        for (FertilizerApplicationBatch batch : batches) {
            values.add(new FirebaseRepository.FertilizerApplicationBatch(
                    batch.product,
                    toFirebaseApplications(batch.applications),
                    batch.appliedUnit,
                    batch.deductStock
            ));
        }
        return firebase.recordFertilizerApplicationBatchesSafely(values);
    }

    public Task<Void> recordBulk(
            FertilizerProduct product,
            List<BulkFertilizerApplication> applications,
            String appliedUnit,
            boolean deductStock
    ) {
        return firebase.recordBulkFertilizerApplicationsSafely(
                product, toFirebaseApplications(applications), appliedUnit, deductStock);
    }

    public Task<Void> saveProduct(FertilizerProduct product) {
        return firebase.saveFertilizerProduct(product);
    }

    public Task<List<String>> findActiveZonesUsingProduct(String productId) {
        return firebase.findActiveZonesUsingFertilizer(productId);
    }

    public Task<Void> removeProduct(FertilizerProduct product) {
        return product.isVerified()
                ? firebase.deactivateFertilizerProduct(product.getProduct_id())
                : firebase.deleteFertilizerProduct(product.getProduct_id());
    }

    public Task<Void> updateApplication(FertilizerApplication application) {
        return firebase.updateFertilizerApplicationSafely(application);
    }

    public Task<Void> deleteApplication(FertilizerApplication application) {
        return firebase.deleteFertilizerApplicationSafely(application);
    }

    public void loadNotificationSettings(Consumer<Map<String, Object>> consumer) {
        firebase.loadNotificationSettings(consumer);
    }

    public void loadPreferences(Consumer<Map<String, Object>> consumer) {
        firebase.loadFertilizationPreferences(consumer);
    }

    public Task<Void> saveNotificationSettings(Map<String, Object> values) {
        return firebase.saveNotificationSettings(values);
    }

    public Task<Void> savePreferences(Map<String, Object> values) {
        return firebase.saveFertilizationPreferences(values);
    }

    private static List<FirebaseRepository.BulkFertilizerApplication>
    toFirebaseApplications(List<BulkFertilizerApplication> values) {
        List<FirebaseRepository.BulkFertilizerApplication> result = new ArrayList<>();
        for (BulkFertilizerApplication value : values) {
            result.add(new FirebaseRepository.BulkFertilizerApplication(
                    value.zoneId, value.zoneName, value.appliedDose, value.areaM2,
                    value.tankLiters, value.recommendedDoseMin,
                    value.recommendedDoseMax, value.applicationMethod, value.notes,
                    value.appliedAt, value.applicationType, value.mixGroupId,
                    value.mixPartnerProductId, value.mixPartnerProductName,
                    value.mixRiskLevel
            ));
        }
        return result;
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

        public BulkFertilizerApplication(
                String zoneId, String zoneName, double appliedDose,
                double areaM2, double tankLiters, double recommendedDoseMin,
                double recommendedDoseMax, String applicationMethod,
                String notes, long appliedAt, String applicationType
        ) {
            this(zoneId, zoneName, appliedDose, areaM2, tankLiters,
                    recommendedDoseMin, recommendedDoseMax, applicationMethod,
                    notes, appliedAt, applicationType, "", "", "", "");
        }

        public BulkFertilizerApplication(
                String zoneId, String zoneName, double appliedDose,
                double areaM2, double tankLiters, double recommendedDoseMin,
                double recommendedDoseMax, String applicationMethod,
                String notes, long appliedAt, String applicationType,
                String mixGroupId, String mixPartnerProductId,
                String mixPartnerProductName, String mixRiskLevel
        ) {
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
            this.mixPartnerProductId =
                    mixPartnerProductId == null ? "" : mixPartnerProductId;
            this.mixPartnerProductName =
                    mixPartnerProductName == null ? "" : mixPartnerProductName;
            this.mixRiskLevel = mixRiskLevel == null ? "" : mixRiskLevel;
        }
    }

    public static final class FertilizerApplicationBatch {
        private final FertilizerProduct product;
        private final List<BulkFertilizerApplication> applications;
        private final String appliedUnit;
        private final boolean deductStock;

        public FertilizerApplicationBatch(
                FertilizerProduct product,
                List<BulkFertilizerApplication> applications,
                String appliedUnit,
                boolean deductStock
        ) {
            this.product = product;
            this.applications = applications;
            this.appliedUnit = appliedUnit;
            this.deductStock = deductStock;
        }
    }
}
