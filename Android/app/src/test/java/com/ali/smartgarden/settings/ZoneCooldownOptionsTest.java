package com.ali.smartgarden.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ZoneCooldownOptionsTest {
    @Test
    public void thirtyMinutesHasAnExactSelectableStep() {
        float sliderValue = ZoneCooldownOptions.sliderValueForMinutes(30);

        assertEquals(30, ZoneCooldownOptions.minutesForSliderValue(sliderValue));
    }

    @Test
    public void existingValuesUseTheNearestSafeOption() {
        float sliderValue = ZoneCooldownOptions.sliderValueForMinutes(31);

        assertEquals(30, ZoneCooldownOptions.minutesForSliderValue(sliderValue));
    }

    @Test
    public void sliderValuesAreClampedToSupportedOptions() {
        assertEquals(1, ZoneCooldownOptions.minutesForSliderValue(-10));
        assertEquals(1440, ZoneCooldownOptions.minutesForSliderValue(100));
    }
}
