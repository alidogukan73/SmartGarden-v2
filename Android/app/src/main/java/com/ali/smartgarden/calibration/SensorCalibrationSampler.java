package com.ali.smartgarden.calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collects distinct live sensor readings and derives a stable median. */
public final class SensorCalibrationSampler {
    public static final int REQUIRED_SAMPLES = 5;
    public static final int MAX_STABLE_SPREAD_RAW = 1200;
    public static final int MIN_CALIBRATION_RANGE_RAW = 500;
    private static final int MAX_ADS1115_RAW = 32767;

    private final List<Integer> samples = new ArrayList<>();
    private long lastSampleEpoch = -1L;

    public boolean addSample(int raw, long updatedAtEpoch) {
        if (raw < 0 || raw > MAX_ADS1115_RAW || updatedAtEpoch <= 0L
                || updatedAtEpoch == lastSampleEpoch || isComplete()) {
            return false;
        }
        samples.add(raw);
        lastSampleEpoch = updatedAtEpoch;
        return true;
    }

    public int getCount() {
        return samples.size();
    }

    public boolean isComplete() {
        return samples.size() >= REQUIRED_SAMPLES;
    }

    public int median() {
        if (!isComplete()) {
            throw new IllegalStateException("CALIBRATION_SAMPLES_INCOMPLETE");
        }
        List<Integer> sorted = new ArrayList<>(samples);
        sorted.sort(null);
        return sorted.get(sorted.size() / 2);
    }

    public int spread() {
        if (samples.isEmpty()) {
            return 0;
        }
        int minimum = Collections.min(samples);
        int maximum = Collections.max(samples);
        return maximum - minimum;
    }

    public boolean isStable() {
        return isComplete() && spread() <= MAX_STABLE_SPREAD_RAW;
    }

    public List<Integer> snapshot() {
        return new ArrayList<>(samples);
    }

    public long getLastSampleEpoch() {
        return lastSampleEpoch;
    }

    public void restore(List<Integer> restoredSamples, long restoredLastEpoch) {
        reset();
        if (restoredSamples != null) {
            for (Integer value : restoredSamples) {
                if (value != null && value >= 0 && value <= MAX_ADS1115_RAW
                        && samples.size() < REQUIRED_SAMPLES) {
                    samples.add(value);
                }
            }
        }
        lastSampleEpoch = restoredLastEpoch;
    }

    public void reset() {
        samples.clear();
        lastSampleEpoch = -1L;
    }

    public static boolean isValidCalibration(int dryRaw, int wetRaw) {
        return dryRaw > wetRaw
                && dryRaw - wetRaw >= MIN_CALIBRATION_RANGE_RAW;
    }
}
