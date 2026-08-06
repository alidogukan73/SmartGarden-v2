package com.ali.smartgarden.models;

public class GardenPhoto {
    private String id;
    private String zone_id;
    private String image_url;
    private String local_path;
    private String note;
    private String related_application_id;
    private long captured_at_epoch;

    public GardenPhoto() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getZone_id() { return zone_id; }
    public void setZone_id(String zoneId) { zone_id = zoneId; }
    public String getImage_url() { return image_url; }
    public void setImage_url(String imageUrl) { image_url = imageUrl; }
    public String getLocal_path() { return local_path; }
    public void setLocal_path(String value) { local_path = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
    public String getRelated_application_id() {
        return related_application_id;
    }
    public void setRelated_application_id(String value) {
        related_application_id = value;
    }
    public long getCaptured_at_epoch() { return captured_at_epoch; }
    public void setCaptured_at_epoch(long value) { captured_at_epoch = value; }
}
