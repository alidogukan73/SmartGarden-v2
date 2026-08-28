package com.ali.smartgarden.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AvoraLanguageManagerTest {

    @Test
    public void englishBundleIsAvailable() {
        assertTrue(AvoraLanguageManager.isEnglishAvailable());
    }

    @Test
    public void explicitModesExposeTheirResourceLanguageTag() {
        assertEquals("tr", AvoraLanguageManager.explicitLanguageTag("tr"));
        assertEquals("en", AvoraLanguageManager.explicitLanguageTag("en"));
        assertEquals("", AvoraLanguageManager.explicitLanguageTag("system"));
    }

}
