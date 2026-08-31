package com.alidogukan.avora.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Typed device tree projection exposed by the data layer to presentation code. */
public final class DeviceInfoSnapshot {
    private final Status status;
    private final Health health;
    private final List<GardenZone> zones;
    private final Set<String> firmwareVersions;

    public DeviceInfoSnapshot(Status status, Health health, List<GardenZone> zones,
                              Set<String> firmwareVersions) {
        this.status = status;
        this.health = health;
        this.zones = Collections.unmodifiableList(new ArrayList<>(zones));
        this.firmwareVersions = Collections.unmodifiableSet(
                new LinkedHashSet<>(firmwareVersions));
    }

    public Status getStatus() {
        return status;
    }

    public Health getHealth() {
        return health;
    }

    public List<GardenZone> getZones() {
        return zones;
    }

    public Set<String> getFirmwareVersions() {
        return firmwareVersions;
    }
}
