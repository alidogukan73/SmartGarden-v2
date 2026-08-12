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
import com.ali.smartgarden.theme.AvoraThemeManager;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.radiobutton.MaterialRadioButton;

/** Lets the user switch AVORA's appearance and applies the choice immediately. */
public class ThemeSettingsActivity extends AppCompatActivity {
    private MaterialRadioButton systemTheme;
    private MaterialRadioButton lightTheme;
    private MaterialRadioButton darkTheme;
    private TextView activeTheme;
    private boolean binding;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_theme_settings);
        applyWindowInsets();

        systemTheme = findViewById(R.id.radioThemeSystem);
        lightTheme = findViewById(R.id.radioThemeLight);
        darkTheme = findViewById(R.id.radioThemeDark);
        activeTheme = findViewById(R.id.txtActiveTheme);

        ((TextView) findViewById(R.id.txtSettingsToolbarTitle))
                .setText(R.string.settings_theme_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);

        bindSelection();
        systemTheme.setOnClickListener(view -> select(AvoraThemeManager.MODE_SYSTEM));
        lightTheme.setOnClickListener(view -> select(AvoraThemeManager.MODE_LIGHT));
        darkTheme.setOnClickListener(view -> select(AvoraThemeManager.MODE_DARK));
        ((View) systemTheme.getParent()).setOnClickListener(
                view -> select(AvoraThemeManager.MODE_SYSTEM));
        ((View) lightTheme.getParent()).setOnClickListener(
                view -> select(AvoraThemeManager.MODE_LIGHT));
        ((View) darkTheme.getParent()).setOnClickListener(
                view -> select(AvoraThemeManager.MODE_DARK));
    }

    private void bindSelection() {
        binding = true;
        String mode = AvoraThemeManager.getThemeMode(this);
        systemTheme.setChecked(AvoraThemeManager.MODE_SYSTEM.equals(mode));
        lightTheme.setChecked(AvoraThemeManager.MODE_LIGHT.equals(mode));
        darkTheme.setChecked(AvoraThemeManager.MODE_DARK.equals(mode));
        activeTheme.setText(getString(R.string.theme_active_format,
                getString(labelFor(mode))));
        binding = false;
    }

    private void select(String mode) {
        if (binding) return;
        AvoraThemeManager.setThemeMode(this, mode);
        bindSelection();
    }

    private int labelFor(String mode) {
        if (AvoraThemeManager.MODE_LIGHT.equals(mode)) {
            return R.string.theme_option_light;
        }
        if (AvoraThemeManager.MODE_DARK.equals(mode)) {
            return R.string.theme_option_dark;
        }
        return R.string.theme_option_system;
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.themeSettingsRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }
}
