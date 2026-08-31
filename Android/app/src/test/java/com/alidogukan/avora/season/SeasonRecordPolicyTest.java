package com.alidogukan.avora.season;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.SeasonOutcome;
import org.junit.Test;

public class SeasonRecordPolicyTest {
    @Test
    public void onlyAStartedWateringIsFieldActivity() {
        assertFalse(SeasonRecordPolicy.hasMeaningfulWatering(0L));
        assertTrue(SeasonRecordPolicy.hasMeaningfulWatering(1L));
    }

    @Test
    public void automaticAndLifecycleEventsAreNotFieldActivity() {
        assertFalse(SeasonRecordPolicy.isFieldJournalEvent(
                "Nem riski", "AUTO", "moisture_risk:2026-08-27"));
        assertFalse(SeasonRecordPolicy.isFieldJournalEvent(
                "Sezon tamamlandı", "MANUAL", ""));
        assertFalse(SeasonRecordPolicy.isFieldJournalEvent(
                "Season completed", "SYSTEM", "season_closed:test"));
        assertTrue(SeasonRecordPolicy.isFieldJournalEvent(
                "Budama", "MANUAL", ""));
    }

    @Test
    public void resultSelectionAloneDoesNotTurnATestIntoARealArchive() {
        SeasonOutcome outcome = new SeasonOutcome();
        outcome.setResult("Orta");
        assertFalse(SeasonRecordPolicy.hasMeaningfulOutcome(outcome));

        outcome.setHarvest_amount("12 kg");
        assertTrue(SeasonRecordPolicy.hasMeaningfulOutcome(outcome));
    }
}
