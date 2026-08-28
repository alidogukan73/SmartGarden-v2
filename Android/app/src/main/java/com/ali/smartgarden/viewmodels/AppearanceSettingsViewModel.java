package com.ali.smartgarden.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.theme.AvoraThemeManager;

/** Persistence boundary for language and theme preferences. */
public final class AppearanceSettingsViewModel extends AndroidViewModel {
    public static final String LANGUAGE_SYSTEM = AvoraLanguageManager.MODE_SYSTEM;
    public static final String LANGUAGE_TURKISH = AvoraLanguageManager.MODE_TURKISH;
    public static final String LANGUAGE_ENGLISH = AvoraLanguageManager.MODE_ENGLISH;
    public static final String THEME_SYSTEM = AvoraThemeManager.MODE_SYSTEM;
    public static final String THEME_LIGHT = AvoraThemeManager.MODE_LIGHT;
    public static final String THEME_DARK = AvoraThemeManager.MODE_DARK;

    public AppearanceSettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean englishAvailable() { return AvoraLanguageManager.isEnglishAvailable(); }
    public String languageMode() {
        return AvoraLanguageManager.getLanguageMode(getApplication());
    }
    public void setLanguageMode(String mode) {
        AvoraLanguageManager.setLanguageMode(getApplication(), mode);
    }
    public String themeMode() { return AvoraThemeManager.getThemeMode(getApplication()); }
    public void setThemeMode(String mode) {
        AvoraThemeManager.setThemeMode(getApplication(), mode);
    }
}
