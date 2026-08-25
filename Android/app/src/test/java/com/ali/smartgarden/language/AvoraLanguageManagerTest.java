package com.ali.smartgarden.language;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AvoraLanguageManagerTest {

    @Test
    public void englishBundleIsAvailable() {
        assertTrue(AvoraLanguageManager.isEnglishAvailable());
    }
}
