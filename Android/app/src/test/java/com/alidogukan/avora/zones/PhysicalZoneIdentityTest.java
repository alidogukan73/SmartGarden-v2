package com.alidogukan.avora.zones;

import static org.junit.Assert.assertEquals;

import com.alidogukan.avora.models.GardenZone;

import org.junit.Test;

public final class PhysicalZoneIdentityTest {
    @Test
    public void legacyCropNameNeverBecomesPhysicalAreaName() {
        GardenZone zone = new GardenZone();
        zone.setZone_id("zone-001");
        zone.setName("Domates");

        assertEquals("1. Bölge", PhysicalZoneIdentity.name(zone));
    }

    @Test
    public void configuredAreaNameIsStableAcrossCrops() {
        GardenZone zone = new GardenZone();
        zone.setZone_id("zone-002");
        zone.setArea_name("Arka bahçe");
        zone.setName("Domates");

        assertEquals("Arka bahçe", PhysicalZoneIdentity.name(zone));
        zone.setName("Biber");
        assertEquals("Arka bahçe", PhysicalZoneIdentity.name(zone));
    }

    @Test
    public void orderSupportsLegacyZonesWithoutCanonicalId() {
        GardenZone zone = new GardenZone();
        zone.setOrder(8);

        assertEquals("8. Bölge", PhysicalZoneIdentity.name(zone));
    }

    @Test
    public void appearanceUsesSafeDefaultsAndPersistsValidChoices() {
        GardenZone zone = new GardenZone();
        assertEquals("🌿", PhysicalZoneIdentity.icon(zone));
        assertEquals("#2E7D32", PhysicalZoneIdentity.color(zone));

        zone.setArea_icon("🌳");
        zone.setArea_color("#1565C0");
        assertEquals("🌳", PhysicalZoneIdentity.icon(zone));
        assertEquals("#1565C0", PhysicalZoneIdentity.color(zone));
        zone.setArea_color("not-a-color");
        assertEquals("#2E7D32", PhysicalZoneIdentity.color(zone));
    }
}
