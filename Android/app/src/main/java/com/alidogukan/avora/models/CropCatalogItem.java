package com.alidogukan.avora.models;

import androidx.annotation.NonNull;

/** A reusable crop definition. Seasons copy its current values as an immutable snapshot. */
public class CropCatalogItem {
    public static final String SOURCE_SYSTEM = "SYSTEM";
    public static final String SOURCE_USER = "USER";

    private String crop_id;
    private String name;
    private String emoji;
    private String plant_type;
    private int ideal_moisture_min;
    private int ideal_moisture_max;
    private String source;
    private boolean enabled;
    private long created_at_epoch;
    private long updated_at_epoch;

    public CropCatalogItem() { }

    public CropCatalogItem(String cropId, String name, String emoji, String plantType,
                           int idealMoistureMin, int idealMoistureMax,
                           String source, boolean enabled) {
        this.crop_id = cropId;
        this.name = name;
        this.emoji = emoji;
        this.plant_type = plantType;
        this.ideal_moisture_min = idealMoistureMin;
        this.ideal_moisture_max = idealMoistureMax;
        this.source = source;
        this.enabled = enabled;
    }

    public String getCrop_id() { return crop_id; }
    public void setCrop_id(String cropId) { this.crop_id = cropId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getPlant_type() { return plant_type; }
    public void setPlant_type(String plantType) { this.plant_type = plantType; }
    public int getIdeal_moisture_min() { return ideal_moisture_min; }
    public void setIdeal_moisture_min(int value) { this.ideal_moisture_min = value; }
    public int getIdeal_moisture_max() { return ideal_moisture_max; }
    public void setIdeal_moisture_max(int value) { this.ideal_moisture_max = value; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getCreated_at_epoch() { return created_at_epoch; }
    public void setCreated_at_epoch(long value) { this.created_at_epoch = value; }
    public long getUpdated_at_epoch() { return updated_at_epoch; }
    public void setUpdated_at_epoch(long value) { this.updated_at_epoch = value; }

    public boolean isSystemItem() { return SOURCE_SYSTEM.equalsIgnoreCase(source); }

    @NonNull
    @Override
    public String toString() {
        String symbol = emoji == null || emoji.isBlank() ? "🌱" : emoji;
        return symbol + " " + (name == null ? "" : name);
    }
}
