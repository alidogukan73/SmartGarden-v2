package com.alidogukan.avora.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;


/** Stores and applies AVORA's phone-local language preference. */
public final class AvoraLanguageManager {
    public static final String PREFERENCES_NAME = "avora_language_preferences";
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_TURKISH = "tr";
    public static final String MODE_ENGLISH = "en";

    private static final String KEY_LANGUAGE_MODE = "language_mode";

    private AvoraLanguageManager() {
    }

    public static void applySavedLanguage(@NonNull Context context) {
        applyMode(getLanguageMode(context));
    }

    @NonNull
    public static String getLanguageMode(@NonNull Context context) {
        String value = preferences(context).getString(KEY_LANGUAGE_MODE, MODE_SYSTEM);
        if (MODE_TURKISH.equals(value)) {
            return MODE_TURKISH;
        }
        if (MODE_ENGLISH.equals(value) && isEnglishAvailable()) {
            return MODE_ENGLISH;
        }
        return MODE_SYSTEM;
    }

    public static boolean setLanguageMode(@NonNull Context context, @NonNull String mode) {
        String normalized = normalize(mode);
        if (normalized.equals(getLanguageMode(context))) {
            applyMode(normalized);
            return false;
        }
        preferences(context).edit().putString(KEY_LANGUAGE_MODE, normalized).apply();
        applyMode(normalized);
        return true;
    }

    public static boolean isEnglishAvailable() {
        return true;
    }

    /**
     * Returns a resource context that follows AVORA's saved language even when
     * the caller is WorkManager or a background service without an Activity.
     */
    @NonNull
    public static Context localizedContext(@NonNull Context context) {
        String languageTag = explicitLanguageTag(getLanguageMode(context));
        if (languageTag.isEmpty()) return context;

        Locale locale = Locale.forLanguageTag(languageTag);
        Configuration configuration = new Configuration(
                context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    static String explicitLanguageTag(String mode) {
        String normalized = normalize(mode);
        return MODE_SYSTEM.equals(normalized) ? "" : normalized;
    }


    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static String normalize(String mode) {
        if (MODE_TURKISH.equals(mode)) {
            return MODE_TURKISH;
        }
        if (MODE_ENGLISH.equals(mode) && isEnglishAvailable()) {
            return MODE_ENGLISH;
        }
        return MODE_SYSTEM;
    }

    private static void applyMode(String mode) {
        LocaleListCompat locales;
        if (MODE_TURKISH.equals(mode)) {
            locales = LocaleListCompat.forLanguageTags(MODE_TURKISH);
        } else if (MODE_ENGLISH.equals(mode) && isEnglishAvailable()) {
            locales = LocaleListCompat.forLanguageTags(MODE_ENGLISH);
        } else {
            locales = LocaleListCompat.getEmptyLocaleList();
        }
        AppCompatDelegate.setApplicationLocales(locales);
    }
}
