package com.ali.smartgarden.models;

public class DisplayUnitSettings {
    public static final String CELSIUS = "celsius";
    public static final String FAHRENHEIT = "fahrenheit";
    public static final String SQUARE_METER = "square_meter";
    public static final String DECARE = "decare";
    public static final String CENTIMETER = "centimeter";
    public static final String METER = "meter";
    public static final String LITER = "liter";
    public static final String CUBIC_METER = "cubic_meter";
    public static final String GRAM = "gram";
    public static final String KILOGRAM = "kilogram";

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
