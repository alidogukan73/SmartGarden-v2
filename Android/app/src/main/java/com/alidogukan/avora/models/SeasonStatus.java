package com.alidogukan.avora.models;

/** Stable Firebase values for a garden season lifecycle. */
public final class SeasonStatus {
    public static final String ACTIVE = "ACTIVE";
    public static final String PLANNED = "PLANNED";
    public static final String CLOSED = "CLOSED";

    private SeasonStatus() { }

    public static boolean isActive(String value) {
        return ACTIVE.equals(value);
    }

    public static boolean isClosed(String value) {
        return CLOSED.equals(value);
    }
}
