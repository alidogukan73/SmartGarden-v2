package com.ali.smartgarden.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.ali.smartgarden.models.GardenProfile;

public final class GardenProfileStore {
    private static final String PREFS = "avora_garden_profile";
    private static final String NAME = "garden_name";
    private static final String TYPE = "garden_type";
    private static final String AREA = "area_square_meters";
    private static final String NOTES = "notes";
    private static final String UPDATED = "updated_at_epoch";

    private final SharedPreferences preferences;

    public GardenProfileStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public GardenProfile load() {
        return new GardenProfile(
                preferences.getString(NAME, "AVORA Bahçesi"),
                preferences.getString(TYPE, "Açık alan"),
                Double.longBitsToDouble(preferences.getLong(
                        AREA, Double.doubleToRawLongBits(0d))),
                preferences.getString(NOTES, ""),
                preferences.getLong(UPDATED, 0L)
        );
    }

    public void save(GardenProfile profile) {
        preferences.edit()
                .putString(NAME, profile.getGarden_name())
                .putString(TYPE, profile.getGarden_type())
                .putLong(AREA, Double.doubleToRawLongBits(profile.getArea_square_meters()))
                .putString(NOTES, profile.getNotes())
                .putLong(UPDATED, profile.getUpdated_at_epoch())
                .apply();
    }
}
