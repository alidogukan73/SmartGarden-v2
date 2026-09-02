package com.alidogukan.avora.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.models.GardenPhoto;
import com.alidogukan.avora.photos.LocalGardenPhotoStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combines shared growth metadata with private photos available on this device. */
public final class PlantGrowthTrackingViewModel extends AndroidViewModel {
    private final LiveData<List<GardenPhoto>> photoMetadata;
    private final LocalGardenPhotoStore localPhotos;

    public PlantGrowthTrackingViewModel(@NonNull Application application) {
        super(application);
        FirebaseRepository repository = new FirebaseRepository();
        photoMetadata = repository.observeGardenPhotoMetadata();
        localPhotos = new LocalGardenPhotoStore(application);
    }

    public LiveData<List<GardenPhoto>> getPhotoMetadata() { return photoMetadata; }

    public List<GardenPhoto> recordsForZone(List<GardenPhoto> cloud, String zoneId) {
        Map<String, GardenPhoto> combined = new LinkedHashMap<>();
        if (cloud != null) {
            for (GardenPhoto photo : cloud) {
                if (photo != null && photo.getId() != null) combined.put(photo.getId(), photo);
            }
        }
        for (GardenPhoto local : localPhotos.load()) {
            if (local == null || local.getId() == null) continue;
            GardenPhoto shared = combined.get(local.getId());
            if (shared == null) {
                combined.put(local.getId(), local);
                continue;
            }
            shared.setLocal_path(local.getLocal_path());
            if (!safe(local.getAnalysis_goal()).isEmpty()) copyAnalysis(local, shared);
        }
        List<GardenPhoto> records = new ArrayList<>();
        for (GardenPhoto photo : combined.values()) {
            if ("growth_status".equals(photo.getAnalysis_goal())
                    && safe(zoneId).equals(safe(photo.getZone_id()))
                    && photo.getGrowth_score() >= 0
                    && photo.getGrowth_score() <= 100) {
                records.add(photo);
            }
        }
        records.sort((left, right) -> Long.compare(
                right.getCaptured_at_epoch(), left.getCaptured_at_epoch()));
        return records;
    }

    private static void copyAnalysis(GardenPhoto source, GardenPhoto target) {
        target.setAnalysis_title(source.getAnalysis_title());
        target.setAnalysis_meta(source.getAnalysis_meta());
        target.setAnalysis_context(source.getAnalysis_context());
        target.setAnalysis_advice(source.getAnalysis_advice());
        target.setAnalysis_goal(source.getAnalysis_goal());
        target.setAnalysis_confidence(source.getAnalysis_confidence());
        target.setGrowth_score(source.getGrowth_score());
        target.setGrowth_stage(source.getGrowth_stage());
        target.setGrowth_trend(source.getGrowth_trend());
        target.setGrowth_score_delta(source.getGrowth_score_delta());
        target.setGrowth_signals(source.getGrowth_signals());
        target.setGrowth_previous_captured_at_epoch(
                source.getGrowth_previous_captured_at_epoch());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
