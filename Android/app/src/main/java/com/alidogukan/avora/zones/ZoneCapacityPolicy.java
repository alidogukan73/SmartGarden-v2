package com.alidogukan.avora.zones;

import com.alidogukan.avora.models.GardenZone;

import java.util.List;
import java.util.Locale;

/** Shared Android rules for the eight physical garden channels. */
public final class ZoneCapacityPolicy {
    public enum DeactivationAction { DELETE, ARCHIVE }
    public static final int MAX_ZONES = 8;
    public static final String LIFECYCLE_ACTIVE = "ACTIVE";
    public static final String LIFECYCLE_HARDWARE_PENDING = "HARDWARE_PENDING";
    public static final String LIFECYCLE_INACTIVE = "INACTIVE";

    public static final String ERROR_INVALID_ZONE = "ZONE_INVALID";
    public static final String ERROR_SENSOR_INVALID = "SENSOR_INVALID";
    public static final String ERROR_VALVE_INVALID = "VALVE_INVALID";
    public static final String ERROR_SENSOR_IN_USE = "SENSOR_IN_USE";
    public static final String ERROR_VALVE_IN_USE = "VALVE_IN_USE";
    public static final String ERROR_IRRIGATION_BUSY = "IRRIGATION_BUSY";
    public static final String ERROR_ACTIVE_SEASON = "ACTIVE_SEASON";
    public static final String ERROR_ZONE_NOT_FOUND = "ZONE_NOT_FOUND";
    public static final String ERROR_ZONE_IN_USE = "ZONE_IN_USE";

    private ZoneCapacityPolicy() { }

    public static String zoneId(int slot) { return id("zone", slot); }
    public static String sensorId(int slot) { return id("soil", slot); }
    public static String valveId(int slot) { return id("valve", slot); }
    public static boolean isValidZoneId(String value) { return slot(value, "zone") > 0; }
    public static boolean isValidSensorId(String value) {
        return blank(value) || slot(value, "soil") > 0;
    }
    public static boolean isValidValveId(String value) {
        return blank(value) || slot(value, "valve") > 0;
    }

    public static boolean isInactive(GardenZone zone) {
        if (zone == null) return true;
        return LIFECYCLE_INACTIVE.equalsIgnoreCase(safe(zone.getLifecycle_status()))
                || !zone.isEnabled();
    }

    public static boolean isActive(GardenZone zone) {
        return zone != null
                && isValidZoneId(zone.getZone_id())
                && !isInactive(zone);
    }

    public static int activeCount(List<GardenZone> zones) {
        int count = 0;
        if (zones == null) return count;
        for (GardenZone zone : zones) {
            if (isActive(zone)) count++;
        }
        return count;
    }

    public static List<GardenZone> activeZones(List<GardenZone> zones) {
        List<GardenZone> result = new java.util.ArrayList<>();
        if (zones == null) return result;
        for (GardenZone zone : zones) {
            if (isActive(zone)) {
                result.add(zone);
            }
        }
        return result;
    }

    /** Returns every hardware channel that is not currently used by an active zone. */
    public static List<Integer> availableSlots(List<GardenZone> zones) {
        boolean[] active = new boolean[MAX_ZONES + 1];
        if (zones != null) {
            for (GardenZone zone : zones) {
                if (!isActive(zone)) continue;
                int value = slot(zone.getZone_id(), "zone");
                if (value > 0) active[value] = true;
            }
        }
        List<Integer> result = new java.util.ArrayList<>();
        for (int index = 1; index <= MAX_ZONES; index++) {
            if (!active[index]) result.add(index);
        }
        return result;
    }

    public static boolean hasProtectedSeason(String status, String activeSeasonId) {
        String normalizedStatus = safe(status).toUpperCase(Locale.US);
        String seasonId = safe(activeSeasonId);
        if ("ACTIVE".equals(normalizedStatus) || "PLANNED".equals(normalizedStatus)) {
            return true;
        }
        return !seasonId.isEmpty() && !"CLOSED".equals(normalizedStatus);
    }

    /** Deletes only a disposable zone; any local or cloud history keeps an archive. */
    public static boolean shouldDeleteOnDeactivate(
            boolean hasLocalHistory, boolean hasCloudHistory) {
        return !hasLocalHistory && !hasCloudHistory;
    }

    /** Complete zone-removal decision shared by Firebase and unit tests. */
    public static DeactivationAction decideDeactivation(
            boolean zoneExists,
            String seasonStatus,
            String activeSeasonId,
            boolean irrigationBusy,
            boolean hasLocalHistory,
            boolean hasCloudHistory
    ) {
        if (!zoneExists) {
            throw new IllegalStateException(ERROR_ZONE_NOT_FOUND);
        }
        if (hasProtectedSeason(seasonStatus, activeSeasonId)) {
            throw new IllegalStateException(ERROR_ACTIVE_SEASON);
        }
        if (irrigationBusy) {
            throw new IllegalStateException(ERROR_IRRIGATION_BUSY);
        }
        return shouldDeleteOnDeactivate(hasLocalHistory, hasCloudHistory)
                ? DeactivationAction.DELETE
                : DeactivationAction.ARCHIVE;
    }


    /** Returns an unused channel, preferring a never-created slot over an archived slot. */
    public static int nextAvailableSlot(List<GardenZone> zones) {
        boolean[] exists = new boolean[MAX_ZONES + 1];
        boolean[] active = new boolean[MAX_ZONES + 1];
        if (zones != null) {
            for (GardenZone zone : zones) {
                if (zone == null) continue;
                int value = slot(zone.getZone_id(), "zone");
                if (value <= 0) continue;
                exists[value] = true;
                active[value] = !isInactive(zone);
            }
        }
        for (int index = 1; index <= MAX_ZONES; index++) {
            if (!exists[index]) return index;
        }
        for (int index = 1; index <= MAX_ZONES; index++) {
            if (!active[index]) return index;
        }
        return -1;
    }

    public static void validateCandidate(GardenZone candidate, List<GardenZone> zones) {
        if (candidate == null || !isValidZoneId(candidate.getZone_id())) {
            throw new IllegalArgumentException(ERROR_INVALID_ZONE);
        }
        String sensorId = safe(candidate.getSensor_id());
        String valveId = safe(candidate.getValve_id());
        if (!isValidSensorId(sensorId)) {
            throw new IllegalArgumentException(ERROR_SENSOR_INVALID);
        }
        if (!isValidValveId(valveId)) {
            throw new IllegalArgumentException(ERROR_VALVE_INVALID);
        }
        if (zones == null) return;
        for (GardenZone zone : zones) {
            if (!isActive(zone)
                    || safe(zone.getZone_id()).equals(candidate.getZone_id())) continue;
            if (!sensorId.isEmpty() && sensorId.equalsIgnoreCase(safe(zone.getSensor_id()))) {
                throw new IllegalArgumentException(ERROR_SENSOR_IN_USE);
            }
            if (!valveId.isEmpty() && valveId.equalsIgnoreCase(safe(zone.getValve_id()))) {
                throw new IllegalArgumentException(ERROR_VALVE_IN_USE);
            }
        }
    }

    private static String id(String prefix, int slot) {
        if (slot < 1 || slot > MAX_ZONES) {
            throw new IllegalArgumentException(ERROR_INVALID_ZONE);
        }
        return String.format(Locale.US, "%s-%03d", prefix, slot);
    }

    private static int slot(String value, String prefix) {
        String normalized = safe(value).toLowerCase(Locale.US);
        String start = prefix + "-";
        if (!normalized.startsWith(start)) return -1;
        try {
            int result = Integer.parseInt(normalized.substring(start.length()));
            return result >= 1 && result <= MAX_ZONES ? result : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean blank(String value) { return safe(value).isEmpty(); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
