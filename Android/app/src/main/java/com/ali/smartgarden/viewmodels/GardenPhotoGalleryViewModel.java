package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Local photo persistence boundary for the gallery. */
public final class GardenPhotoGalleryViewModel extends AndroidViewModel {
    private final LocalGardenPhotoStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GardenPhotoGalleryViewModel(@NonNull Application application) {
        super(application);
        store = new LocalGardenPhotoStore(application);
    }

    public List<GardenPhoto> load() { return store.load(); }
    public boolean delete(GardenPhoto photo) { return store.delete(photo); }
    public void deleteSelected(List<GardenPhoto> photos, Set<String> selectedIds,
                               Consumer<Integer> completed) {
        List<GardenPhoto> photoCopy = new ArrayList<>(photos);
        Set<String> selectedCopy = new HashSet<>(selectedIds);
        executor.execute(() -> {
            int deleted = 0;
            for (GardenPhoto photo : photoCopy) {
                if (selectedCopy.contains(photo.getId()) && store.delete(photo)) deleted++;
            }
            completed.accept(deleted);
        });
    }

    @Override protected void onCleared() {
        executor.shutdown();
    }
}
