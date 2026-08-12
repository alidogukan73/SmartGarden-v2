package com.ali.smartgarden.models;

public class DisplayUnitSettings {
    private String temperature;
    private String area;
    private String length;
    private String volume;
    private String weight;
    private long updated_at_epoch;

    public DisplayUnitSettings() {
        // Firebase requires an empty constructor.
    }

    public DisplayUnitSettings(String temperature, String area, String length,
                               String volume, String weight) {
        this.temperature = temperature;
        this.area = area;
        this.length = length;
        this.volume = volume;
        this.weight = weight;
    }

    public String getTemperature() { return temperature; }
    public String getArea() { return area; }
    public String getLength() { return length; }
    public String getVolume() { return volume; }
    public String getWeight() { return weight; }
    public long getUpdated_at_epoch() { return updated_at_epoch; }

    public void setTemperature(String temperature) { this.temperature = temperature; }
    public void setArea(String area) { this.area = area; }
    public void setLength(String length) { this.length = length; }
    public void setVolume(String volume) { this.volume = volume; }
    public void setWeight(String weight) { this.weight = weight; }
    public void setUpdated_at_epoch(long updatedAtEpoch) { this.updated_at_epoch = updatedAtEpoch; }

    public boolean isComplete() {
        return temperature != null && area != null && length != null
                && volume != null && weight != null;
    }
}
