package com.ali.smartgarden.models;

public class FertilizerApplicationSchedule {
    private String product_name = "";
    private long last_application_at_epoch;
    private long next_application_at_epoch;
    public FertilizerApplicationSchedule() {}
    public String getProduct_name() { return product_name; }
    public void setProduct_name(String value) { product_name = value; }
    public long getLast_application_at_epoch() { return last_application_at_epoch; }
    public void setLast_application_at_epoch(long value) { last_application_at_epoch = value; }
    public long getNext_application_at_epoch() { return next_application_at_epoch; }
    public void setNext_application_at_epoch(long value) { next_application_at_epoch = value; }
}
