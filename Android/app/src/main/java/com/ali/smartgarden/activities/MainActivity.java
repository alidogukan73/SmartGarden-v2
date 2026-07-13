package com.ali.smartgarden.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.Sensor;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.appcompat.widget.AppCompatImageView;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    // Header
    private MaterialCardView cardOnlineStatus;
    private TextView txtOnline;
    private TextView txtDevice;

    // Sensor
    private TextView txtMoisture;
    private TextView txtMoistureStatus;
    private TextView txtVoltage;
    private TextView txtRaw;
    private ProgressBar progressMoisture;

    // Pump
    private MaterialCardView cardPumpStatus;
    private TextView txtRelay;
    private TextView txtPumpDescription;

    // Automatic mode
    private MaterialCardView cardAutoMode;
    private MaterialSwitch switchAuto;
    private TextView txtAutoDescription;

    // Manual control
    private MaterialButton btnWater;
    private TextView txtManualHint;

    private AppCompatImageView imgPumpStatus;

    private MaterialButton btnStatistics;

    private MaterialButton btnHistory;

    private MaterialButton btnHealth;

    private MaterialButton btnSettings;

    private boolean updatingAutoSwitch = false;
    private boolean relayOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeButtons();
    }

    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
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

        imgPumpStatus = findViewById(R.id.imgPumpStatus);

        btnSettings = findViewById(
                R.id.btnSettings
        );

        btnHealth = findViewById(
                R.id.btnHealth
        );

        btnStatistics = findViewById(
                R.id.btnStatistics
        );

        btnHistory = findViewById(
                R.id.btnHistory
        );

        cardOnlineStatus = findViewById(R.id.cardOnlineStatus);
        txtOnline = findViewById(R.id.txtOnline);
        txtDevice = findViewById(R.id.txtDevice);

        txtMoisture = findViewById(R.id.txtMoisture);
        txtMoistureStatus = findViewById(R.id.txtMoistureStatus);
        txtVoltage = findViewById(R.id.txtVoltage);
        txtRaw = findViewById(R.id.txtRaw);
        progressMoisture = findViewById(R.id.progressMoisture);

        cardPumpStatus = findViewById(R.id.cardPumpStatus);
        txtRelay = findViewById(R.id.txtRelay);
        txtPumpDescription = findViewById(R.id.txtPumpDescription);

        cardAutoMode = findViewById(R.id.cardAutoMode);
        switchAuto = findViewById(R.id.switchAuto);
        txtAutoDescription = findViewById(R.id.txtAutoDescription);

        btnWater = findViewById(R.id.btnWater);
        txtManualHint = findViewById(R.id.txtManualHint);

        txtDevice.setText(
                R.string.default_device_name
        );
    }

    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);
    }

    private void observeViewModel() {

        viewModel.getSensor().observe(
                this,
                this::renderSensor
        );

        viewModel.getStatus().observe(
                this,
                this::renderStatus
        );

        viewModel.getCommand().observe(
                this,
                this::renderCommand
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

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void renderSensor(Sensor sensor) {

        if (sensor == null) {
            return;
        }

        long moisture = Math.max(
                0,
                Math.min(
                        100,
                        sensor.getMoisture()
                )
        );

        txtMoisture.setText(
                getString(
                        R.string.moisture_format,
                        moisture
                )
        );

        progressMoisture.setProgress(
                (int) moisture
        );

        txtVoltage.setText(
                getString(
                        R.string.voltage_format,
                        sensor.getVoltage()
                )
        );

        txtRaw.setText(
                String.valueOf(
                        sensor.getRaw()
                )
        );

        updateMoistureUi(moisture);
    }

    private void renderStatus(Status status) {

        if (status == null) {
            return;
        }

        updateOnlineUi(
                status.isOnline()
        );

        relayOn = status.isRelay();

        updatePumpUi(
                relayOn
        );
    }

    private void renderCommand(Command command) {

        if (command == null) {
            return;
        }

        boolean autoMode =
                command.isAutoMode();

        if (
                switchAuto.isChecked()
                        != autoMode
        ) {

            updatingAutoSwitch = true;

            switchAuto.setChecked(
                    autoMode
            );

            updatingAutoSwitch = false;
        }

        updateAutoModeUi(
                autoMode
        );
    }

    private void updateOnlineUi(boolean online) {

        txtOnline.setText(
                online
                        ? R.string.status_online
                        : R.string.status_offline
        );

        int textColor =
                online
                        ? color(R.color.online)
                        : color(R.color.offline);

        int backgroundColor =
                online
                        ? color(R.color.onlineBackground)
                        : color(R.color.offlineBackground);

        txtOnline.setTextColor(
                textColor
        );

        cardOnlineStatus.setCardBackgroundColor(
                backgroundColor
        );

        cardOnlineStatus.setStrokeColor(
                textColor
        );
    }

    private void updateMoistureUi(long moisture) {

        int statusColor;
        int statusTextResource;

        if (moisture < 30) {

            statusColor =
                    color(R.color.moistureLow);

            statusTextResource =
                    R.string.moisture_very_low;

        } else if (moisture < 50) {

            statusColor =
                    color(R.color.moistureWarning);

            statusTextResource =
                    R.string.moisture_drying;

        } else if (moisture <= 70) {

            statusColor =
                    color(R.color.moistureIdeal);

            statusTextResource =
                    R.string.moisture_ideal;

        } else {

            statusColor =
                    color(R.color.moistureHigh);

            statusTextResource =
                    R.string.moisture_high;
        }

        txtMoisture.setTextColor(
                statusColor
        );

        txtMoistureStatus.setText(
                statusTextResource
        );

        txtMoistureStatus.setTextColor(
                statusColor
        );

        progressMoisture.setProgressTintList(
                ColorStateList.valueOf(
                        statusColor
                )
        );
    }

    private void updatePumpUi(boolean running) {

        if (running) {
            imgPumpStatus.setImageTintList(
                    ColorStateList.valueOf(
                            color(R.color.pumpRunning)
                    )
            );

            txtRelay.setText(
                    R.string.pump_running
            );

            txtRelay.setTextColor(
                    color(R.color.pumpRunning)
            );

            txtPumpDescription.setText(
                    R.string.pump_description_running
            );

            txtPumpDescription.setTextColor(
                    color(R.color.pumpRunning)
            );

            cardPumpStatus.setCardBackgroundColor(
                    color(R.color.pumpRunningBackground)
            );

            cardPumpStatus.setStrokeColor(
                    color(R.color.pumpRunning)
            );

            btnWater.setText(
                    R.string.button_stop_watering
            );

            btnWater.setBackgroundTintList(
                    ColorStateList.valueOf(
                            color(R.color.buttonStop)
                    )
            );

            txtManualHint.setText(
                    R.string.manual_hint_running
            );

            return;
        }

        imgPumpStatus.setImageTintList(
                ColorStateList.valueOf(
                        color(R.color.pumpStopped)
                )
        );

        txtRelay.setText(
                R.string.pump_stopped
        );

        txtRelay.setTextColor(
                color(R.color.pumpStopped)
        );

        txtPumpDescription.setText(
                R.string.pump_description_idle
        );

        txtPumpDescription.setTextColor(
                color(R.color.textSecondary)
        );

        cardPumpStatus.setCardBackgroundColor(
                color(R.color.surfaceSoft)
        );

        cardPumpStatus.setStrokeColor(
                color(R.color.border)
        );

        btnWater.setText(
                R.string.button_start_manual_watering
        );

        btnWater.setBackgroundTintList(
                ColorStateList.valueOf(
                        color(R.color.buttonStart)
                )
        );

        txtManualHint.setText(
                R.string.manual_hint_idle
        );
    }

    private void updateAutoModeUi(boolean enabled) {

        if (enabled) {

            cardAutoMode.setCardBackgroundColor(
                    color(R.color.onlineBackground)
            );

            cardAutoMode.setStrokeColor(
                    color(R.color.online)
            );

            txtAutoDescription.setText(
                    R.string.auto_mode_active_description
            );

            txtAutoDescription.setTextColor(
                    color(R.color.online)
            );

            return;
        }

        cardAutoMode.setCardBackgroundColor(
                color(R.color.surfaceSoft)
        );

        cardAutoMode.setStrokeColor(
                color(R.color.border)
        );

        txtAutoDescription.setText(
                R.string.auto_mode_inactive_description
        );

        txtAutoDescription.setTextColor(
                color(R.color.textSecondary)
        );
    }

    private void initializeButtons() {

        btnSettings.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    SettingsActivity.class
                            );

                    startActivity(intent);
                }
        );

        btnHealth.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    DeviceHealthActivity.class
                            );

                    startActivity(intent);
                }
        );

        btnHistory.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    WateringHistoryActivity.class
                            );

                    startActivity(intent);
                }
        );

        btnWater.setOnClickListener(
                view -> {

                    if (relayOn) {

                        viewModel.setRelay(
                                false
                        );

                        return;
                    }

                    viewModel.setAutoMode(
                            false
                    );

                    viewModel.setRelay(
                            true
                    );
                }
        );

        switchAuto.setOnCheckedChangeListener(
                (
                        buttonView,
                        checked
                ) -> {

                    if (updatingAutoSwitch) {
                        return;
                    }

                    updateAutoModeUi(
                            checked
                    );

                    viewModel.setAutoMode(
                            checked
                    );

                    if (checked) {

                        viewModel.setRelay(
                                false
                        );
                    }
                }
        );
        btnStatistics.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    StatisticsActivity.class
                            );

                    startActivity(intent);
                }
        );
    }

    private int color(int colorResource) {

        return ContextCompat.getColor(
                this,
                colorResource
        );
    }
}