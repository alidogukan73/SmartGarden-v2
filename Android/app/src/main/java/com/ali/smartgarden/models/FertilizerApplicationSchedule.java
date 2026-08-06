package com.ali.smartgarden.models;

public class FertilizerApplicationSchedule {
    private String product_id = "";
    private String product_name = "";
    private long last_application_at_epoch;
    private long next_application_at_epoch;
    private int interval_days;
    public FertilizerApplicationSchedule() {}
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String value) { product_id = value; }
    public String getProduct_name() { return product_name; }
    public void setProduct_name(String value) { product_name = value; }
    public long getLast_application_at_epoch() { return last_application_at_epoch; }
    public void setLast_application_at_epoch(long value) { last_application_at_epoch = value; }
    public long getNext_application_at_epoch() { return next_application_at_epoch; }
    public void setNext_application_at_epoch(long value) { next_application_at_epoch = value; }
    public int getInterval_days() { return interval_days; }
    public void setInterval_days(int value) { interval_days = value; }
}
