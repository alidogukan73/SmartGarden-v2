package com.alidogukan.avora.zones;

/** Decides whether a season or zone operation would touch active irrigation hardware. */
public final class ZoneOperationSafetyPolicy {
    private ZoneOperationSafetyPolicy() { }

    public static boolean isTargetBusy(
            String targetZoneId,
            String targetValveId,
            boolean targetWateringActive,
            boolean targetSelectedForWatering,
            long targetQueuePosition,
            boolean targetHasPendingWatering,
            boolean hardwareBusy,
            String activeZoneId,
            String activeValveId
    ) {
        if (targetWateringActive || targetSelectedForWatering
                || targetQueuePosition > 0L || targetHasPendingWatering) {
            return true;
        }
        if (!hardwareBusy) return false;

        String expectedZone = safe(targetZoneId);
        String expectedValve = safe(targetValveId);
        String currentZone = safe(activeZoneId);
        String currentValve = safe(activeValveId);
        if (currentZone.isEmpty() && currentValve.isEmpty()) {
            // A running pump with no owner cannot be changed safely.
            return true;
        }
        return (!expectedZone.isEmpty() && expectedZone.equals(currentZone))
                || (!expectedValve.isEmpty() && expectedValve.equals(currentValve));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
