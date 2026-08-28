package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.models.FertilizerProduct;
import com.google.android.gms.tasks.Task;

import java.util.List;

public final class FertilizerProductsViewModel extends ViewModel {
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
}
