package com.ali.smartgarden.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ali.smartgarden.R;
import com.ali.smartgarden.crop.CropCatalog;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.journal.LocalSeasonOutcomeStore;
import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenSeason;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.models.ZoneSeasonState;
import com.ali.smartgarden.season.SeasonRepository;
import com.ali.smartgarden.season.SeasonArchiveRepository;
import com.ali.smartgarden.season.SeasonScope;
import com.ali.smartgarden.season.SeasonStartConfiguration;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Owns season lifecycle decisions and side effects; the Activity only renders UI. */
public final class SeasonManagementViewModel extends AndroidViewModel {
    private final FirebaseRepository firebaseRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonArchiveRepository archiveRepository;
    private final LocalSeasonOutcomeStore outcomeStore;
    private final LocalGardenEventStore eventStore;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> bootstrapRequested = new HashSet<>();
    private final Set<String> autoSeasonRepairRequested = new HashSet<>();
    private final MutableLiveData<OneShotEvent<BootstrapNotice>> bootstrapNotices =
            new MutableLiveData<>();
    private final LiveData<List<GardenZone>> zones;
    private final LiveData<List<GardenSeason>> seasons;
    private final LiveData<List<CropCatalogItem>> cropCatalogItems;
    private List<GardenZone> latestZones = new ArrayList<>();
    private boolean bootstrapInProgress;
    private int bootstrapRetryCount;

    public SeasonManagementViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = new FirebaseRepository();
        seasonRepository = new SeasonRepository();
        archiveRepository = new SeasonArchiveRepository();
        outcomeStore = new LocalSeasonOutcomeStore(application);
        eventStore = new LocalGardenEventStore(application);
        zones = firebaseRepository.observeGardenZones();
        seasons = seasonRepository.observeAllSeasons();
        cropCatalogItems = firebaseRepository.observeCropCatalogItems();
    }

    public LiveData<List<GardenZone>> getZones() {
        return zones;
    }

    public LiveData<List<GardenSeason>> getSeasons() {
        return seasons;
    }

    public LiveData<List<CropCatalogItem>> getCropCatalogItems() {
        return cropCatalogItems;
    }

    public LiveData<OneShotEvent<BootstrapNotice>> getBootstrapNotices() {
        return bootstrapNotices;
    }

    public void synchronizeLegacySeasons(List<GardenZone> values) {
        latestZones = values == null ? new ArrayList<>() : new ArrayList<>(values);
        bootstrapNext();
    }

    private void bootstrapNext() {
        if (bootstrapInProgress) return;
        for (GardenZone zone : latestZones) {
            if (zone == null || safe(zone.getZone_id()).isBlank()
                    || ZoneCapacityPolicy.isInactive(zone)) {
                continue;
            }
            ZoneSeasonState state = zone.getSeason();
            if (SeasonScope.isModernAutoBootstrapCandidate(
                    state, zone.getCreated_at_epoch())
                    && autoSeasonRepairRequested.add(zone.getZone_id())) {
                bootstrapInProgress = true;
                seasonRepository.repairEmptyAutoStartedSeason(zone.getZone_id())
                        .addOnSuccessListener(repaired -> {
                            bootstrapInProgress = false;
                            if (Boolean.TRUE.equals(repaired)) {
                                emitNotice(R.string.season_auto_start_repaired, null);
                            }
                            bootstrapNext();
                        })
                        .addOnFailureListener(error -> {
                            bootstrapInProgress = false;
                            autoSeasonRepairRequested.remove(zone.getZone_id());
                            emitNotice(R.string.season_auto_start_repair_failed, error);
                        });
                return;
            }
            if (state != null && !safe(state.getStatus()).isBlank()) continue;
            if (!bootstrapRequested.add(zone.getZone_id())) continue;

            bootstrapInProgress = true;
            seasonRepository.bootstrapLegacySeason(zone)
                    .addOnSuccessListener(unused -> {
                        bootstrapInProgress = false;
                        bootstrapRetryCount = 0;
                        bootstrapNext();
                    })
                    .addOnFailureListener(error -> {
                        bootstrapInProgress = false;
                        bootstrapRequested.remove(zone.getZone_id());
                        if (transactionWasOverridden(error) && bootstrapRetryCount < 3) {
                            bootstrapRetryCount++;
                            handler.postDelayed(this::bootstrapNext,
                                    500L * bootstrapRetryCount);
                            return;
                        }
                        bootstrapRetryCount = 0;
                        emitNotice(R.string.season_bootstrap_failed,
                                transactionWasOverridden(error) ? null : error);
                    });
            return;
        }
    }

    public Task<Void> startSeason(
            GardenZone zone,
            String plantingDate,
            String growthStage,
            String label,
            SeasonStartConfiguration configuration
    ) {
        return seasonRepository.startSeason(
                zone, plantingDate, growthStage, label, configuration);
    }

    public Task<Boolean> canCancelNewSeason(String zoneId) {
        return seasonRepository.canCancelNewSeason(zoneId);
    }

    public Task<Void> cancelNewSeason(String zoneId) {
        return seasonRepository.cancelNewSeason(zoneId);
    }

    public Task<Void> closeSeason(
            GardenZone zone,
            ZoneSeasonState state,
            SeasonOutcome outcome
    ) {
        Task<Void> task = seasonRepository.closeSeason(zone.getZone_id(), outcome);
        task.addOnSuccessListener(ignored -> {
            outcomeStore.addForSeason(outcome);
            GardenEvent event = eventStore.addSystemForSeason(
                    zone.getZone_id(),
                    state.getActive_season_id(),
                    getApplication().getString(R.string.season_closed_event_title),
                    closeEventNote(outcome),
                    "season_closed:" + state.getActive_season_id()
            );
            firebaseRepository.saveGardenEvent(event);
        });
        return task;
    }

    public List<GardenSeason> visibleSeasonsFor(
            GardenZone zone,
            List<GardenSeason> allSeasons
    ) {
        return archiveRepository.visibleFor(zone, allSeasons);
    }

    public List<GardenSeason> completedArchives(
            List<GardenSeason> history,
            boolean requireRecordedActivity
    ) {
        return archiveRepository.completed(history, requireRecordedActivity);
    }

    public boolean hasRecordedArchive(String zoneId, List<GardenSeason> allSeasons) {
        return archiveRepository.hasRecorded(zoneId, allSeasons);
    }

    private String closeEventNote(SeasonOutcome outcome) {
        String note = safe(outcome.getResult());
        if (!safe(outcome.getHarvest_amount()).isBlank()) {
            note += " · " + getApplication().getString(
                    R.string.season_harvest_short, outcome.getHarvest_amount());
        }
        return note;
    }

    private boolean transactionWasOverridden(Exception error) {
        String message = error == null ? "" : safe(error.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("overridden") || message.contains("subsequent set");
    }

    private void emitNotice(int messageRes, Exception error) {
        bootstrapNotices.setValue(new OneShotEvent<>(new BootstrapNotice(messageRes, error)));
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class BootstrapNotice {
        public final int messageRes;
        public final Exception error;

        private BootstrapNotice(int messageRes, Exception error) {
            this.messageRes = messageRes;
            this.error = error;
        }
    }
}
