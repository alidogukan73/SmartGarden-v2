package com.ali.smartgarden.plantdoctor;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the most recent actionable plant-doctor recommendation on this phone. */
public final class PlantDoctorRecommendationStore {
    private static final String PREFS = "plant_doctor_recommendation";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ADVICE = "advice";

    private PlantDoctorRecommendationStore() { }

    public static void save(Context context, String title, String advice) {
        String cleanAdvice = clean(advice);
        if (cleanAdvice.isEmpty()) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TITLE, clean(title))
                .putString(KEY_ADVICE, cleanAdvice)
                .apply();
    }

    public static String summary(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
        String advice = clean(preferences.getString(KEY_ADVICE, ""));
        if (advice.isEmpty()) return "Şu an öneri yok";
        String title = clean(preferences.getString(KEY_TITLE, ""));
        String text = title.isEmpty() ? advice : title + " · " + advice;
        return text.length() <= 145 ? text : text.substring(0, 142).trim() + "…";
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
