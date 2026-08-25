package com.ali.smartgarden.calibration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SensorCalibrationSamplerTest {

    @Test
    public void usesFiveDistinctUpdatesAndMedian() {
        SensorCalibrationSampler sampler = new SensorCalibrationSampler();

        assertTrue(sampler.addSample(16000, 1));
        assertFalse(sampler.addSample(12000, 1));
        assertTrue(sampler.addSample(16100, 2));
        assertTrue(sampler.addSample(15950, 3));
        assertTrue(sampler.addSample(16040, 4));
        assertTrue(sampler.addSample(15990, 5));

        assertTrue(sampler.isComplete());
        assertTrue(sampler.isStable());
        assertEquals(16000, sampler.median());
        assertEquals(150, sampler.spread());
    }

    @Test
    public void rejectsUnstableAndNarrowCalibration() {
        SensorCalibrationSampler sampler = new SensorCalibrationSampler();
        sampler.addSample(12000, 1);
        sampler.addSample(14000, 2);
        sampler.addSample(16000, 3);
        sampler.addSample(13000, 4);
        sampler.addSample(15000, 5);

        assertFalse(sampler.isStable());
        assertFalse(SensorCalibrationSampler.isValidCalibration(10000, 9800));
        assertFalse(SensorCalibrationSampler.isValidCalibration(5000, 10000));
        assertTrue(SensorCalibrationSampler.isValidCalibration(16000, 4000));
    }
}
