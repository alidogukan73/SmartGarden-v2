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
import com.ali.smartgarden.viewmodels.SettingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsActivity extends AppCompatActivity {

    private static final long DEFAULT_MOISTURE_LIMIT = 40;
    private static final long DEFAULT_PUMP_DURATION = 10;
    private static final long DEFAULT_COOLDOWN_SECONDS = 120;
    private static final long DEFAULT_RESTART_DELTA = 10;
    private static final boolean DEFAULT_SYSTEM_ENABLED = true;

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

    private MaterialSwitch switchSystemEnabled;
    private TextView txtSystemEnabledDescription;
    private TextView txtSettingsStatus;

    /*
     * Firebase değerleri ekrana yüklenirken slider listener'larının
     * yanlışlıkla "değişiklik var" durumunu tetiklemesini engeller.
     */
    private boolean updatingUi = false;

    /*
     * Ekrana en son Firebase'den yüklenen değerler.
     * Kullanıcının değişiklik yapıp yapmadığını bunlarla karşılaştıracağız.
     */
    private long originalMoistureLimit = DEFAULT_MOISTURE_LIMIT;
    private long originalPumpDuration = DEFAULT_PUMP_DURATION;
    private long originalCooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
    private long originalRestartDelta = DEFAULT_RESTART_DELTA;
    private boolean originalSystemEnabled = DEFAULT_SYSTEM_ENABLED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_settings);

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        initializeListeners();
        observeViewModel();
        initializeActions();
    }


    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsRoot),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }


    private void initializeViews() {

        btnBack =
                findViewById(R.id.btnBack);

        btnSaveSettings =
                findViewById(R.id.btnSaveSettings);

        btnResetSettings =
                findViewById(R.id.btnResetSettings);

        cardUnsavedChanges =
                findViewById(R.id.cardUnsavedChanges);

        sliderMoistureLimit =
                findViewById(R.id.sliderMoistureLimit);

        sliderPumpDuration =
                findViewById(R.id.sliderPumpDuration);

        sliderCooldown =
                findViewById(R.id.sliderCooldown);

        sliderRestartDelta =
                findViewById(R.id.sliderRestartDelta);

        txtMoistureLimitValue =
                findViewById(R.id.txtMoistureLimitValue);

        txtPumpDurationValue =
                findViewById(R.id.txtPumpDurationValue);

        txtCooldownValue =
                findViewById(R.id.txtCooldownValue);

        txtRestartDeltaValue =
                findViewById(R.id.txtRestartDeltaValue);

        switchSystemEnabled =
                findViewById(R.id.switchSystemEnabled);

        txtSystemEnabledDescription =
                findViewById(R.id.txtSystemEnabledDescription);

        txtSettingsStatus =
                findViewById(R.id.txtSettingsStatus);
    }


    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(SettingsViewModel.class);
    }


    private void initializeListeners() {

        sliderMoistureLimit.addOnChangeListener(
                (slider, value, fromUser) -> {

                    updateMoistureLimitLabel(
                            Math.round(value)
                    );

                    if (fromUser && !updatingUi) {
                        updateUnsavedState();
                    }
                }
        );

        sliderPumpDuration.addOnChangeListener(
                (slider, value, fromUser) -> {

                    updatePumpDurationLabel(
                            Math.round(value)
                    );

                    if (fromUser && !updatingUi) {
                        updateUnsavedState();
                    }
                }
        );

        sliderCooldown.addOnChangeListener(
                (slider, value, fromUser) -> {

                    updateCooldownLabel(
                            Math.round(value)
                    );

                    if (fromUser && !updatingUi) {
                        updateUnsavedState();
                    }
                }
        );

        sliderRestartDelta.addOnChangeListener(
                (slider, value, fromUser) -> {

                    updateRestartDeltaLabel(
                            Math.round(value)
                    );

                    if (fromUser && !updatingUi) {
                        updateUnsavedState();
                    }
                }
        );

        switchSystemEnabled.setOnCheckedChangeListener(
                (buttonView, checked) -> {

                    updateSystemEnabledDescription(
                            checked
                    );

                    if (!updatingUi) {
                        updateUnsavedState();
                    }
                }
        );
    }


    private void observeViewModel() {

        viewModel.getCommand().observe(
                this,
                this::renderCommand
        );

        viewModel.getLoading().observe(
                this,
                loading -> {

                    boolean isLoading =
                            Boolean.TRUE.equals(loading);

                    setControlsEnabled(
                            !isLoading
                    );

                    txtSettingsStatus.setText(
                            isLoading
                                    ? R.string.settings_status_loading
                                    : R.string.settings_status_ready
                    );
                }
        );

        viewModel.getSaving().observe(
                this,
                saving -> {

                    boolean isSaving =
                            Boolean.TRUE.equals(saving);

                    setControlsEnabled(
                            !isSaving
                    );

                    btnSaveSettings.setText(
                            isSaving
                                    ? R.string.settings_saving
                                    : R.string.settings_save
                    );

                    if (isSaving) {

                        txtSettingsStatus.setText(
                                R.string.settings_status_saving
                        );
                    }
                }
        );

        viewModel.getSaveSuccess().observe(
                this,
                success -> {

                    if (!Boolean.TRUE.equals(success)) {
                        return;
                    }

                    saveCurrentValuesAsOriginal();

                    updateUnsavedState();

                    txtSettingsStatus.setText(
                            R.string.settings_status_saved
                    );

                    Toast.makeText(
                            this,
                            R.string.settings_saved_message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        viewModel.getError().observe(
                this,
                message -> {

                    if (
                            message == null
                                    || message.isBlank()
                    ) {
                        return;
                    }

                    txtSettingsStatus.setText(
                            R.string.settings_status_error
                    );

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }


    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> handleBackAction()
        );

        btnSaveSettings.setOnClickListener(
                view -> saveSettings()
        );

        btnResetSettings.setOnClickListener(
                view -> showResetConfirmation()
        );

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

        updatingUi = true;

        originalMoistureLimit =
                clamp(
                        command.getMoistureLimit(),
                        10,
                        80
                );

        originalPumpDuration =
                clamp(
                        command.getPumpDuration(),
                        5,
                        60
                );

        originalCooldownSeconds =
                roundToStep(
                        clamp(
                                command.getCooldownSeconds(),
                                30,
                                600
                        ),
                        10
                );

        originalRestartDelta =
                clamp(
                        command.getRestartDelta(),
                        1,
                        30
                );

        originalSystemEnabled =
                command.isEnabled();

        sliderMoistureLimit.setValue(
                originalMoistureLimit
        );

        sliderPumpDuration.setValue(
                originalPumpDuration
        );

        sliderCooldown.setValue(
                originalCooldownSeconds
        );

        sliderRestartDelta.setValue(
                originalRestartDelta
        );

        switchSystemEnabled.setChecked(
                originalSystemEnabled
        );

        updateMoistureLimitLabel(
                originalMoistureLimit
        );

        updatePumpDurationLabel(
                originalPumpDuration
        );

        updateCooldownLabel(
                originalCooldownSeconds
        );

        updateRestartDeltaLabel(
                originalRestartDelta
        );

        updateSystemEnabledDescription(
                originalSystemEnabled
        );

        updatingUi = false;

        updateUnsavedState();
    }


    private void saveSettings() {

        if (!hasUnsavedChanges()) {

            Toast.makeText(
                    this,
                    R.string.settings_no_changes,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        viewModel.saveSettings(
                getMoistureLimit(),
                getPumpDuration(),
                getCooldownSeconds(),
                getRestartDelta(),
                switchSystemEnabled.isChecked()
        );
    }


    private void showResetConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.settings_reset_dialog_title
                )
                .setMessage(
                        R.string.settings_reset_dialog_message
                )
                .setNegativeButton(
                        R.string.settings_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.settings_reset_confirm,
                        (dialog, which) ->
                                applyDefaultValuesToUi()
                )
                .show();
    }


    /**
     * Varsayılan değerleri yalnızca ekrana uygular.
     * Kullanıcı yine "Ayarları Kaydet" butonuna basmalıdır.
     */
    private void applyDefaultValuesToUi() {

        updatingUi = true;

        sliderMoistureLimit.setValue(
                DEFAULT_MOISTURE_LIMIT
        );

        sliderPumpDuration.setValue(
                DEFAULT_PUMP_DURATION
        );

        sliderCooldown.setValue(
                DEFAULT_COOLDOWN_SECONDS
        );

        sliderRestartDelta.setValue(
                DEFAULT_RESTART_DELTA
        );

        switchSystemEnabled.setChecked(
                DEFAULT_SYSTEM_ENABLED
        );

        updateMoistureLimitLabel(
                DEFAULT_MOISTURE_LIMIT
        );

        updatePumpDurationLabel(
                DEFAULT_PUMP_DURATION
        );

        updateCooldownLabel(
                DEFAULT_COOLDOWN_SECONDS
        );

        updateRestartDeltaLabel(
                DEFAULT_RESTART_DELTA
        );

        updateSystemEnabledDescription(
                DEFAULT_SYSTEM_ENABLED
        );

        updatingUi = false;

        updateUnsavedState();

        txtSettingsStatus.setText(
                R.string.settings_defaults_applied
        );
    }


    private void handleBackAction() {

        if (!hasUnsavedChanges()) {

            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.settings_unsaved_dialog_title
                )
                .setMessage(
                        R.string.settings_unsaved_dialog_message
                )
                .setNegativeButton(
                        R.string.settings_continue_editing,
                        null
                )
                .setNeutralButton(
                        R.string.settings_discard_changes,
                        (dialog, which) -> finish()
                )
                .setPositiveButton(
                        R.string.settings_save_and_exit,
                        (dialog, which) -> {

                            viewModel.saveSettings(
                                    getMoistureLimit(),
                                    getPumpDuration(),
                                    getCooldownSeconds(),
                                    getRestartDelta(),
                                    switchSystemEnabled.isChecked()
                            );
                        }
                )
                .show();
    }


    private void updateUnsavedState() {

        boolean hasChanges =
                hasUnsavedChanges();

        cardUnsavedChanges.setVisibility(
                hasChanges
                        ? View.VISIBLE
                        : View.GONE
        );

        btnSaveSettings.setEnabled(
                hasChanges
        );

        if (hasChanges) {

            txtSettingsStatus.setText(
                    R.string.settings_status_unsaved
            );

        } else {

            txtSettingsStatus.setText(
                    R.string.settings_status_ready
            );
        }
    }


    private boolean hasUnsavedChanges() {

        return getMoistureLimit()
                != originalMoistureLimit

                || getPumpDuration()
                != originalPumpDuration

                || getCooldownSeconds()
                != originalCooldownSeconds

                || getRestartDelta()
                != originalRestartDelta

                || switchSystemEnabled.isChecked()
                != originalSystemEnabled;
    }


    private void saveCurrentValuesAsOriginal() {

        originalMoistureLimit =
                getMoistureLimit();

        originalPumpDuration =
                getPumpDuration();

        originalCooldownSeconds =
                getCooldownSeconds();

        originalRestartDelta =
                getRestartDelta();

        originalSystemEnabled =
                switchSystemEnabled.isChecked();
    }


    private void setControlsEnabled(boolean enabled) {

        sliderMoistureLimit.setEnabled(enabled);
        sliderPumpDuration.setEnabled(enabled);
        sliderCooldown.setEnabled(enabled);
        sliderRestartDelta.setEnabled(enabled);
        switchSystemEnabled.setEnabled(enabled);

        btnResetSettings.setEnabled(enabled);

        btnSaveSettings.setEnabled(
                enabled && hasUnsavedChanges()
        );
    }


    private long getMoistureLimit() {

        return Math.round(
                sliderMoistureLimit.getValue()
        );
    }


    private long getPumpDuration() {

        return Math.round(
                sliderPumpDuration.getValue()
        );
    }


    private long getCooldownSeconds() {

        return Math.round(
                sliderCooldown.getValue()
        );
    }


    private long getRestartDelta() {

        return Math.round(
                sliderRestartDelta.getValue()
        );
    }


    private void updateMoistureLimitLabel(long value) {

        txtMoistureLimitValue.setText(
                getString(
                        R.string.settings_percentage_format,
                        value
                )
        );
    }


    private void updatePumpDurationLabel(long value) {

        txtPumpDurationValue.setText(
                getString(
                        R.string.settings_seconds_format,
                        value
                )
        );
    }


    private void updateCooldownLabel(long value) {

        txtCooldownValue.setText(
                formatCooldown(value)
        );
    }


    private void updateRestartDeltaLabel(long value) {

        txtRestartDeltaValue.setText(
                getString(
                        R.string.settings_percentage_format,
                        value
                )
        );
    }


    private void updateSystemEnabledDescription(boolean enabled) {

        txtSystemEnabledDescription.setText(
                enabled
                        ? R.string.settings_system_enabled_active
                        : R.string.settings_system_enabled_inactive
        );

        txtSystemEnabledDescription.setTextColor(
                getColor(
                        enabled
                                ? R.color.online
                                : R.color.offline
                )
        );
    }


    private String formatCooldown(long seconds) {

        if (seconds < 60) {

            return getString(
                    R.string.settings_seconds_format,
                    seconds
            );
        }

        long minutes =
                seconds / 60;

        long remainingSeconds =
                seconds % 60;

        if (remainingSeconds == 0) {

            return getString(
                    R.string.settings_minutes_format,
                    minutes
            );
        }

        return getString(
                R.string.settings_minutes_seconds_format,
                minutes,
                remainingSeconds
        );
    }


    private long clamp(
            long value,
            long min,
            long max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    private long roundToStep(
            long value,
            long step
    ) {

        return Math.round(
                (double) value / step
        ) * step;
    }
}