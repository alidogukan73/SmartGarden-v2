package com.alidogukan.avora.season;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.alidogukan.avora.firebase.FirebaseLiveData;
import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.SeasonOutcome;
import com.alidogukan.avora.models.SeasonStatus;
import com.alidogukan.avora.models.ZoneSeasonState;
import com.alidogukan.avora.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns the lifecycle of zone seasons. All lifecycle mutations are atomic and
 * preserve hardware configuration, inventory, calibration and historical data.
 */
public final class SeasonRepository {
    private static final String DEVICE_ID = "avora-001";
    private final DatabaseReference deviceRef = FirebaseDatabase.getInstance()
            .getReference("devices")
            .child(DEVICE_ID);
    private final DatabaseReference seasonsRef = deviceRef
            .child("garden_journal")
            .child("seasons");

    public LiveData<List<GardenSeason>> observeAllSeasons() {
        FirebaseLiveData<List<GardenSeason>> liveData = new FirebaseLiveData<>(seasonsRef);
        liveData.setEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GardenSeason> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GardenSeason value = child.getValue(GardenSeason.class);
                    if (value == null) continue;
                    if (value.getSeason_id().isBlank()) {
                        value.setSeason_id(child.getKey());
                    }
                    values.add(value);
                }
                values.sort(Comparator.comparingLong(GardenSeason::getStarted_at_epoch).reversed());
                liveData.setValue(values);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                liveData.setValue(new ArrayList<>());
            }
        });
        return liveData;
    }

    public LiveData<List<GardenSeason>> observeZoneSeasons(String zoneId) {
        com.google.firebase.database.Query query = seasonsRef
                .orderByChild("zone_id")
                .equalTo(safe(zoneId));
        FirebaseLiveData<List<GardenSeason>> liveData = new FirebaseLiveData<>(query);
        liveData.setEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GardenSeason> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GardenSeason value = child.getValue(GardenSeason.class);
                    if (value == null) continue;
                    if (value.getSeason_id().isBlank()) value.setSeason_id(child.getKey());
                    values.add(value);
                }
                values.sort(Comparator.comparingLong(GardenSeason::getStarted_at_epoch).reversed());
                liveData.setValue(values);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                liveData.setValue(new ArrayList<>());
            }
        });
        return liveData;
    }

    /** Creates the one-time compatibility season only when no season state exists. */
    public Task<Void> bootstrapLegacySeason(GardenZone zone) {
        if (zone == null || safe(zone.getZone_id()).isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Bölge bilgisi gerekli."));
        }
        long now = nowEpoch();
        String zoneId = zone.getZone_id();

        /*
         * Do not run a transaction on the complete device root here. The Raspberry Pi
         * continuously updates sensor and health children below that root; Firebase
         * therefore cancels a root transaction as OVERRIDDEN_BY_SET and the UI remains
         * in PREPARING forever. A single multi-location update still creates the state,
         * manifest and AI link atomically, while unrelated live measurements may continue.
         */
        return deviceRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Exception error = task.getException();
                return Tasks.forException(error == null
                        ? new IllegalStateException("İlk sezon hazırlanamadı.")
                        : error);
            }

            DataSnapshot root = task.getResult();
            DataSnapshot zoneData = root.child("zones").child(zoneId);
            String existingStatus = stringValue(zoneData.child("season").child("status"));
            if (!existingStatus.isBlank()) return Tasks.forResult(null);

            String plantingDate = stringValue(zoneData
                    .child("fertilization")
                    .child("planting_date"));
            long startedAt = inferLegacySeasonStart(root, zoneId, now);
            String seasonId = SeasonScope.createSeasonId(zoneId, startedAt);
            String label = seasonLabel(zone, startedAt);
            String statePath = "zones/" + zoneId + "/season/";
            String manifestPath = "garden_journal/seasons/" + seasonId + "/";

            Map<String, Object> updates = new HashMap<>();
            updates.put(statePath + "active_season_id", seasonId);
            updates.put(statePath + "status", SeasonStatus.ACTIVE);
            updates.put(statePath + "label", label);
            updates.put(statePath + "started_at_epoch", startedAt);
            updates.put(statePath + "ended_at_epoch", 0L);
            updates.put(statePath + "include_legacy_records", true);
            updates.put(statePath + "updated_at_epoch", now);

            updates.put(manifestPath + "season_id", seasonId);
            updates.put(manifestPath + "zone_id", zoneId);
            updates.put(manifestPath + "zone_name", safe(zone.getName()));
            updates.put(manifestPath + "plant_type", safe(zone.getPlant_type()));
            updates.put(manifestPath + "emoji", safe(zone.getEmoji()));
            updates.put(manifestPath + "sensor_id", safe(zone.getSensor_id()));
            updates.put(manifestPath + "sensor_enabled", zone.isSensor_enabled());
            updates.put(manifestPath + "valve_id", safe(zone.getValve_id()));
            updates.put(manifestPath + "valve_mode", safe(zone.getValve_mode()));
            updates.put(manifestPath + "label", label);
            updates.put(manifestPath + "status", SeasonStatus.ACTIVE);
            updates.put(manifestPath + "planting_date", plantingDate);
            updates.put(manifestPath + "started_at_epoch", startedAt);
            updates.put(manifestPath + "ended_at_epoch", 0L);
            updates.put(manifestPath + "includes_legacy_records", true);
            updates.put(manifestPath + "created_at_epoch", now);
            updates.put(manifestPath + "updated_at_epoch", now);

            updates.put("zones/" + zoneId + "/ai/season_id", seasonId);
            updates.put("zones/" + zoneId + "/ai/season_status", SeasonStatus.ACTIVE);
            updates.put("zones/" + zoneId + "/ai/season_started_at_epoch", startedAt);
            updates.put("zones/" + zoneId + "/ai/season_closed_at_epoch", 0L);
            return deviceRef.updateChildren(updates);
        });
    }

    /**
     * Repairs the old compatibility behavior that could create an active legacy
     * season immediately after a modern zone was added. Only a completely empty,
     * automatically generated manifest is removed; user-started and recorded
     * seasons are never eligible.
     */
    public Task<Boolean> repairEmptyAutoStartedSeason(String zoneId) {
        if (safe(zoneId).isBlank()) return Tasks.forResult(false);
        return deviceRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Exception error = task.getException();
                return Tasks.forException(error == null
                        ? new IllegalStateException("Sezon verileri okunamadı.")
                        : error);
            }

            DataSnapshot root = task.getResult();
            DataSnapshot zoneData = root.child("zones").child(zoneId);
            ZoneSeasonState state = zoneData.child("season").getValue(ZoneSeasonState.class);
            long zoneCreatedAt = longValue(zoneData.child("created_at_epoch"));
            if (!SeasonScope.isModernAutoBootstrapCandidate(state, zoneCreatedAt)) {
                return Tasks.forResult(false);
            }

            String seasonId = state.getActive_season_id();
            DataSnapshot manifest = root.child("garden_journal")
                    .child("seasons")
                    .child(seasonId);
            boolean generatedLegacyManifest = manifest.exists()
                    && SeasonStatus.isActive(stringValue(manifest.child("status")))
                    && booleanValue(manifest.child("includes_legacy_records"))
                    && !manifest.child("cancellation_snapshot").exists();
            if (!generatedLegacyManifest) return Tasks.forResult(false);

            SeasonCounts counts = calculateCounts(root, zoneId, state, false);
            boolean hasFieldRecords = counts.wateringCount > 0
                    || counts.fertilizerCount > 0
                    || counts.eventCount > 0
                    || counts.photoCount > 0
                    || counts.analysisCount > 0;
            String growthStage = stringValue(zoneData
                    .child("fertilization")
                    .child("growth_stage"));
            boolean untouchedStage = growthStage.isBlank()
                    || "SOIL_PREPARATION".equalsIgnoreCase(growthStage);
            if (hasFieldRecords || !untouchedStage || isIrrigationBusy(root)) {
                return Tasks.forResult(false);
            }

            long now = nowEpoch();
            String zonePath = "zones/" + zoneId + "/";
            Map<String, Object> updates = new HashMap<>();
            updates.put(zonePath + "season/active_season_id", "");
            updates.put(zonePath + "season/status", SeasonStatus.CLOSED);
            updates.put(zonePath + "season/label", "");
            updates.put(zonePath + "season/started_at_epoch", 0L);
            updates.put(zonePath + "season/ended_at_epoch", 0L);
            updates.put(zonePath + "season/include_legacy_records", false);
            updates.put(zonePath + "season/updated_at_epoch", now);
            updates.put(zonePath + "ai/season_id", "");
            updates.put(zonePath + "ai/season_status", SeasonStatus.CLOSED);
            updates.put(zonePath + "ai/season_started_at_epoch", 0L);
            updates.put(zonePath + "ai/season_closed_at_epoch", 0L);
            updates.put("garden_journal/seasons/" + seasonId, null);

            return deviceRef.updateChildren(updates).continueWithTask(writeTask -> {
                if (writeTask.isSuccessful()) return Tasks.forResult(true);
                Exception error = writeTask.getException();
                return Tasks.forException(error == null
                        ? new IllegalStateException("Boş sezon düzeltilemedi.")
                        : error);
            });
        });
    }

    public Task<Void> startSeason(
            GardenZone zone,
            String plantingDate,
            String growthStage,
            String requestedLabel
    ) {
        return startSeason(
                zone,
                plantingDate,
                growthStage,
                requestedLabel,
                SeasonStartConfiguration.fromZone(zone)
        );
    }

    public Task<Void> startSeason(
            GardenZone zone,
            String plantingDate,
            String growthStage,
            String requestedLabel,
            SeasonStartConfiguration requestedConfiguration
    ) {
        if (zone == null || safe(zone.getZone_id()).isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Bölge bilgisi gerekli."));
        }
        SeasonStartConfiguration configuration = requestedConfiguration == null
                ? SeasonStartConfiguration.fromZone(zone)
                : requestedConfiguration;
        if (!configuration.isValid()) {
            return Tasks.forException(new IllegalArgumentException("Yeni sezon için ürün bilgisi gerekli."));
        }
        FirebaseDatabase.getInstance().goOnline();
        return deviceRef.get()
                .continueWithTask(readTask -> {
                    if (!readTask.isSuccessful() || readTask.getResult() == null) {
                        Exception error = readTask.getException();
                        return Tasks.forException(error == null
                                ? new IllegalStateException("Yeni sezon için bölge verileri okunamadı.")
                                : error);
                    }
                    return startSeasonFromSnapshot(
                            zone,
                            safe(plantingDate),
                            growthStage,
                            requestedLabel,
                            configuration,
                            readTask.getResult()
                    );
                });
    }

    /**
     * Starts a season with one atomic multi-location update. A transaction on the
     * complete device root cannot be used because Raspberry Pi measurements update
     * that root continuously and Firebase cancels the transaction as overridden.
     */
    private Task<Void> startSeasonFromSnapshot(
            GardenZone zone,
            String plantingDate,
            String growthStage,
            String requestedLabel,
            SeasonStartConfiguration configuration,
            DataSnapshot root
    ) {
        String zoneId = zone.getZone_id();
        DataSnapshot zoneData = root.child("zones").child(zoneId);
        if (!zoneData.exists()) {
            return Tasks.forException(new IllegalStateException("Bölge bulunamadı."));
        }

        GardenZone persistedZone = zoneData.getValue(GardenZone.class);
        if (ZoneCapacityPolicy.isInactive(persistedZone)) {
            return Tasks.forException(new IllegalStateException(
                    "Kullanım dışı bölge için yeni sezon başlatılamaz. Bölgeyi önce yeniden etkinleştirin."
            ));
        }

        ZoneSeasonState current = zoneData.child("season").getValue(ZoneSeasonState.class);
        if (!SeasonScope.canStart(current)) {
            return Tasks.forException(new IllegalStateException(
                    "Bu bölgede açık bir sezon zaten var."
            ));
        }

        long now = nowEpoch();
        String seasonId = SeasonScope.createSeasonId(zoneId, now);
        String label = safe(requestedLabel).trim();
        if (label.isBlank()) label = seasonLabel(zone, now);
        String zonePath = "zones/" + zoneId + "/";
        String manifestPath = "garden_journal/seasons/" + seasonId + "/";
        Map<String, Object> updates = new HashMap<>();

        updates.put(zonePath + "name", configuration.getCropName());
        updates.put(zonePath + "plant_type", configuration.getPlantType());
        updates.put(zonePath + "emoji", configuration.getEmoji());

        putNewSeasonState(updates, zonePath, seasonId, label, now);
        putNewSeasonManifest(
                updates,
                manifestPath,
                zoneData,
                configuration,
                seasonId,
                label,
                plantingDate,
                now
        );
        putNewSeasonFertilization(updates, zonePath, plantingDate, growthStage, now);
        putNewSeasonIrrigationState(updates, zonePath, now);
        putNewSeasonAiState(updates, zonePath, seasonId, now);
        return deviceRef.updateChildren(updates);
    }

    private static void putNewSeasonState(
            Map<String, Object> updates,
            String zonePath,
            String seasonId,
            String label,
            long now
    ) {
        String state = zonePath + "season/";
        updates.put(state + "active_season_id", seasonId);
        updates.put(state + "status", SeasonStatus.ACTIVE);
        updates.put(state + "label", label);
        updates.put(state + "started_at_epoch", now);
        updates.put(state + "ended_at_epoch", 0L);
        updates.put(state + "include_legacy_records", false);
        updates.put(state + "updated_at_epoch", now);
    }

    private static void putNewSeasonManifest(
            Map<String, Object> updates,
            String path,
            DataSnapshot zoneData,
            SeasonStartConfiguration configuration,
            String seasonId,
            String label,
            String plantingDate,
            long now
    ) {
        updates.put(path + "season_id", seasonId);
        updates.put(path + "zone_id", safe(zoneData.getKey()));
        updates.put(path + "zone_name", configuration.getCropName());
        updates.put(path + "plant_type", configuration.getPlantType());
        updates.put(path + "emoji", configuration.getEmoji());
        updates.put(path + "sensor_id", stringValue(zoneData.child("sensor_id")));
        updates.put(path + "sensor_enabled", booleanValue(zoneData.child("sensor_enabled")));
        updates.put(path + "valve_id", stringValue(zoneData.child("valve_id")));
        updates.put(path + "valve_mode", stringValue(zoneData.child("valve_mode")));
        updates.put(path + "label", label);
        updates.put(path + "status", SeasonStatus.ACTIVE);
        updates.put(path + "planting_date", plantingDate);
        updates.put(path + "started_at_epoch", now);
        updates.put(path + "ended_at_epoch", 0L);
        updates.put(path + "includes_legacy_records", false);
        updates.put(path + "created_at_epoch", now);
        updates.put(path + "updated_at_epoch", now);
        putCancellationSnapshot(updates, path, zoneData);
    }

    /**
     * Keeps only the pre-season configuration needed to undo an untouched new season.
     * Historical records are never copied, moved or deleted by this snapshot.
     */
    private static void putCancellationSnapshot(
            Map<String, Object> updates,
            String manifestPath,
            DataSnapshot zoneData
    ) {
        String path = manifestPath + "cancellation_snapshot/";
        updates.put(path + "season", zoneData.child("season").getValue());
        updates.put(path + "fertilization", zoneData.child("fertilization").getValue());
        updates.put(path + "ai", zoneData.child("ai").getValue());

        String[] fields = {
                "name", "plant_type", "emoji", "sensor_id", "sensor_enabled",
                "sensor_config_updated_at_epoch", "irrigation_enabled", "moisture",
                "raw", "voltage", "rssi", "updated_at_epoch"
        };
        for (String field : fields) {
            updates.put(path + "zone/" + field, zoneData.child(field).getValue());
        }
    }

    private static void putNewSeasonFertilization(
            Map<String, Object> updates,
            String zonePath,
            String plantingDate,
            String growthStage,
            long now
    ) {
        String path = zonePath + "fertilization/";
        updates.put(path + "planting_date", plantingDate);
        updates.put(path + "growth_stage", normalizedGrowthStage(growthStage));
        updates.put(path + "enabled", false);
        updates.put(path + "reminder_enabled", false);
        updates.put(path + "active_plan_id", "");
        updates.put(path + "active_product_id", "");
        updates.put(path + "next_application_at_epoch", 0L);
        updates.put(path + "last_application_at_epoch", 0L);
        updates.put(path + "application_schedules", null);
        updates.put(path + "updated_at_epoch", now);
    }

    private static void putNewSeasonIrrigationState(
            Map<String, Object> updates,
            String zonePath,
            long now
    ) {
        String path = zonePath + "irrigation_status/";
        updates.put(path + "decision", "WAIT");
        updates.put(path + "decision_reason", "NEW_SEASON_STARTED");
        updates.put(path + "cooldown_active", false);
        updates.put(path + "cooldown_remaining", 0);
        updates.put(path + "queue_position", 0);
        updates.put(path + "selected_for_watering", false);
        updates.put(path + "watering_active", false);
        updates.put(path + "completed_watering_cycles", 0);
        updates.put(path + "waiting_for_moisture_recovery", false);
        updates.put(path + "updated_at_epoch", now);
    }

    private static void putNewSeasonAiState(
            Map<String, Object> updates,
            String zonePath,
            String seasonId,
            long now
    ) {
        String path = zonePath + "ai/";
        updates.put(path + "decision", null);
        updates.put(path + "explanation", null);
        updates.put(path + "moisture_prediction", null);
        updates.put(path + "prediction_accuracy", null);
        updates.put(path + "confidence", null);
        updates.put(path + "adaptive_recommendation", null);
        updates.put(path + "prediction_validation", null);
        updates.put(path + "season_id", seasonId);
        updates.put(path + "season_status", SeasonStatus.ACTIVE);
        updates.put(path + "season_started_at_epoch", now);
        updates.put(path + "season_closed_at_epoch", 0L);
        updates.put(path + "learning_status", "LEARNING");
        updates.put(path + "updated_at_epoch", now);
    }
    public Task<Boolean> canCancelNewSeason(String zoneId) {
        if (safe(zoneId).isBlank()) return Tasks.forResult(false);
        return deviceRef.get().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                return false;
            }
            return evaluateCancellation(zoneId, task.getResult()).allowed;
        });
    }

    public Task<Void> cancelNewSeason(String zoneId) {
        if (safe(zoneId).isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Bölge bilgisi gerekli."));
        }
        FirebaseDatabase.getInstance().goOnline();
        return deviceRef.get()
                .continueWithTask(readTask -> {
                    if (!readTask.isSuccessful() || readTask.getResult() == null
                            || !readTask.getResult().exists()) {
                        Exception error = readTask.getException();
                        return Tasks.forException(error == null
                                ? new IllegalStateException("Sezon verileri okunamadı.")
                                : error);
                    }
                    return cancelNewSeasonFromSnapshot(zoneId, readTask.getResult());
                });
    }

    private Task<Void> cancelNewSeasonFromSnapshot(String zoneId, DataSnapshot root) {
        CancellationCheck check = evaluateCancellation(zoneId, root);
        if (!check.allowed) {
            return Tasks.forException(new IllegalStateException(check.message));
        }

        DataSnapshot zoneData = root.child("zones").child(zoneId);
        ZoneSeasonState state = zoneData.child("season").getValue(ZoneSeasonState.class);
        if (state == null) {
            return Tasks.forException(new IllegalStateException("İptal edilecek sezon bulunamadı."));
        }

        long now = nowEpoch();
        String seasonId = state.getActive_season_id();
        String zonePath = "zones/" + zoneId + "/";
        DataSnapshot snapshot = root.child("garden_journal")
                .child("seasons")
                .child(seasonId)
                .child("cancellation_snapshot");
        Map<String, Object> updates = new HashMap<>();

        restorePriorSeasonOrClose(updates, zonePath, snapshot, state, now);
        restoreSnapshotObject(updates, zonePath + "fertilization", snapshot.child("fertilization"));
        restoreSnapshotObject(updates, zonePath + "ai", snapshot.child("ai"));
        restoreZoneFields(updates, zonePath, snapshot.child("zone"));
        putCancelledIrrigationState(updates, zonePath, now);
        updates.put(zonePath + "irrigation_enabled", false);

        // Only the untouched, currently active manifest is removed. Closed archives remain intact.
        updates.put("garden_journal/seasons/" + seasonId, null);
        return deviceRef.updateChildren(updates);
    }

    private static CancellationCheck evaluateCancellation(String zoneId, DataSnapshot root) {
        DataSnapshot zoneData = root.child("zones").child(zoneId);
        ZoneSeasonState state = zoneData.child("season").getValue(ZoneSeasonState.class);
        if (state == null || !state.isActive() || safe(state.getActive_season_id()).isBlank()) {
            return CancellationCheck.blocked("İptal edilecek aktif sezon bulunamadı.");
        }

        DataSnapshot manifest = root.child("garden_journal")
                .child("seasons")
                .child(state.getActive_season_id());
        if (!manifest.child("cancellation_snapshot").exists()) {
            return CancellationCheck.blocked(
                    "Bu sezon güvenli iptal desteğinden önce açıldığı için silinemez."
            );
        }

        String growthStage = stringValue(zoneData.child("fertilization").child("growth_stage"));
        SeasonCounts counts = calculateCounts(root, zoneId, state, false);
        boolean hasSeasonRecords = counts.wateringCount > 0
                || counts.fertilizerCount > 0
                || counts.eventCount > 0
                || counts.photoCount > 0
                || counts.analysisCount > 0;
        boolean irrigationBusy = isIrrigationBusy(root);
        if (SeasonScope.canCancelNewSeason(
                state,
                growthStage,
                hasSeasonRecords,
                irrigationBusy
        )) {
            return CancellationCheck.allowed();
        }
        if (state.isInclude_legacy_records()) {
            return CancellationCheck.blocked("Arşivlenmiş veya eski kayıtları içeren sezon silinemez.");
        }
        if (!"SOIL_PREPARATION".equalsIgnoreCase(safe(growthStage).trim())) {
            return CancellationCheck.blocked(
                    "Yeni sezon yalnız Toprak hazırlığı dönemindeyken iptal edilebilir."
            );
        }
        if (hasSeasonRecords) {
            return CancellationCheck.blocked(
                    "Bu sezonda saha kaydı bulunduğu için sezon artık silinemez."
            );
        }
        return CancellationCheck.blocked(
                "Sulama, pompa, vana veya sulama kuyruğu çalışırken sezon iptal edilemez."
        );
    }

    private static boolean isIrrigationBusy(DataSnapshot root) {
        if (booleanValue(root.child("status").child("relay"))
                || booleanValue(root.child("status").child("valve_open"))
                || booleanValue(root.child("commands").child("relay"))
                || booleanValue(root.child("irrigation_hardware").child("valve_open"))
                || root.child("irrigation_runtime").child("pending_waterings").hasChildren()) {
            return true;
        }
        for (DataSnapshot zone : root.child("zones").getChildren()) {
            DataSnapshot status = zone.child("irrigation_status");
            if (booleanValue(status.child("watering_active"))
                    || booleanValue(status.child("selected_for_watering"))
                    || longValue(status.child("queue_position")) > 0L) {
                return true;
            }
        }
        return false;
    }

    private static void restorePriorSeasonOrClose(
            Map<String, Object> updates,
            String zonePath,
            DataSnapshot snapshot,
            ZoneSeasonState cancelledState,
            long now
    ) {
        DataSnapshot previous = snapshot.child("season");
        if (previous.exists() && previous.getValue() != null) {
            updates.put(zonePath + "season", previous.getValue());
            return;
        }
        String path = zonePath + "season/";
        updates.put(path + "active_season_id", "");
        updates.put(path + "status", SeasonStatus.CLOSED);
        updates.put(path + "label", cancelledState.getLabel());
        updates.put(path + "started_at_epoch", 0L);
        updates.put(path + "ended_at_epoch", 0L);
        updates.put(path + "include_legacy_records", false);
        updates.put(path + "updated_at_epoch", now);
    }

    private static void restoreSnapshotObject(
            Map<String, Object> updates,
            String targetPath,
            DataSnapshot snapshot
    ) {
        if (snapshot.exists() && snapshot.getValue() != null) {
            updates.put(targetPath, snapshot.getValue());
        }
    }

    private static void restoreZoneFields(
            Map<String, Object> updates,
            String zonePath,
            DataSnapshot snapshot
    ) {
        String[] fields = {
                "name", "plant_type", "emoji", "sensor_id", "sensor_enabled",
                "sensor_config_updated_at_epoch", "moisture", "raw", "voltage",
                "rssi", "updated_at_epoch"
        };
        for (String field : fields) {
            if (snapshot.hasChild(field)) {
                updates.put(zonePath + field, snapshot.child(field).getValue());
            }
        }
    }

    private static void putCancelledIrrigationState(
            Map<String, Object> updates,
            String zonePath,
            long now
    ) {
        String path = zonePath + "irrigation_status/";
        updates.put(path + "decision", "WAIT");
        updates.put(path + "decision_reason", "SEASON_CANCELLED");
        updates.put(path + "cooldown_active", false);
        updates.put(path + "cooldown_remaining", 0);
        updates.put(path + "queue_position", 0);
        updates.put(path + "selected_for_watering", false);
        updates.put(path + "watering_active", false);
        updates.put(path + "waiting_for_moisture_recovery", false);
        updates.put(path + "updated_at_epoch", now);
    }
    public Task<Void> closeSeason(String zoneId, SeasonOutcome outcome) {
        if (safe(zoneId).isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Bölge bilgisi gerekli."));
        }
        long now = nowEpoch();
        return deviceRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Exception error = task.getException();
                return Tasks.forException(error == null
                        ? new IllegalStateException("Sezon verileri okunamadı.")
                        : error);
            }
            return closeSeasonFromSnapshot(zoneId, outcome, now, task.getResult());
        });
    }

    private Task<Void> closeSeasonFromSnapshot(
            String zoneId,
            SeasonOutcome outcome,
            long now,
            DataSnapshot root
    ) {
        DataSnapshot zoneData = root.child("zones").child(zoneId);
        ZoneSeasonState state = zoneData.child("season").getValue(ZoneSeasonState.class);
        if (state == null || !state.isActive() || safe(state.getActive_season_id()).isBlank()) {
            return Tasks.forException(new IllegalStateException("Kapatılacak aktif sezon bulunamadı."));
        }

        if (!SeasonScope.canClose(state, isIrrigationBusy(root))) {
            return Tasks.forException(new IllegalStateException(
                    "Sulama, pompa, vana veya sulama kuyruğu çalışırken sezon kapatılamaz."
            ));
        }

        String seasonId = state.getActive_season_id();
        boolean pendingFinalMeasurement = hasPendingWateringForSeason(root, zoneId, seasonId);
        SeasonCounts counts = calculateCounts(root, zoneId, state, pendingFinalMeasurement);
        String manifest = "garden_journal/seasons/" + seasonId + "/";
        String zone = "zones/" + zoneId + "/";
        Map<String, Object> updates = new HashMap<>();

        updates.put(manifest + "snapshots/ai", zoneData.child("ai").getValue());
        updates.put(manifest + "snapshots/irrigation_status",
                zoneData.child("irrigation_status").getValue());
        updates.put(manifest + "snapshots/fertilization",
                zoneData.child("fertilization").getValue());
        putSnapshotSetting(updates, manifest, zoneData, "area_m2");
        putSnapshotSetting(updates, manifest, zoneData, "tank_volume_l");
        putSnapshotSetting(updates, manifest, zoneData, "moisture_limit");
        putSnapshotSetting(updates, manifest, zoneData, "pump_duration");
        putSnapshotSetting(updates, manifest, zoneData, "cooldown_seconds");
        putSnapshotSetting(updates, manifest, zoneData, "restart_delta");
        updates.put(manifest + "snapshots/zone_settings/captured_at_epoch", now);

        updates.put(manifest + "status", SeasonStatus.CLOSED);
        updates.put(manifest + "ended_at_epoch", now);
        updates.put(manifest + "watering_count", counts.wateringCount);
        updates.put(manifest + "watering_seconds", counts.wateringSeconds);
        updates.put(manifest + "fertilizer_application_count", counts.fertilizerCount);
        updates.put(manifest + "journal_event_count", counts.eventCount);
        updates.put(manifest + "manual_journal_event_count", counts.eventCount);
        updates.put(manifest + "photo_count", counts.photoCount);
        updates.put(manifest + "plant_assistant_analysis_count", counts.analysisCount);
        updates.put(manifest + "notification_count", counts.notificationCount);
        updates.put(manifest + "pending_final_measurement", pendingFinalMeasurement);
        updates.put(manifest + "final_moisture", (int) longValue(zoneData.child("moisture")));
        updates.put(manifest + "final_sensor_updated_at_epoch",
                longValue(zoneData.child("updated_at_epoch")));
        putOutcome(updates, manifest, outcome, seasonId, zoneId, now);
        updates.put(manifest + "updated_at_epoch", now);

        putSeasonState(updates, zone, state, now);
        updates.put(zone + "irrigation_enabled", false);
        updates.put(zone + "fertilization/enabled", false);
        updates.put(zone + "fertilization/reminder_enabled", false);
        updates.put(zone + "fertilization/next_application_at_epoch", 0L);
        updates.put(zone + "fertilization/updated_at_epoch", now);
        updates.put(zone + "ai/season_status", SeasonStatus.CLOSED);
        updates.put(zone + "ai/season_closed_at_epoch", now);
        updates.put(zone + "ai/updated_at_epoch", now);

        String outcomePath = "garden_journal/season_outcomes/" + seasonId + "/";
        putOutcome(updates, outcomePath, outcome, seasonId, zoneId, now);
        updates.put(outcomePath + "id", seasonId);
        return deviceRef.updateChildren(updates);
    }

    private static void putSnapshotSetting(
            Map<String, Object> updates,
            String manifest,
            DataSnapshot zoneData,
            String key
    ) {
        updates.put(manifest + "snapshots/zone_settings/" + key,
                zoneData.child(key).getValue());
    }

    private static void putSeasonState(
            Map<String, Object> updates,
            String zone,
            ZoneSeasonState state,
            long now
    ) {
        updates.put(zone + "season/active_season_id", state.getActive_season_id());
        updates.put(zone + "season/status", SeasonStatus.CLOSED);
        updates.put(zone + "season/label", state.getLabel());
        updates.put(zone + "season/started_at_epoch", state.getStarted_at_epoch());
        updates.put(zone + "season/ended_at_epoch", now);
        updates.put(zone + "season/include_legacy_records", state.isInclude_legacy_records());
        updates.put(zone + "season/updated_at_epoch", now);
    }

    public Task<String> resolveActiveSeasonId(String zoneId) {
        if (safe(zoneId).isBlank()) return Tasks.forResult("");
        return deviceRef.child("zones").child(zoneId).child("season").get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return "";
                    ZoneSeasonState state = task.getResult().getValue(ZoneSeasonState.class);
                    return state != null && state.isActive() ? state.getActive_season_id() : "";
                });
    }


    /**
     * Returns an active season id. Existing installations are bootstrapped once;
     * a deliberately closed season is never reopened implicitly.
     */
    public Task<String> requireActiveSeasonId(String zoneId) {
        if (safe(zoneId).isBlank()) {
            return Tasks.forException(new IllegalArgumentException("Bölge bilgisi gerekli."));
        }
        return deviceRef.child("zones").child(zoneId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Exception error = task.getException();
                return Tasks.forException(error == null
                        ? new IllegalStateException("Bölge bulunamadı.")
                        : error);
            }
            DataSnapshot snapshot = task.getResult();
            GardenZone zone = snapshot.getValue(GardenZone.class);
            if (zone == null) {
                return Tasks.forException(new IllegalStateException("Bölge bilgisi okunamadı."));
            }
            if (safe(zone.getZone_id()).isBlank()) zone.setZone_id(zoneId);
            if (ZoneCapacityPolicy.isInactive(zone)) {
                return Tasks.forException(new IllegalStateException(
                        "Kullanım dışı bölgeye kayıt eklenemez."
                ));
            }
            ZoneSeasonState state = snapshot.child("season").getValue(ZoneSeasonState.class);
            if (state != null && state.isActive()) {
                return Tasks.forResult(state.getActive_season_id());
            }
            if (state != null && SeasonStatus.isClosed(state.getStatus())) {
                return Tasks.forException(new IllegalStateException(
                        "Bu bölgenin sezonu kapalı. Önce yeni sezon başlatın."
                ));
            }
            return bootstrapLegacySeason(zone)
                    .continueWithTask(ignored -> resolveActiveSeasonId(zoneId));
        });
    }
    private static void putOutcome(
            Map<String, Object> updates,
            String path,
            SeasonOutcome outcome,
            String seasonId,
            String zoneId,
            long now
    ) {
        updates.put(path + "season_id", seasonId);
        updates.put(path + "zone_id", zoneId);
        updates.put(path + "result", outcome == null ? "" : outcome.getResult());
        updates.put(path + "harvest_amount", outcome == null ? "" : outcome.getHarvest_amount());
        updates.put(path + "yield_note", outcome == null ? "" : outcome.getYield_note());
        updates.put(path + "issues_note", outcome == null ? "" : outcome.getIssues_note());
        updates.put(path + "successful_practices",
                outcome == null ? "" : outcome.getSuccessful_practices());
        updates.put(path + "next_season_note",
                outcome == null ? "" : outcome.getNext_season_note());
        updates.put(path + "water_summary", outcome == null ? "" : outcome.getWater_summary());
        updates.put(path + "fertilizer_summary",
                outcome == null ? "" : outcome.getFertilizer_summary());
        updates.put(path + "recorded_at_epoch", now);
    }

    private static SeasonCounts calculateCounts(MutableData root, String zoneId, ZoneSeasonState state) {
        SeasonCounts counts = new SeasonCounts();
        for (MutableData record : root.child("watering_history").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            if (!belongs(record, state)) continue;
            long duration = Math.max(0L, longValue(record.child("duration")));
            if (!SeasonRecordPolicy.hasMeaningfulWatering(duration)) continue;
            counts.wateringCount++;
            counts.wateringSeconds += duration;
        }
        for (MutableData record : root.child("fertilizer_history").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            if (belongs(record, state)) counts.fertilizerCount++;
        }
        for (MutableData record : root.child("garden_journal").child("events").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            if (!belongs(record, state)) continue;
            if (!SeasonRecordPolicy.isFieldJournalEvent(
                    stringValue(record.child("type")),
                    stringValue(record.child("source")),
                    stringValue(record.child("source_key")))) continue;
            counts.eventCount++;
            String type = stringValue(record.child("type")).toUpperCase(Locale.ROOT);
            if (type.contains("AI") || type.contains("ASSISTANT") || type.contains("ANALYSIS")) {
                counts.analysisCount++;
            }
        }
        for (MutableData record : root.child("garden_journal").child("photo_metadata").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            if (!belongs(record, state)) continue;
            counts.photoCount++;
            if (!stringValue(record.child("analysis_title")).isBlank()) counts.analysisCount++;
        }
        for (MutableData record : root.child("notifications").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            if (belongs(record, state)) counts.notificationCount++;
        }
        return counts;
    }

    private static SeasonCounts calculateCounts(
            DataSnapshot root,
            String zoneId,
            ZoneSeasonState state,
            boolean includePending
    ) {
        SeasonCounts counts = new SeasonCounts();
        for (DataSnapshot record : root.child("watering_history").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id"))) || !belongs(record, state)) continue;
            long duration = Math.max(0L, longValue(record.child("duration")));
            if (!SeasonRecordPolicy.hasMeaningfulWatering(duration)) continue;
            counts.wateringCount++;
            counts.wateringSeconds += duration;
        }
        if (includePending) {
            for (DataSnapshot pending : root.child("irrigation_runtime")
                    .child("pending_waterings").getChildren()) {
                DataSnapshot record = pending.child("record");
                if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
                String pendingSeasonId = stringValue(record.child("season_id"));
                if (!pendingSeasonId.isBlank()
                        && !state.getActive_season_id().equals(pendingSeasonId)) continue;
                long duration = Math.max(0L, longValue(record.child("duration")));
                if (!SeasonRecordPolicy.hasMeaningfulWatering(duration)) continue;
                counts.wateringCount++;
                counts.wateringSeconds += duration;
            }
        }
        for (DataSnapshot record : root.child("fertilizer_history").getChildren()) {
            if (zoneId.equals(stringValue(record.child("zone_id"))) && belongs(record, state)) {
                counts.fertilizerCount++;
            }
        }
        for (DataSnapshot record : root.child("garden_journal").child("events").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id"))) || !belongs(record, state)) continue;
            if (!SeasonRecordPolicy.isFieldJournalEvent(
                    stringValue(record.child("type")),
                    stringValue(record.child("source")),
                    stringValue(record.child("source_key")))) continue;
            counts.eventCount++;
            String type = stringValue(record.child("type")).toUpperCase(Locale.ROOT);
            if (type.contains("AI") || type.contains("ASSISTANT") || type.contains("ANALYSIS")) {
                counts.analysisCount++;
            }
        }
        for (DataSnapshot record : root.child("garden_journal")
                .child("photo_metadata").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id"))) || !belongs(record, state)) continue;
            counts.photoCount++;
            if (!stringValue(record.child("analysis_title")).isBlank()) counts.analysisCount++;
        }
        for (DataSnapshot record : root.child("notifications").getChildren()) {
            if (zoneId.equals(stringValue(record.child("zone_id"))) && belongs(record, state)) {
                counts.notificationCount++;
            }
        }
        return counts;
    }

    private static boolean belongs(DataSnapshot record, ZoneSeasonState state) {
        String recordSeasonId = stringValue(record.child("season_id"));
        if (!recordSeasonId.isBlank()) return state.getActive_season_id().equals(recordSeasonId);
        return state.isInclude_legacy_records();
    }

    private static boolean belongs(MutableData record, ZoneSeasonState state) {
        String recordSeasonId = stringValue(record.child("season_id"));
        if (!recordSeasonId.isBlank()) return state.getActive_season_id().equals(recordSeasonId);
        return state.isInclude_legacy_records();
    }

    private static boolean hasPendingWateringForSeason(
            MutableData root,
            String zoneId,
            String seasonId
    ) {
        for (MutableData pending : root.child("irrigation_runtime")
                .child("pending_waterings").getChildren()) {
            MutableData record = pending.child("record");
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            String pendingSeasonId = stringValue(record.child("season_id"));
            if (pendingSeasonId.isBlank() || pendingSeasonId.equals(seasonId)) return true;
        }
        return false;
    }

    private static boolean hasPendingWateringForSeason(
            DataSnapshot root,
            String zoneId,
            String seasonId
    ) {
        for (DataSnapshot pending : root.child("irrigation_runtime")
                .child("pending_waterings").getChildren()) {
            DataSnapshot record = pending.child("record");
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            String pendingSeasonId = stringValue(record.child("season_id"));
            if (pendingSeasonId.isBlank() || pendingSeasonId.equals(seasonId)) return true;
        }
        return false;
    }

    private static long inferLegacySeasonStart(MutableData root, String zoneId, long fallback) {
        long earliest = fallback;
        for (MutableData record : root.child("fertilizer_history").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("applied_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        for (MutableData record : root.child("garden_journal").child("events").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("occurred_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        for (MutableData record : root.child("garden_journal").child("photo_metadata").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("captured_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        return earliest;
    }

    private static long inferLegacySeasonStart(DataSnapshot root, String zoneId, long fallback) {
        long earliest = fallback;
        for (DataSnapshot record : root.child("fertilizer_history").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("applied_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        for (DataSnapshot record : root.child("garden_journal").child("events").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("occurred_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        for (DataSnapshot record : root.child("garden_journal").child("photo_metadata").getChildren()) {
            if (!zoneId.equals(stringValue(record.child("zone_id")))) continue;
            long epoch = longValue(record.child("captured_at_epoch"));
            if (epoch > 0L) earliest = Math.min(earliest, epoch);
        }
        return earliest;
    }

    private static String seasonLabel(GardenZone zone, long epoch) {
        String year = new java.text.SimpleDateFormat("yyyy", Locale.getDefault())
                .format(new java.util.Date(epoch * 1000L));
        return year + " Sezonu";
    }

    private static String normalizedGrowthStage(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "NOT_SET" : normalized;
    }

    private static long nowEpoch() {
        return System.currentTimeMillis() / 1000L;
    }

    private static String stringValue(MutableData data) {
        Object value = data.getValue();
        return value == null ? "" : String.valueOf(value);
    }

    private static long longValue(MutableData data) {
        Object value = data.getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static boolean booleanValue(MutableData data) {
        Object value = data.getValue();
        return value instanceof Boolean && (Boolean) value;
    }

    private static String stringValue(DataSnapshot data) {
        Object value = data.getValue();
        return value == null ? "" : String.valueOf(value);
    }

    private static long longValue(DataSnapshot data) {
        Object value = data.getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static boolean booleanValue(DataSnapshot data) {
        Object value = data.getValue();
        return value instanceof Boolean && (Boolean) value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }


    private static final class CancellationCheck {
        final boolean allowed;
        final String message;

        private CancellationCheck(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        static CancellationCheck allowed() {
            return new CancellationCheck(true, "");
        }

        static CancellationCheck blocked(String message) {
            return new CancellationCheck(false, message);
        }
    }
    private static final class SeasonCounts {
        int wateringCount;
        long wateringSeconds;
        int fertilizerCount;
        int eventCount;
        int photoCount;
        int analysisCount;
        int notificationCount;
    }
}
