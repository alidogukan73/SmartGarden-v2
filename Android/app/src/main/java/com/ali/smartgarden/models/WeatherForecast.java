package com.ali.smartgarden.models;

import java.util.Collections;
import java.util.List;

public class WeatherForecast {
    private final String city, district;
    private final Double tomorrowTemperatureMax, tomorrowRainProbability, tomorrowRainMm, tomorrowWindMax;
    private final Double todayTemperatureMax, todayRainProbability, todayRainMm, todayWindMax;
    private final List<WeatherDay> days;
    private final Long todayWeatherCode, tomorrowWeatherCode;
    private final Double currentTemperature, currentHumidity, currentWind, currentPressure;
    private final Long currentWeatherCode;
    private String source = "";
    private long updatedAtEpoch;
    public WeatherForecast(String city, String district, Double tomorrowTemperatureMax,
                           Double tomorrowRainProbability, Double tomorrowRainMm, Double tomorrowWindMax) {
        this.city = city == null ? "" : city;
        this.district = district == null ? "" : district;
        this.tomorrowTemperatureMax = tomorrowTemperatureMax;
        this.tomorrowRainProbability = tomorrowRainProbability;
        this.tomorrowRainMm = tomorrowRainMm;
        this.tomorrowWindMax = tomorrowWindMax;
        this.todayTemperatureMax = null; this.todayRainProbability = null;
        this.todayRainMm = null; this.todayWindMax = null; this.days = Collections.emptyList();
        this.todayWeatherCode = null; this.tomorrowWeatherCode = null;
        this.currentTemperature = null; this.currentHumidity = null; this.currentWind = null;
        this.currentPressure = null; this.currentWeatherCode = null;
    }
    public WeatherForecast(String city, String district, Double todayTemperatureMax,
                           Double todayRainProbability, Double todayRainMm, Double todayWindMax,
                           Double tomorrowTemperatureMax, Double tomorrowRainProbability,
                           Double tomorrowRainMm, Double tomorrowWindMax, List<WeatherDay> days,
                           Long todayWeatherCode, Long tomorrowWeatherCode, Double currentTemperature,
                           Double currentHumidity, Double currentWind, Double currentPressure, Long currentWeatherCode) {
        this.city = city == null ? "" : city; this.district = district == null ? "" : district;
        this.todayTemperatureMax = todayTemperatureMax; this.todayRainProbability = todayRainProbability;
        this.todayRainMm = todayRainMm; this.todayWindMax = todayWindMax;
        this.tomorrowTemperatureMax = tomorrowTemperatureMax; this.tomorrowRainProbability = tomorrowRainProbability;
        this.tomorrowRainMm = tomorrowRainMm; this.tomorrowWindMax = tomorrowWindMax;
        this.days = days == null ? Collections.emptyList() : days;
        this.todayWeatherCode = todayWeatherCode; this.tomorrowWeatherCode = tomorrowWeatherCode;
        this.currentTemperature = currentTemperature; this.currentHumidity = currentHumidity;
        this.currentWind = currentWind; this.currentPressure = currentPressure; this.currentWeatherCode = currentWeatherCode;
    }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public Double getTomorrowTemperatureMax() { return tomorrowTemperatureMax; }
    public Double getTomorrowRainProbability() { return tomorrowRainProbability; }
    public Double getTomorrowRainMm() { return tomorrowRainMm; }
    public Double getTomorrowWindMax() { return tomorrowWindMax; }
    public Double getTodayTemperatureMax() { return todayTemperatureMax; }
    public Double getTodayRainProbability() { return todayRainProbability; }
    public Double getTodayRainMm() { return todayRainMm; }
    public Double getTodayWindMax() { return todayWindMax; }
    public List<WeatherDay> getDays() { return days; }
    public Long getTodayWeatherCode() { return todayWeatherCode; }
    public Long getTomorrowWeatherCode() { return tomorrowWeatherCode; }
    public Double getCurrentTemperature() { return currentTemperature; }
    public Double getCurrentHumidity() { return currentHumidity; }
    public Double getCurrentWind() { return currentWind; }
    public Double getCurrentPressure() { return currentPressure; }
    public Long getCurrentWeatherCode() { return currentWeatherCode; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source == null ? "" : source; }
    public long getUpdatedAtEpoch() { return updatedAtEpoch; }
    public void setUpdatedAtEpoch(long value) { updatedAtEpoch = value; }
}
