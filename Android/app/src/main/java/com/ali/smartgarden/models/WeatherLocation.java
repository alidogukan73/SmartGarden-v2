package com.ali.smartgarden.models;

public class WeatherLocation {
    private final String city;
    private final String district;
    private final Double latitude, longitude;
    private final String forecastSource;
    public WeatherLocation(String city, String district) { this(city, district, null, null, "auto"); }
    public WeatherLocation(String city, String district, Double latitude, Double longitude) {
        this(city, district, latitude, longitude, "auto");
    }
    public WeatherLocation(String city, String district, Double latitude, Double longitude, String forecastSource) {
        this.city = city == null ? "" : city;
        this.district = district == null ? "" : district;
        this.latitude = latitude; this.longitude = longitude;
        this.forecastSource = forecastSource == null ? "auto" : forecastSource;
    }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getForecastSource() { return forecastSource; }
}
