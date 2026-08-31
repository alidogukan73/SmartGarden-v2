package com.alidogukan.avora.health;

/** Transparent, advisory result for one garden zone. */
public final class GardenHealthZoneResult {
    private final int score;
    private final String reason;

    public GardenHealthZoneResult(int score, String reason) {
        this.score = Math.max(0, Math.min(100, score));
        this.reason = reason == null ? "" : reason;
    }

    public int getScore() { return score; }
    public String getReason() { return reason; }
}
