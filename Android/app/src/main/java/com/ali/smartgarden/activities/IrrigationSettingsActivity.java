package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.ali.smartgarden.viewmodels.SettingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class IrrigationSettingsActivity extends AppCompatActivity {

    private static final long DEFAULT_MOISTURE_LIMIT = 40;
    private static final long DEFAULT_PUMP_DURATION = 10;
    private static final long DEFAULT_COOLDOWN_SECONDS = 600;
    private static final long DEFAULT_RESTART_DELTA = 10;
    private static final boolean DEFAULT_SYSTEM_ENABLED = true;
    private static final boolean DEFAULT_AUTO_MODE = true;

    private SettingsViewModel viewModel;

    private MaterialButton btnBack;
    private MaterialButton btnSaveSettings;
    private MaterialButton btnResetSettings;
    private MaterialCardView cardUnsavedChanges;

    private Slider sliderMoistureLimit;
    private Slider sliderPumpDuration;
    private Slider sliderCooldown;
    private Slider sliderRestartDelta;

    private TextView txtMoistureLimitValue;
    private TextView txtPumpDurationValue;
    private TextView txtCooldownValue;
    private TextView txtRestartDeltaValue;
    private TextView txtAutoModeDescription;
    private TextView txtSystemEnabledDescription;
    private TextView txtSettingsStatus;

    private MaterialSwitch switchAutoMode;
    private MaterialSwitch switchSystemEnabled;

    private boolean updatingUi;
    private boolean pendingExitAfterSave;
    private boolean commandLoaded;

    private long originalMoistureLimit = DEFAULT_MOISTURE_LIMIT;
    private long originalPumpDuration = DEFAULT_PUMP_DURATION;
    private long originalCooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
    private long originalRestartDelta = DEFAULT_RESTART_DELTA;
    private boolean originalSystemEnabled = DEFAULT_SYSTEM_ENABLED;
    private boolean originalAutoMode = DEFAULT_AUTO_MODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        initializeListeners();
        observeViewModel();
        initializeActions();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsScreenRoot),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnResetSettings = findViewById(R.id.btnResetSettings);
        cardUnsavedChanges = findViewById(R.id.cardUnsavedChanges);

        sliderMoistureLimit = findViewById(R.id.sliderMoistureLimit);
        sliderPumpDuration = findViewById(R.id.sliderPumpDuration);
        sliderCooldown = findViewById(R.id.sliderCooldown);
        sliderRestartDelta = findViewById(R.id.sliderRestartDelta);

        txtMoistureLimitValue = findViewById(R.id.txtMoistureLimitValue);
        txtPumpDurationValue = findViewById(R.id.txtPumpDurationValue);
        txtCooldownValue = findViewById(R.id.txtCooldownValue);
        txtRestartDeltaValue = findViewById(R.id.txtRestartDeltaValue);
        txtAutoModeDescription = findViewById(R.id.txtAutoModeDescription);
        txtSystemEnabledDescription = findViewById(R.id.txtSystemEnabledDescription);
        txtSettingsStatus = findViewById(R.id.txtSettingsStatus);

        switchAutoMode = findViewById(R.id.switchAutoMode);
        switchSystemEnabled = findViewById(R.id.switchSystemEnabled);
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    }

    private void initializeListeners() {
        sliderMoistureLimit.addOnChangeListener((slider, value, fromUser) -> {
            updateMoistureLimitLabel(Math.round(value));
            onSettingChanged(fromUser);
        });
        sliderPumpDuration.addOnChangeListener((slider, value, fromUser) -> {
            updatePumpDurationLabel(Math.round(value));
            onSettingChanged(fromUser);
        });
        sliderCooldown.addOnChangeListener((slider, value, fromUser) -> {
            updateCooldownLabel(Math.round(value));
            onSettingChanged(fromUser);
        });
        sliderRestartDelta.addOnChangeListener((slider, value, fromUser) -> {
            updateRestartDeltaLabel(Math.round(value));
            onSettingChanged(fromUser);
        });

        switchAutoMode.setOnCheckedChangeListener((button, checked) -> {
            updateAutoModeDescription(checked);
            if (!updatingUi) {
                updateUnsavedState();
            }
        });
        switchSystemEnabled.setOnCheckedChangeListener((button, checked) -> {
            updateSystemEnabledDescription(checked);
            updateAutoModeDescription(switchAutoMode.isChecked());
            if (!updatingUi) {
                updateUnsavedState();
            }
        });
    }

    private void onSettingChanged(boolean fromUser) {
        if (fromUser && !updatingUi) {
            updateUnsavedState();
        }
    }

    private void observeViewModel() {
        viewModel.getCommand().observe(this, this::renderCommand);
        viewModel.getLoading().observe(this, loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            setControlsEnabled(!active);
            txtSettingsStatus.setText(active
                    ? R.string.settings_status_loading
                    : R.string.settings_status_ready);
        });
        viewModel.getSaving().observe(this, saving -> {
            boolean active = Boolean.TRUE.equals(saving);
            setControlsEnabled(!active);
            btnSaveSettings.setText(active
                    ? R.string.settings_saving
                    : R.string.settings_save);
            if (active) {
                txtSettingsStatus.setText(R.string.settings_status_saving);
            }
        });
        viewModel.getSaveSuccess().observe(this, success -> {
            if (!Boolean.TRUE.equals(success)) {
                return;
            }
            saveCurrentValuesAsOriginal();
            updateUnsavedState();
            txtSettingsStatus.setText(R.string.settings_status_saved);
            Toast.makeText(this, R.string.settings_saved_message, Toast.LENGTH_SHORT).show();
            viewModel.clearSaveSuccess();

            if (pendingExitAfterSave) {
                pendingExitAfterSave = false;
                finish();
            }
        });
        viewModel.getError().observe(this, message -> {
            if (message == null || message.isBlank()) {
                return;
            }
            pendingExitAfterSave = false;
            txtSettingsStatus.setText(R.string.settings_status_error);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void initializeActions() {
        btnBack.setOnClickListener(view -> handleBackAction());
        btnSaveSettings.setOnClickListener(view -> confirmAndSave(false));
        btnResetSettings.setOnClickListener(view -> showResetConfirmation());
        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackAction();
                    }
                }
        );
    }

    private void renderCommand(Command command) {
        if (command == null) {
            return;
        }
        if (commandLoaded && hasUnsavedChanges()) {
            return;
        }

        updatingUi = true;
        originalMoistureLimit = positiveOrDefault(
                command.getMoistureLimit(), DEFAULT_MOISTURE_LIMIT, 10, 80);
        originalPumpDuration = clamp(command.getPumpDuration(), 0, 10800);
        originalCooldownSeconds = roundToStep(
                positiveOrDefault(command.getCooldownSeconds(), DEFAULT_COOLDOWN_SECONDS, 300, 3600),
                60);
        originalRestartDelta = positiveOrDefault(
                command.getRestartDelta(), DEFAULT_RESTART_DELTA, 1, 30);
        originalSystemEnabled = command.isEnabled();
        originalAutoMode = command.isAutoMode();

        sliderMoistureLimit.setValue(originalMoistureLimit);
        sliderPumpDuration.setValue(originalPumpDuration);
        sliderCooldown.setValue(originalCooldownSeconds);
        sliderRestartDelta.setValue(originalRestartDelta);
        switchSystemEnabled.setChecked(originalSystemEnabled);
        switchAutoMode.setChecked(originalAutoMode);

        updateMoistureLimitLabel(originalMoistureLimit);
        updatePumpDurationLabel(originalPumpDuration);
        updateCooldownLabel(originalCooldownSeconds);
        updateRestartDeltaLabel(originalRestartDelta);
        updateSystemEnabledDescription(originalSystemEnabled);
        updateAutoModeDescription(originalAutoMode);

        updatingUi = false;
        commandLoaded = true;
        updateUnsavedState();
    }

    private void confirmAndSave(boolean closeAfterSave) {
        if (!hasUnsavedChanges()) {
            if (closeAfterSave) {
                finish();
            } else {
                Toast.makeText(this, R.string.settings_no_changes, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.irrigation_settings_sync_title)
                .setMessage(R.string.irrigation_settings_sync_message)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.irrigation_settings_sync_confirm,
                        (dialog, which) -> performSave(closeAfterSave))
                .show();
    }

    private void performSave(boolean closeAfterSave) {
        pendingExitAfterSave = closeAfterSave;
        viewModel.saveSettings(
                getMoistureLimit(),
                getPumpDuration(),
                getCooldownSeconds(),
                getRestartDelta(),
                switchSystemEnabled.isChecked(),
                switchAutoMode.isChecked()
        );
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_reset_dialog_title)
                .setMessage(R.string.settings_reset_dialog_message)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_reset_confirm,
                        (dialog, which) -> applyDefaultValuesToUi())
                .show();
    }

    private void applyDefaultValuesToUi() {
        updatingUi = true;
        sliderMoistureLimit.setValue(DEFAULT_MOISTURE_LIMIT);
        sliderPumpDuration.setValue(DEFAULT_PUMP_DURATION);
        sliderCooldown.setValue(DEFAULT_COOLDOWN_SECONDS);
        sliderRestartDelta.setValue(DEFAULT_RESTART_DELTA);
        switchSystemEnabled.setChecked(DEFAULT_SYSTEM_ENABLED);
        switchAutoMode.setChecked(DEFAULT_AUTO_MODE);

        updateMoistureLimitLabel(DEFAULT_MOISTURE_LIMIT);
        updatePumpDurationLabel(DEFAULT_PUMP_DURATION);
        updateCooldownLabel(DEFAULT_COOLDOWN_SECONDS);
        updateRestartDeltaLabel(DEFAULT_RESTART_DELTA);
        updateSystemEnabledDescription(DEFAULT_SYSTEM_ENABLED);
        updateAutoModeDescription(DEFAULT_AUTO_MODE);
        updatingUi = false;

        updateUnsavedState();
        txtSettingsStatus.setText(R.string.settings_defaults_applied);
    }

    private void handleBackAction() {
        if (!hasUnsavedChanges()) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_unsaved_dialog_title)
                .setMessage(R.string.irrigation_settings_unsaved_sync_message)
                .setNegativeButton(R.string.settings_continue_editing, null)
                .setNeutralButton(R.string.settings_discard_changes,
                        (dialog, which) -> finish())
                .setPositiveButton(R.string.settings_save_and_exit,
                        (dialog, which) -> performSave(true))
                .show();
    }

    private void updateUnsavedState() {
        boolean changed = hasUnsavedChanges();
        cardUnsavedChanges.setVisibility(changed ? View.VISIBLE : View.GONE);
        btnSaveSettings.setEnabled(changed);
        txtSettingsStatus.setText(changed
                ? R.string.settings_status_unsaved
                : R.string.settings_status_ready);
    }

    private boolean hasUnsavedChanges() {
        return getMoistureLimit() != originalMoistureLimit
                || getPumpDuration() != originalPumpDuration
                || getCooldownSeconds() != originalCooldownSeconds
                || getRestartDelta() != originalRestartDelta
                || switchSystemEnabled.isChecked() != originalSystemEnabled
                || switchAutoMode.isChecked() != originalAutoMode;
    }

    private void saveCurrentValuesAsOriginal() {
        originalMoistureLimit = getMoistureLimit();
        originalPumpDuration = getPumpDuration();
        originalCooldownSeconds = getCooldownSeconds();
        originalRestartDelta = getRestartDelta();
        originalSystemEnabled = switchSystemEnabled.isChecked();
        originalAutoMode = switchAutoMode.isChecked();
    }

    private void setControlsEnabled(boolean enabled) {
        sliderMoistureLimit.setEnabled(enabled);
        sliderPumpDuration.setEnabled(enabled);
        sliderCooldown.setEnabled(enabled);
        sliderRestartDelta.setEnabled(enabled);
        switchAutoMode.setEnabled(enabled);
        switchSystemEnabled.setEnabled(enabled);
        btnResetSettings.setEnabled(enabled);
        btnSaveSettings.setEnabled(enabled && hasUnsavedChanges());
    }

    private long getMoistureLimit() {
        return Math.round(sliderMoistureLimit.getValue());
    }

    private long getPumpDuration() {
        return Math.round(sliderPumpDuration.getValue());
    }

    private long getCooldownSeconds() {
        return Math.round(sliderCooldown.getValue());
    }

    private long getRestartDelta() {
        return Math.round(sliderRestartDelta.getValue());
    }

    private void updateMoistureLimitLabel(long value) {
        txtMoistureLimitValue.setText(
                getString(R.string.settings_percentage_format, value));
    }

    private void updatePumpDurationLabel(long value) {
        txtPumpDurationValue.setText(formatDuration(value));
    }

    private void updateCooldownLabel(long value) {
        txtCooldownValue.setText(formatDuration(value));
    }

    private void updateRestartDeltaLabel(long value) {
        txtRestartDeltaValue.setText(
                getString(R.string.settings_percentage_format, value));
    }

    private void updateSystemEnabledDescription(boolean enabled) {
        txtSystemEnabledDescription.setText(enabled
                ? R.string.settings_system_enabled_active
                : R.string.settings_system_enabled_inactive);
        txtSystemEnabledDescription.setTextColor(getColor(
                enabled ? R.color.online : R.color.offline));
    }

    private void updateAutoModeDescription(boolean autoMode) {
        if (!switchSystemEnabled.isChecked()) {
            txtAutoModeDescription.setText(R.string.irrigation_settings_auto_mode_blocked);
            txtAutoModeDescription.setTextColor(getColor(R.color.textSecondary));
            return;
        }
        txtAutoModeDescription.setText(autoMode
                ? R.string.irrigation_settings_auto_mode_active
                : R.string.irrigation_settings_auto_mode_inactive);
        txtAutoModeDescription.setTextColor(getColor(
                autoMode ? R.color.online : R.color.textSecondary));
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        if (safeSeconds < 60) {
            return getString(R.string.settings_seconds_format, safeSeconds);
        }
        if (safeSeconds >= 3600) {
            long hours = safeSeconds / 3600;
            long minutes = (safeSeconds % 3600) / 60;
            return minutes == 0
                    ? getString(R.string.settings_hours_format, hours)
                    : getString(R.string.settings_hours_minutes_format, hours, minutes);
        }
        long minutes = safeSeconds / 60;
        long remainingSeconds = safeSeconds % 60;
        return remainingSeconds == 0
                ? getString(R.string.settings_minutes_format, minutes)
                : getString(R.string.settings_minutes_seconds_format, minutes, remainingSeconds);
    }

    private long positiveOrDefault(long value, long defaultValue, long min, long max) {
        return value <= 0 ? defaultValue : clamp(value, min, max);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private long roundToStep(long value, long step) {
        return Math.round((double) value / step) * step;
    }
}