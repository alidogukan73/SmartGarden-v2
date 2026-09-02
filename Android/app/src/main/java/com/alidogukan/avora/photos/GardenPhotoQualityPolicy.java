package com.alidogukan.avora.photos;

/** Storage-quality limits for private Plant Journal photos. */
public final class GardenPhotoQualityPolicy {
    public static final int MAX_LONG_EDGE = 2560;
    public static final int JPEG_QUALITY = 94;

    private GardenPhotoQualityPolicy() { }

    public static int decodeSampleSize(int width, int height) {
        int largest = Math.max(width, height);
        int sample = 1;
        while (largest / (sample * 2) >= MAX_LONG_EDGE) sample *= 2;
        return sample;
    }

    public static int[] scaledDimensions(int width, int height) {
        int largest = Math.max(width, height);
        if (largest <= MAX_LONG_EDGE) return new int[]{width, height};
        double scale = (double) MAX_LONG_EDGE / largest;
        return new int[]{
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))
        };
    }
}
