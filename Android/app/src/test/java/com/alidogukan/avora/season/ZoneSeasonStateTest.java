package com.alidogukan.avora.season;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.SeasonStatus;
import com.alidogukan.avora.models.ZoneSeasonState;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ZoneSeasonStateTest {
    @Test
    public void multipleActiveSeasonsAreTrackedIndependently() {
        ZoneSeasonState state = new ZoneSeasonState();
        state.setStatus(SeasonStatus.ACTIVE);
        state.setActive_season_id("pepper");
        Map<String, Boolean> active = new LinkedHashMap<>();
        active.put("bean", true);
        active.put("pepper", true);
        state.setActive_season_ids(active);

        assertTrue(state.isActive());
        assertTrue(state.isSeasonActive("bean"));
        assertTrue(state.isSeasonActive("pepper"));
        assertFalse(state.isSeasonActive("tomato"));
    }
}
