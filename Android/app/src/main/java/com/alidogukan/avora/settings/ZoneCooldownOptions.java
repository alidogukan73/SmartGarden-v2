package com.alidogukan.avora.settings;

/**
 * Maps the zone cooldown slider to human-friendly, selectable durations.
 * The slider uses option indexes so short durations do not get compressed
 * into the first few pixels by the 24-hour upper limit.
 */
public final class ZoneCooldownOptions {
    private static final int[] MINUTES = {
            1, 2, 3, 5, 10, 15, 20, 30,
            45, 60, 90, 120, 180, 360, 720, 1440
    };

    private ZoneCooldownOptions() {
    }

    public static int maximumSliderIndex() {
        return MINUTES.length - 1;
    }

    public static int minutesForSliderValue(float sliderValue) {
        int index = Math.round(sliderValue);
        index = Math.max(0, Math.min(maximumSliderIndex(), index));
        return MINUTES[index];
    }

    public static float sliderValueForMinutes(int minutes) {
        int normalizedMinutes = Math.max(1, minutes);
        int nearestIndex = 0;
        int nearestDistance = Math.abs(MINUTES[0] - normalizedMinutes);

        for (int index = 1; index < MINUTES.length; index++) {
            int distance = Math.abs(MINUTES[index] - normalizedMinutes);
            if (distance < nearestDistance) {
                nearestIndex = index;
                nearestDistance = distance;
            }
        }

        return nearestIndex;
    }
}
