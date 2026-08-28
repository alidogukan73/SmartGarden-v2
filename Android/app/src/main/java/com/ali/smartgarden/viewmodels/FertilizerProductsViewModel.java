package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.fertilization.FertilizerAiAdvisor;
import com.ali.smartgarden.fertilization.FertilizerAiProfile;
import com.ali.smartgarden.fertilization.FertilizerStagePolicy;
import com.ali.smartgarden.models.FertilizerProduct;
import com.google.android.gms.tasks.Task;

import java.util.List;

public final class FertilizerProductsViewModel extends ViewModel {
    private static final String[] PRODUCT_STAGE_CODES = {
            FertilizerStagePolicy.SOIL_PREPARATION,
            FertilizerStagePolicy.ROOTING,
            FertilizerStagePolicy.VEGETATIVE,
            FertilizerStagePolicy.FLOWERING,
            FertilizerStagePolicy.FRUITING,
            FertilizerStagePolicy.HARVEST
    };
    private final FertilizationRepository repository = new FertilizationRepository();
    private final LiveData<List<FertilizerProduct>> products = repository.observeProducts();

    public LiveData<List<FertilizerProduct>> getProducts() {
        return products;
    }

    public Task<Void> saveProduct(FertilizerProduct product) {
        return repository.saveProduct(product);
    }

    public Task<List<String>> findActiveZonesUsingProduct(String productId) {
        return repository.findActiveZonesUsingProduct(productId);
    }

    public Task<Void> removeProduct(FertilizerProduct product) {
        return repository.removeProduct(product);
    }

    public String[] stageCodes() { return PRODUCT_STAGE_CODES.clone(); }
    public List<String> effectiveStages(FertilizerProduct product) {
        return FertilizerStagePolicy.effectiveStages(product);
    }
    public ProductGuidance guidanceFor(FertilizerProduct product) {
        FertilizerAiProfile profile = FertilizerAiAdvisor.profileFor(
                product == null ? new FertilizerProduct() : product);
        return new ProductGuidance(profile.getSuitability(), profile.getReason(),
                profile.getFruitStageAdvice(), profile.getSafetyNote());
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
