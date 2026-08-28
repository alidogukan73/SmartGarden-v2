package com.ali.smartgarden.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.ali.smartgarden.models.DisplayUnitSettings;

import java.text.DecimalFormat;

/**
 * Stores display-only units. Sensor, irrigation and Firebase measurements keep their
 * canonical metric values so changing a unit can never change an automation decision.
 */
public final class UnitPreferences {
    public static final String CELSIUS = DisplayUnitSettings.CELSIUS;
    public static final String FAHRENHEIT = DisplayUnitSettings.FAHRENHEIT;
    public static final String SQUARE_METER = DisplayUnitSettings.SQUARE_METER;
    public static final String DECARE = DisplayUnitSettings.DECARE;
    public static final String CENTIMETER = DisplayUnitSettings.CENTIMETER;
    public static final String METER = DisplayUnitSettings.METER;
    public static final String LITER = DisplayUnitSettings.LITER;
    public static final String CUBIC_METER = DisplayUnitSettings.CUBIC_METER;
    public static final String GRAM = DisplayUnitSettings.GRAM;
    public static final String KILOGRAM = DisplayUnitSettings.KILOGRAM;

    private static final String PREFS = "avora_display_units";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_AREA = "area";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_WEIGHT = "weight";

    private final SharedPreferences preferences;

    public UnitPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public DisplayUnitSettings load() {
        return new DisplayUnitSettings(
                preferences.getString(KEY_TEMPERATURE, CELSIUS),
                preferences.getString(KEY_AREA, SQUARE_METER),
                preferences.getString(KEY_LENGTH, CENTIMETER),
                preferences.getString(KEY_VOLUME, LITER),
                preferences.getString(KEY_WEIGHT, GRAM)
        );
    }

    public boolean hasSavedValues() {
        return preferences.contains(KEY_TEMPERATURE)
                && preferences.contains(KEY_AREA)
                && preferences.contains(KEY_LENGTH)
                && preferences.contains(KEY_VOLUME)
                && preferences.contains(KEY_WEIGHT);
    }
    public void save(DisplayUnitSettings settings) {
        preferences.edit()
                .putString(KEY_TEMPERATURE, safe(settings.getTemperature(), CELSIUS))
                .putString(KEY_AREA, safe(settings.getArea(), SQUARE_METER))
                .putString(KEY_LENGTH, safe(settings.getLength(), CENTIMETER))
                .putString(KEY_VOLUME, safe(settings.getVolume(), LITER))
                .putString(KEY_WEIGHT, safe(settings.getWeight(), GRAM))
                .apply();
    }

    public void reset() {
        preferences.edit().clear().apply();
    }

    public double areaFromSquareMeters(double value) {
        return DECARE.equals(load().getArea()) ? value / 1000d : value;
    }

    public double areaToSquareMeters(double displayedValue) {
        return DECARE.equals(load().getArea()) ? displayedValue * 1000d : displayedValue;
    }

    public String areaSymbol() {
        return DECARE.equals(load().getArea()) ? "da" : "m²";
    }

    public String formatTemperature(double celsius) {
        DisplayUnitSettings value = load();
        double displayed = FAHRENHEIT.equals(value.getTemperature())
                ? (celsius * 9d / 5d) + 32d : celsius;
        return number(displayed) + (FAHRENHEIT.equals(value.getTemperature()) ? " °F" : " °C");
    }

    public String formatArea(double squareMeters) {
        return number(areaFromSquareMeters(squareMeters)) + " " + areaSymbol();
    }

    public String formatLength(double centimeters) {
        DisplayUnitSettings value = load();
        double displayed = METER.equals(value.getLength()) ? centimeters / 100d : centimeters;
        return number(displayed) + (METER.equals(value.getLength()) ? " m" : " cm");
    }

    public String formatVolume(double liters) {
        DisplayUnitSettings value = load();
        double displayed = CUBIC_METER.equals(value.getVolume()) ? liters / 1000d : liters;
        return number(displayed) + (CUBIC_METER.equals(value.getVolume()) ? " m³" : " L");
    }

    public String formatWeight(double grams) {
        DisplayUnitSettings value = load();
        double displayed = KILOGRAM.equals(value.getWeight()) ? grams / 1000d : grams;
        return number(displayed) + (KILOGRAM.equals(value.getWeight()) ? " kg" : " g");
    }

    private String number(double value) {
        return new DecimalFormat("0.##").format(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
