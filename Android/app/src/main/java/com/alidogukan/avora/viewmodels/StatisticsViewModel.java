package com.alidogukan.avora.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.alidogukan.avora.R;
import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.language.AvoraLanguageManager;
import com.alidogukan.avora.models.Statistics;
import com.alidogukan.avora.models.GardenZone;

import java.util.List;

/** Lifecycle-aware statistics stream for the statistics screen. */
public class StatisticsViewModel extends AndroidViewModel {
    private final MediatorLiveData<Statistics> statistics = new MediatorLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final LiveData<List<GardenZone>> zones;

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        FirebaseRepository repository = new FirebaseRepository();
        zones = repository.observeGardenZones();
        LiveData<Statistics> source = repository.observeStatistics(databaseError ->
                error.setValue(AvoraLanguageManager.localizedContext(
                        getApplication()).getString(
                        R.string.statistics_read_error)));
        statistics.addSource(source, statistics::setValue);
    }

    public LiveData<Statistics> getStatistics() { return statistics; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<GardenZone>> getZones() { return zones; }
}
