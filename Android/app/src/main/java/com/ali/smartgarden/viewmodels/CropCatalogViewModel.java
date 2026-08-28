package com.ali.smartgarden.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.crop.CropCatalog;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.season.SeasonStartConfiguration;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Owns crop catalogue persistence and protects archived season snapshots from UI edits. */
public final class CropCatalogViewModel extends ViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final LiveData<List<CropCatalogItem>> userItems =
            repository.observeCropCatalogItems();

    public LiveData<List<CropCatalogItem>> getUserItems() {
        return userItems;
    }

    public List<CropCatalogItem> getBuiltInItems() {
        return CropCatalog.builtIns();
    }

    public Task<Void> save(@Nullable CropCatalogItem existing, String name, String emoji,
                           int minimumMoisture, int maximumMoisture) {
        String effectiveEmoji = emoji == null ? "" : emoji.trim();
        if (effectiveEmoji.isBlank()) {
            effectiveEmoji = SeasonStartConfiguration.suggestedCropEmoji(name);
        }
        CropCatalogItem item = existing == null
                ? CropCatalog.newUserItem(name, effectiveEmoji,
                minimumMoisture, maximumMoisture)
                : existing;
        item.setName(name);
        item.setEmoji(effectiveEmoji);
        item.setPlant_type(SeasonStartConfiguration.customPlantType(name));
        item.setIdeal_moisture_min(minimumMoisture);
        item.setIdeal_moisture_max(maximumMoisture);
        return repository.saveCropCatalogItem(item);
    }

    public Task<Void> deactivate(String cropId) {
        return repository.deactivateCropCatalogItem(cropId);
    }
}
