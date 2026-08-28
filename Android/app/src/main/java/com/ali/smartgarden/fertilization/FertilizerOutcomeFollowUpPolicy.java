package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerApplication;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rules for the single post-application observation requested by AVORA. */
public final class FertilizerOutcomeFollowUpPolicy {

    public static final long FOLLOW_UP_DELAY_SECONDS = 3L * 24L * 60L * 60L;
    public static final long MAX_FOLLOW_UP_AGE_SECONDS = 30L * 24L * 60L * 60L;
    public static final int RELIABLE_OBSERVATION_COUNT = 2;
    public static final String SOURCE_PREFIX = "fertilizer_outcome_follow_up:";

    private FertilizerOutcomeFollowUpPolicy() {
    }

    public static long dueAt(FertilizerApplication value) {
        if (value == null) return 0L;
        if (value.getOutcome_follow_up_due_at_epoch() > 0L) {
            return value.getOutcome_follow_up_due_at_epoch();
        }
        return value.getApplied_at_epoch() > 0L
                ? value.getApplied_at_epoch() + FOLLOW_UP_DELAY_SECONDS
                : 0L;
    }

    public static boolean isEvaluated(FertilizerApplication value) {
        if (value == null) return false;
        String status = value.getOutcome_status();
        return value.getOutcome_observed_at_epoch() > 0L
                || value.getOutcome_vigor_score() > 0
                || (status != null && !status.isBlank());
    }

    public static boolean isDue(FertilizerApplication value, long nowEpoch) {
        long dueAt = dueAt(value);
        long appliedAt = value == null ? 0L : value.getApplied_at_epoch();
        boolean recentEnough = appliedAt > 0L
                && nowEpoch >= appliedAt
                && nowEpoch - appliedAt <= MAX_FOLLOW_UP_AGE_SECONDS;
        return dueAt > 0L
                && dueAt <= nowEpoch
                && recentEnough
                && !isEvaluated(value);
    }

    /**
     * Returns at most one actionable follow-up per zone. Only the newest
     * application in a zone may create a reminder, so installing or opening
     * the app cannot replay an old backlog as a notification storm.
     */
    public static List<FertilizerApplication> latestDuePerZone(
            List<FertilizerApplication> history, long nowEpoch) {
        List<FertilizerApplication> result = new ArrayList<>();
        if (history == null || history.isEmpty()) return result;

        Map<String, FertilizerApplication> latestByZone = new LinkedHashMap<>();
        for (FertilizerApplication value : history) {
            if (value == null || value.getApplied_at_epoch() <= 0L) continue;
            String zoneKey = zoneKey(value);
            FertilizerApplication current = latestByZone.get(zoneKey);
            if (current == null || isNewer(value, current)) {
                latestByZone.put(zoneKey, value);
            }
        }

        for (FertilizerApplication value : latestByZone.values()) {
            if (isDue(value, nowEpoch)) result.add(value);
        }
        return result;
    }

    private static boolean isNewer(FertilizerApplication value,
                                   FertilizerApplication current) {
        if (value.getApplied_at_epoch() != current.getApplied_at_epoch()) {
            return value.getApplied_at_epoch() > current.getApplied_at_epoch();
        }
        return safe(value.getApplication_id()).compareTo(
                safe(current.getApplication_id())) > 0;
    }

    private static String zoneKey(FertilizerApplication value) {
        String zoneId = safe(value.getZone_id());
        if (!zoneId.isEmpty()) return "zone:" + zoneId;
        String zoneName = safe(value.getZone_name());
        if (!zoneName.isEmpty()) return "name:" + zoneName;
        return "application:" + safe(value.getApplication_id());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static String sourceKey(FertilizerApplication value) {
        String id = value == null || value.getApplication_id() == null
                ? "" : value.getApplication_id().trim();
        return id.isEmpty() ? "" : SOURCE_PREFIX + id;
    }

    public static String applicationIdFromSource(String sourceKey) {
        if (sourceKey == null || !sourceKey.startsWith(SOURCE_PREFIX)) return "";
        return sourceKey.substring(SOURCE_PREFIX.length()).trim();
    }

    public static int evaluatedCount(List<FertilizerApplication> history,
                                     String zoneId,
                                     String productId) {
        if (history == null) return 0;
        int count = 0;
        for (FertilizerApplication value : history) {
            if (value == null || !same(zoneId, value.getZone_id())
                    || !same(productId, value.getProduct_id())) continue;
            if (isEvaluated(value)) count++;
        }
        return count;
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.equals(right);
    }
}
