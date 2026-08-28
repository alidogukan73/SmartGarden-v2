package com.ali.smartgarden.viewmodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OneShotEventTest {

    @Test
    public void viewModelResultIsConsumedOnlyOnceAcrossRecreation() {
        OneShotEvent<String> event = new OneShotEvent<>("completed");

        assertEquals("completed", event.consume());
        assertNull(event.consume());
    }
}
