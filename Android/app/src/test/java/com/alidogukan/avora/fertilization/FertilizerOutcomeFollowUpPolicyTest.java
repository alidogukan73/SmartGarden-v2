package com.alidogukan.avora.fertilization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.FertilizerApplication;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FertilizerOutcomeFollowUpPolicyTest {

    private static final long NOW = 2_000_000_000L;

    @Test
    public void followUpBecomesDueAfterThreeDays() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        value.setApplied_at_epoch(NOW - FertilizerOutcomeFollowUpPolicy.FOLLOW_UP_DELAY_SECONDS);

        assertTrue(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW));
    }

    @Test
    public void followUpIsNotDueBeforeThreeDays() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        value.setApplied_at_epoch(NOW - FertilizerOutcomeFollowUpPolicy.FOLLOW_UP_DELAY_SECONDS + 1L);

        assertFalse(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW));
    }

    @Test
    public void evaluatedApplicationDoesNotCreateAnotherFollowUp() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        value.setApplied_at_epoch(NOW - FertilizerOutcomeFollowUpPolicy.FOLLOW_UP_DELAY_SECONDS);
        value.setOutcome_status("IMPROVED");

        assertFalse(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW));
    }

    @Test
    public void staleHistoryDoesNotCreateNotificationStorm() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        value.setApplied_at_epoch(NOW - FertilizerOutcomeFollowUpPolicy.MAX_FOLLOW_UP_AGE_SECONDS - 1L);

        assertFalse(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW));
    }

    @Test
    public void explicitDueDateIsRespected() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        value.setApplied_at_epoch(NOW - 5L * 86_400L);
        value.setOutcome_follow_up_due_at_epoch(NOW + 60L);

        assertFalse(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW));
        assertTrue(FertilizerOutcomeFollowUpPolicy.isDue(value, NOW + 60L));
    }

    @Test
    public void sourceKeyRoundTripKeepsApplicationId() {
        FertilizerApplication value = application("app-001", "zone-001", "product-001");
        String source = FertilizerOutcomeFollowUpPolicy.sourceKey(value);

        assertEquals("app-001", FertilizerOutcomeFollowUpPolicy.applicationIdFromSource(source));
    }

    @Test
    public void evaluatedCountUsesSameZoneAndProductOnly() {
        FertilizerApplication first = application("app-001", "zone-001", "product-001");
        first.setOutcome_status("IMPROVED");
        FertilizerApplication second = application("app-002", "zone-001", "product-001");
        second.setOutcome_vigor_score(4);
        FertilizerApplication otherZone = application("app-003", "zone-002", "product-001");
        otherZone.setOutcome_status("IMPROVED");

        assertEquals(2, FertilizerOutcomeFollowUpPolicy.evaluatedCount(
                Arrays.asList(first, second, otherZone), "zone-001", "product-001"
        ));
    }

    @Test
    public void onlyNewestDueApplicationPerZoneCreatesAFollowUp() {
        FertilizerApplication older = application("app-001", "zone-001", "product-001");
        older.setApplied_at_epoch(NOW - 6L * 86_400L);
        FertilizerApplication newest = application("app-002", "zone-001", "product-001");
        newest.setApplied_at_epoch(NOW - 4L * 86_400L);
        FertilizerApplication otherZone = application("app-003", "zone-002", "product-002");
        otherZone.setApplied_at_epoch(NOW - 5L * 86_400L);

        List<FertilizerApplication> due =
                FertilizerOutcomeFollowUpPolicy.latestDuePerZone(
                        Arrays.asList(older, newest, otherZone), NOW);

        assertEquals(2, due.size());
        assertFalse(due.contains(older));
        assertTrue(due.contains(newest));
        assertTrue(due.contains(otherZone));
    }

    @Test
    public void evaluatedNewestApplicationDoesNotReplayOlderZoneBacklog() {
        FertilizerApplication older = application("app-001", "zone-001", "product-001");
        older.setApplied_at_epoch(NOW - 6L * 86_400L);
        FertilizerApplication newest = application("app-002", "zone-001", "product-001");
        newest.setApplied_at_epoch(NOW - 4L * 86_400L);
        newest.setOutcome_status("IMPROVED");

        List<FertilizerApplication> due =
                FertilizerOutcomeFollowUpPolicy.latestDuePerZone(
                        Arrays.asList(older, newest), NOW);

        assertTrue(due.isEmpty());
    }


    private static FertilizerApplication application(String id, String zoneId, String productId) {
        FertilizerApplication value = new FertilizerApplication();
        value.setApplication_id(id);
        value.setZone_id(zoneId);
        value.setProduct_id(productId);
        return value;
    }
}
