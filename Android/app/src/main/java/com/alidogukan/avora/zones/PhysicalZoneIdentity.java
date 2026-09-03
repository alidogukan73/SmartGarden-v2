package com.alidogukan.avora.zones;

import com.alidogukan.avora.models.GardenZone;

import java.util.Locale;

/** Stable user-facing identity for one of the eight physical growing areas. */
public final class PhysicalZoneIdentity {
    private static final String FALLBACK = "Bölge";
    public static final String DEFAULT_ICON = "🌿";
    public static final String DEFAULT_COLOR = "#2E7D32";

    private PhysicalZoneIdentity() { }

    public static String name(GardenZone zone) {
        String configured = safe(zone == null ? "" : zone.getArea_name());
        if (!configured.isEmpty()) return configured;
        int slot = slot(zone);
        return slot > 0 ? defaultName(slot) : FALLBACK;
    }

    public static String icon(GardenZone zone) {
        String configured = safe(zone == null ? "" : zone.getArea_icon());
        return configured.isEmpty() ? DEFAULT_ICON : configured;
    }

    public static String color(GardenZone zone) {
        String configured = safe(zone == null ? "" : zone.getArea_color());
        return configured.matches("#[0-9a-fA-F]{6}") ? configured : DEFAULT_COLOR;
    }

    public static String defaultName(int slot) {
        if (slot < 1 || slot > ZoneCapacityPolicy.MAX_ZONES) return FALLBACK;
        return String.format(Locale.forLanguageTag("tr-TR"), "%d. Bölge", slot);
    }

    public static int slot(GardenZone zone) {
        if (zone == null) return -1;
        String zoneId = safe(zone.getZone_id()).toLowerCase(Locale.US);
        if (zoneId.startsWith("zone-")) {
            try {
                int parsed = Integer.parseInt(zoneId.substring("zone-".length()));
                if (parsed >= 1 && parsed <= ZoneCapacityPolicy.MAX_ZONES) return parsed;
            } catch (NumberFormatException ignored) {
                // Fall back to the persisted order below.
            }
        }
        int order = zone.getOrder();
        return order >= 1 && order <= ZoneCapacityPolicy.MAX_ZONES ? order : -1;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
