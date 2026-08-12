package com.ali.smartgarden.models;

public class GardenProfile {
    private String garden_name;
    private String garden_type;
    private double area_square_meters;
    private String notes;
    private long updated_at_epoch;

    public GardenProfile() {
        // Firebase requires an empty constructor.
    }

    public GardenProfile(String gardenName, String gardenType, double areaSquareMeters,
                         String notes, long updatedAtEpoch) {
        this.garden_name = gardenName;
        this.garden_type = gardenType;
        this.area_square_meters = areaSquareMeters;
        this.notes = notes;
        this.updated_at_epoch = updatedAtEpoch;
    }

    public String getGarden_name() { return garden_name; }
    public String getGarden_type() { return garden_type; }
    public double getArea_square_meters() { return area_square_meters; }
    public String getNotes() { return notes; }
    public long getUpdated_at_epoch() { return updated_at_epoch; }

    public void setGarden_name(String gardenName) { this.garden_name = gardenName; }
    public void setGarden_type(String gardenType) { this.garden_type = gardenType; }
    public void setArea_square_meters(double areaSquareMeters) { this.area_square_meters = areaSquareMeters; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setUpdated_at_epoch(long updatedAtEpoch) { this.updated_at_epoch = updatedAtEpoch; }

    public boolean hasData() {
        return garden_name != null && !garden_name.trim().isEmpty();
    }
}
