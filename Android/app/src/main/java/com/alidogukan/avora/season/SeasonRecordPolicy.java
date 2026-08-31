package com.alidogukan.avora.season;

import com.alidogukan.avora.models.SeasonOutcome;
import java.util.Locale;

/** Distinguishes real field records from automatic advice and season metadata. */
public final class SeasonRecordPolicy {
    public static final String SOURCE_SYSTEM = "SYSTEM";
    public static final String SEASON_CLOSED_SOURCE_KEY_PREFIX = "season_closed:";

    private SeasonRecordPolicy() { }

    public static boolean hasMeaningfulWatering(long durationSeconds) {
        return durationSeconds > 0L;
    }

    public static boolean isFieldJournalEvent(
            String type,
            String source,
            String sourceKey) {
        String normalizedSource = safe(source).toUpperCase(Locale.ROOT);
        String normalizedKey = safe(sourceKey).toLowerCase(Locale.ROOT);
        if ("AUTO".equals(normalizedSource) || SOURCE_SYSTEM.equals(normalizedSource)
                || normalizedKey.startsWith(SEASON_CLOSED_SOURCE_KEY_PREFIX)) {
            return false;
        }
        String normalizedType = safe(type).toLowerCase(Locale.ROOT);
        return !"sezon tamamlandı".equals(normalizedType)
                && !"season completed".equals(normalizedType);
    }

    public static boolean hasMeaningfulOutcome(SeasonOutcome outcome) {
        return outcome != null && (present(outcome.getHarvest_amount())
                || present(outcome.getYield_note())
                || present(outcome.getIssues_note())
                || present(outcome.getSuccessful_practices())
                || present(outcome.getWater_summary())
                || present(outcome.getFertilizer_summary())
                || present(outcome.getNext_season_note()));
    }

    public static boolean hasMeaningfulOutcomeValues(
            String harvestAmount,
            String yieldNote,
            String issuesNote,
            String successfulPractices,
            String waterSummary,
            String fertilizerSummary,
            String nextSeasonNote) {
        return present(harvestAmount) || present(yieldNote) || present(issuesNote)
                || present(successfulPractices) || present(waterSummary)
                || present(fertilizerSummary) || present(nextSeasonNote);
    }

    private static boolean present(String value) {
        return !safe(value).isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
