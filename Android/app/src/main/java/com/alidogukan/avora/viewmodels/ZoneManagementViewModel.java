package com.alidogukan.avora.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.crop.CropCatalog;
import com.alidogukan.avora.models.CropCatalogItem;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.season.ZoneAreaIdentity;
import com.alidogukan.avora.zones.ZoneLocalDataRepository;
import com.alidogukan.avora.zones.ZoneCapacityPolicy;
import com.alidogukan.avora.zones.PhysicalZoneIdentity;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Business boundary for zone inventory, hardware mapping and safe removal. */
public final class ZoneManagementViewModel extends AndroidViewModel {
    public static final int MAX_ZONES = ZoneCapacityPolicy.MAX_ZONES;
    public static final String ERROR_SENSOR_IN_USE = ZoneCapacityPolicy.ERROR_SENSOR_IN_USE;
    public static final String ERROR_VALVE_IN_USE = ZoneCapacityPolicy.ERROR_VALVE_IN_USE;
    public static final String ERROR_IRRIGATION_BUSY = ZoneCapacityPolicy.ERROR_IRRIGATION_BUSY;
    public static final String ERROR_ACTIVE_SEASON = ZoneCapacityPolicy.ERROR_ACTIVE_SEASON;
    public static final String ERROR_ZONE_IN_USE = ZoneCapacityPolicy.ERROR_ZONE_IN_USE;
    public static final String ERROR_INVALID_ZONE = ZoneCapacityPolicy.ERROR_INVALID_ZONE;
    public static final String ERROR_SENSOR_INVALID = ZoneCapacityPolicy.ERROR_SENSOR_INVALID;
    public static final String ERROR_VALVE_INVALID = ZoneCapacityPolicy.ERROR_VALVE_INVALID;
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

    public List<CropCatalogItem> mergedCrops(List<CropCatalogItem> values) {
        return CropCatalog.merge(values);
    }
    public List<Integer> availableSlots(List<GardenZone> values) {
        return ZoneCapacityPolicy.availableSlots(values);
    }
    public int activeCount(List<GardenZone> values) {
        return ZoneCapacityPolicy.activeCount(values);
    }
    public String zoneId(int slot) { return ZoneCapacityPolicy.zoneId(slot); }
    public String sensorId(int slot) { return ZoneCapacityPolicy.sensorId(slot); }
    public String valveId(int slot) { return ZoneCapacityPolicy.valveId(slot); }
    public boolean isInactive(GardenZone zone) { return ZoneCapacityPolicy.isInactive(zone); }

    public GardenZone createCandidate(GardenZone existing, int slot, CropCatalogItem crop,
                                      String enteredName, String sensorId, String valveId,
                                      boolean irrigationEnabled) {
        GardenZone zone = new GardenZone();
        zone.setZone_id(ZoneCapacityPolicy.zoneId(slot));
        zone.setArea_id(existing == null || ZoneCapacityPolicy.isInactive(existing)
                ? ZoneAreaIdentity.newAreaId()
                : ZoneAreaIdentity.effective(existing));
        zone.setArea_name(enteredName.isEmpty()
                ? PhysicalZoneIdentity.defaultName(slot) : enteredName.trim());
        zone.setLocation_name(existing == null ? "" : existing.getLocation_name());
        zone.setArea_icon(existing == null
                ? PhysicalZoneIdentity.DEFAULT_ICON : PhysicalZoneIdentity.icon(existing));
        zone.setArea_color(existing == null
                ? PhysicalZoneIdentity.DEFAULT_COLOR : PhysicalZoneIdentity.color(existing));
        zone.setLow_moisture_alert_enabled(existing == null
                || existing.isLow_moisture_alert_enabled());
        zone.setWatering_complete_alert_enabled(existing == null
                || existing.isWatering_complete_alert_enabled());
        zone.setName(crop.getName());
        zone.setPlant_type(crop.getPlant_type());
        zone.setEmoji(crop.getEmoji());
        zone.setSensor_id(sensorId);
        zone.setValve_id(valveId);
        zone.setEnabled(true);
        zone.setIrrigation_enabled(irrigationEnabled);
        zone.setOrder(slot);
        zone.setSensor_enabled(!sensorId.isEmpty());
        if (existing != null && sensorId.equals(existing.getSensor_id())) {
            zone.setSensor_calibration_dry_raw(existing.getSensor_calibration_dry_raw());
            zone.setSensor_calibration_wet_raw(existing.getSensor_calibration_wet_raw());
        }
        if (existing == null || ZoneCapacityPolicy.isInactive(existing)) {
            zone.setMoisture_limit(crop.getIdeal_moisture_min());
            zone.setPump_duration(10);
            zone.setCooldown_seconds(600);
            zone.setRestart_delta(10);
        } else {
            zone.setMoisture_limit(existing.getMoisture_limit());
            zone.setPump_duration(existing.getPump_duration());
            zone.setCooldown_seconds(existing.getCooldown_seconds());
            zone.setRestart_delta(existing.getRestart_delta());
        }
        return zone;
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
        boolean hasLocalHistory = localData.hasMeaningfulHistory(zone);
        Task<Boolean> task = repository.deactivateGardenZone(zoneId, hasLocalHistory);
        task.addOnSuccessListener(deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                localData.removeEmptyZoneData(zoneId);
            }
        });
        return task;
    }
}
