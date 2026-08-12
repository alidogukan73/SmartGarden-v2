package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.radiobutton.MaterialRadioButton;

/** Controls AVORA's app language without changing stored garden data. */
public class LanguageSettingsActivity extends AppCompatActivity {
    private MaterialRadioButton systemLanguage;
    private MaterialRadioButton turkishLanguage;
    private MaterialRadioButton englishLanguage;
    private TextView activeLanguage;
    private boolean binding;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_language_settings);
        applyWindowInsets();

        systemLanguage = findViewById(R.id.radioLanguageSystem);
        turkishLanguage = findViewById(R.id.radioLanguageTurkish);
        englishLanguage = findViewById(R.id.radioLanguageEnglish);
        activeLanguage = findViewById(R.id.txtActiveLanguage);

        ((TextView) findViewById(R.id.txtSettingsToolbarTitle))
                .setText(R.string.settings_language_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);

        configureOptions();
        bindSelection();
    }

    private void configureOptions() {
        systemLanguage.setOnClickListener(
                view -> select(AvoraLanguageManager.MODE_SYSTEM));
        turkishLanguage.setOnClickListener(
                view -> select(AvoraLanguageManager.MODE_TURKISH));
        ((View) systemLanguage.getParent()).setOnClickListener(
                view -> select(AvoraLanguageManager.MODE_SYSTEM));
        ((View) turkishLanguage.getParent()).setOnClickListener(
                view -> select(AvoraLanguageManager.MODE_TURKISH));

        boolean englishAvailable = AvoraLanguageManager.isEnglishAvailable();
        englishLanguage.setEnabled(englishAvailable);
        ((View) englishLanguage.getParent()).setEnabled(englishAvailable);
        if (englishAvailable) {
            englishLanguage.setOnClickListener(
                    view -> select(AvoraLanguageManager.MODE_ENGLISH));
            ((View) englishLanguage.getParent()).setOnClickListener(
                    view -> select(AvoraLanguageManager.MODE_ENGLISH));
        }
    }

    private void bindSelection() {
        binding = true;
        String mode = AvoraLanguageManager.getLanguageMode(this);
        systemLanguage.setChecked(AvoraLanguageManager.MODE_SYSTEM.equals(mode));
        turkishLanguage.setChecked(AvoraLanguageManager.MODE_TURKISH.equals(mode));
        englishLanguage.setChecked(AvoraLanguageManager.MODE_ENGLISH.equals(mode));
        activeLanguage.setText(getString(R.string.language_active_format,
                getString(labelFor(mode))));
        binding = false;
    }

    private void select(String mode) {
        if (binding) return;
        AvoraLanguageManager.setLanguageMode(this, mode);
        bindSelection();
    }

    private int labelFor(String mode) {
        if (AvoraLanguageManager.MODE_TURKISH.equals(mode)) {
            return R.string.language_option_turkish;
        }
        if (AvoraLanguageManager.MODE_ENGLISH.equals(mode)) {
            return R.string.language_option_english;
        }
        return R.string.language_option_system;
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.languageSettingsRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }
}
