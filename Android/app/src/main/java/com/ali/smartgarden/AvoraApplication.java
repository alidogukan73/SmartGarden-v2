package com.ali.smartgarden;

import android.app.Application;

import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.theme.AvoraThemeManager;

/** Application entry point for preferences that must be active before a screen is drawn. */
public class AvoraApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AvoraLanguageManager.applySavedLanguage(this);
        AvoraThemeManager.applySavedTheme(this);
    }
}
