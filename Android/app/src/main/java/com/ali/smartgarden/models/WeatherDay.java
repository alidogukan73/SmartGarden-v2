package com.ali.smartgarden.models;

public class WeatherDay {
    private final String date;
    private final Double temperatureMax, temperatureMin, rainProbability, rainMm, windMax;
    public WeatherDay(String date, Double temperatureMax, Double temperatureMin, Double rainProbability, Double rainMm, Double windMax) {
        this.date = date == null ? "" : date;
        this.temperatureMax = temperatureMax; this.temperatureMin = temperatureMin; this.rainProbability = rainProbability;
        this.rainMm = rainMm; this.windMax = windMax;
    }
    public String getDate() { return date; }
    public Double getTemperatureMax() { return temperatureMax; }
    public Double getTemperatureMin() { return temperatureMin; }
    public Double getRainProbability() { return rainProbability; }
    public Double getRainMm() { return rainMm; }
    public Double getWindMax() { return windMax; }
}
