package com.alidogukan.avora.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/** Stores and applies AVORA's phone-local appearance preference. */
public final class AvoraThemeManager {
    public static final String PREFERENCES_NAME = "avora_theme_preferences";
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";

    private static final String KEY_THEME_MODE = "theme_mode";

    private AvoraThemeManager() {
    }

    public static void applySavedTheme(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(delegateMode(getThemeMode(context)));
    }

    @NonNull
    public static String getThemeMode(@NonNull Context context) {
        String value = preferences(context).getString(KEY_THEME_MODE, MODE_SYSTEM);
        if (MODE_LIGHT.equals(value) || MODE_DARK.equals(value)) {
            return value;
        }
        return MODE_SYSTEM;
    }

    public static boolean setThemeMode(@NonNull Context context, @NonNull String mode) {
        String normalized = normalize(mode);
        if (normalized.equals(getThemeMode(context))) {
            return false;
        }
        preferences(context).edit().putString(KEY_THEME_MODE, normalized).apply();
        AppCompatDelegate.setDefaultNightMode(delegateMode(normalized));
        return true;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static String normalize(String mode) {
        if (MODE_LIGHT.equals(mode) || MODE_DARK.equals(mode)) {
            return mode;
        }
        return MODE_SYSTEM;
    }

    private static int delegateMode(String mode) {
        if (MODE_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (MODE_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
