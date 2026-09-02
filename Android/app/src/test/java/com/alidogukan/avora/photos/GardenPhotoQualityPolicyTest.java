package com.alidogukan.avora.photos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GardenPhotoQualityPolicyTest {
    @Test
    public void keepsImagesThatAlreadyFitTheJournalLimit() {
        assertEquals(1, GardenPhotoQualityPolicy.decodeSampleSize(1600, 1200));
        assertArrayEquals(new int[]{1600, 1200},
                GardenPhotoQualityPolicy.scaledDimensions(1600, 1200));
    }

    @Test
    public void decodesLargeImagesNearTheTargetBeforeExactScaling() {
        assertEquals(2, GardenPhotoQualityPolicy.decodeSampleSize(6000, 4000));
        assertArrayEquals(new int[]{2560, 1707},
                GardenPhotoQualityPolicy.scaledDimensions(3000, 2000));
    }

    @Test
    public void preservesPortraitAspectRatio() {
        assertArrayEquals(new int[]{1440, 2560},
                GardenPhotoQualityPolicy.scaledDimensions(2250, 4000));
    }
}
