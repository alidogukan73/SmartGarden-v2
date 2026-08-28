package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.zones.ZoneLocalDataRepository;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Business boundary for zone inventory, hardware mapping and safe removal. */
public final class ZoneManagementViewModel extends AndroidViewModel {
    private final FirebaseRepository repository;
    private final ZoneLocalDataRepository localData;
    private final LiveData<List<GardenZone>> zones;
    private final LiveData<List<CropCatalogItem>> cropCatalogItems;

    public ZoneManagementViewModel(@NonNull Application application) {
        super(application);
        repository = new FirebaseRepository();
        localData = new ZoneLocalDataRepository(application);
        zones = repository.observeGardenZones();
        cropCatalogItems = repository.observeCropCatalogItems();
    }

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public LiveData<List<CropCatalogItem>> getCropCatalogItems() {
        return cropCatalogItems;
    }

    public Task<Void> saveZone(GardenZone zone, boolean createNewChannel) {
        return createNewChannel
                ? repository.createGardenZone(zone)
                : repository.saveGardenZone(zone);
    }

    /**
     * Firebase decides whether the zone can be deleted or must be archived.
     * Local records participate in the same decision and are removed only when
     * Firebase confirms that the empty test zone was physically deleted.
     */
    public Task<Boolean> deactivateZone(GardenZone zone) {
        String zoneId = zone == null || zone.getZone_id() == null
                ? "" : zone.getZone_id().trim();
        boolean hasLocalHistory = localData.hasMeaningfulHistory(zoneId);
        Task<Boolean> task = repository.deactivateGardenZone(zoneId, hasLocalHistory);
        task.addOnSuccessListener(deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                localData.removeEmptyZoneData(zoneId);
            }
        });
        return task;
    }
}
