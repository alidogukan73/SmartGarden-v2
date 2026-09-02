package com.alidogukan.avora.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.season.SeasonScope;

import java.util.List;

/** Zone stream and durable presentation choices for the plant list. */
public final class PlantListViewModel extends AndroidViewModel {
    private static final String PREFS = "plant_list_preferences";
    private static final String SORT_MODE = "sort_mode";
    private final SharedPreferences preferences;
    private final LiveData<List<GardenZone>> zones;

    public PlantListViewModel(@NonNull Application application) {
        super(application);
        preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        zones = new FirebaseRepository().observeGardenZones();
    }

    public LiveData<List<GardenZone>> getZones() { return zones; }
    public List<GardenZone> activeZones(List<GardenZone> values) {
        return SeasonScope.activeSeasonZones(values);
    }
    public int getSortMode(int fallback) { return preferences.getInt(SORT_MODE, fallback); }
    public void setSortMode(int value) { preferences.edit().putInt(SORT_MODE, value).apply(); }
}
