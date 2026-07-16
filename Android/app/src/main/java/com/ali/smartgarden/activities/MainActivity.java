package com.ali.smartgarden.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
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
import com.ali.smartgarden.ui.MainMenuBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.ali.smartgarden.models.AdaptiveRecommendation;

import android.os.Handler;
import android.os.Looper;

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

    private MaterialButton btnMainMenu;
    private TextView txtAdaptiveBadge;
    private TextView txtAdaptiveRecommendation;
    private TextView txtAdaptiveConfidence;
    private TextView txtAdaptivePump;
    private TextView txtAdaptiveRecords;
    private TextView txtAdaptiveUpdated;
    private MaterialCardView cardAdaptiveBadge;

    private boolean updatingAutoSwitch = false;
    private boolean relayOn = false;

    private static final long ONLINE_TIMEOUT_SECONDS = 35;
    private static final long ONLINE_CHECK_INTERVAL_MILLIS = 5_000;
    private static final int MIN_RECORDS_FOR_CONFIDENCE = 5;

    private final Handler onlineStatusHandler =
            new Handler(Looper.getMainLooper());

    private Status latestStatus;

    private final Runnable onlineStatusChecker =
            new Runnable() {

                @Override
                public void run() {

                    renderEffectiveOnlineStatus();

                    onlineStatusHandler.postDelayed(
                            this,
                            ONLINE_CHECK_INTERVAL_MILLIS
                    );
                }
            };

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

        btnMainMenu = findViewById(
                R.id.btnMainMenu
        );

        cardAdaptiveBadge =
                findViewById(
                        R.id.cardAdaptiveBadge
                );

        txtAdaptiveBadge =
                findViewById(
                        R.id.txtAdaptiveBadge
                );

        txtAdaptiveRecommendation =
                findViewById(
                        R.id.txtAdaptiveRecommendation
                );

        txtAdaptiveConfidence =
                findViewById(
                        R.id.txtAdaptiveConfidence
                );

        txtAdaptivePump =
                findViewById(
                        R.id.txtAdaptivePump
                );

        txtAdaptiveRecords =
                findViewById(
                        R.id.txtAdaptiveRecords
                );

        txtAdaptiveUpdated =
                findViewById(
                        R.id.txtAdaptiveUpdated
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

        viewModel.getAdaptiveRecommendation().observe(
                this,
                this::renderAdaptiveRecommendation
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

    private void renderAdaptiveRecommendation(
            AdaptiveRecommendation recommendation
    ) {

        if (recommendation == null) {
            return;
        }

        String recommendationText =
                formatAdaptiveRecommendation(
                        recommendation.getRecommendationType()
                );

        String confidenceText =
                formatAdaptiveConfidence(
                        recommendation.getConfidenceLevel()
                );

        String pumpText =
                formatAdaptivePumpDuration(
                        recommendation.getCurrentPumpDurationSeconds(),
                        recommendation.getRecommendedPumpDurationSeconds()
                );

        String recordsText =
                getString(
                        R.string.adaptive_records_format,
                        recommendation.getWateringCountAnalyzed()
                );

        String updatedText =
                formatAdaptiveUpdatedAt(
                        recommendation.getUpdatedAt()
                );

        txtAdaptiveRecommendation.setText(
                recommendationText
        );

        long confidence =
                Math.round(
                        recommendation.getConfidence() * 100
                );

        boolean learning =
                recommendation.getWateringCountAnalyzed()
                        < MIN_RECORDS_FOR_CONFIDENCE;

        if (learning) {

            txtAdaptiveConfidence.setText(
                    "ÖĞRENİYOR"
            );

            txtAdaptiveBadge.setText(
                    "ÖĞRENİYOR"
            );

        } else {

            txtAdaptiveConfidence.setText(
                    confidenceText
                            + " • %"
                            + confidence
            );

            txtAdaptiveBadge.setText(
                    confidenceText
            );
        }

        applyAdaptiveConfidenceStyle(
                learning,
                recommendation.getConfidenceLevel()
        );

        txtAdaptivePump.setText(
                pumpText
        );

        txtAdaptiveRecords.setText(
                recordsText
        );

        txtAdaptiveUpdated.setText(
                getString(
                        R.string.adaptive_updated_format,
                        updatedText
                )
        );

        if (recommendation.getWateringCountAnalyzed()
                < MIN_RECORDS_FOR_CONFIDENCE) {

            txtAdaptiveBadge.setText(
                    "ÖĞRENİYOR"
            );

        } else {

            txtAdaptiveBadge.setText(
                    confidenceText
            );

        }
    }

    private void applyAdaptiveConfidenceStyle(
            boolean learning,
            String confidenceLevel
    ) {

        int textColorRes;
        int backgroundColorRes;
        int strokeColorRes;

        if (learning) {

            textColorRes =
                    R.color.primary;

            backgroundColorRes =
                    R.color.surfaceGreen;

            strokeColorRes =
                    R.color.primary;

        } else if ("HIGH".equals(confidenceLevel)) {

            textColorRes =
                    R.color.online;

            backgroundColorRes =
                    R.color.onlineBackground;

            strokeColorRes =
                    R.color.online;

        } else if ("MEDIUM".equals(confidenceLevel)) {

            textColorRes =
                    R.color.warning;

            backgroundColorRes =
                    R.color.warningBackground;

            strokeColorRes =
                    R.color.warning;

        } else {

            textColorRes =
                    R.color.accentOrange;

            backgroundColorRes =
                    R.color.warningBackground;

            strokeColorRes =
                    R.color.accentOrange;
        }

        int textColor =
                ContextCompat.getColor(
                        this,
                        textColorRes
                );

        int backgroundColor =
                ContextCompat.getColor(
                        this,
                        backgroundColorRes
                );

        int strokeColor =
                ContextCompat.getColor(
                        this,
                        strokeColorRes
                );

        txtAdaptiveBadge.setTextColor(
                textColor
        );

        txtAdaptiveConfidence.setTextColor(
                textColor
        );

        cardAdaptiveBadge.setCardBackgroundColor(
                backgroundColor
        );

        cardAdaptiveBadge.setStrokeColor(
                strokeColor
        );

        cardAdaptiveBadge.setStrokeWidth(
                dpToPx(
                        1
                )
        );
    }

    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
    private String formatAdaptiveRecommendation(
            String recommendationType
    ) {

        if (recommendationType == null) {
            return getString(
                    R.string.adaptive_recommendation_waiting
            );
        }

        switch (recommendationType) {

            case "INSUFFICIENT_DATA":

                return getString(
                        R.string.adaptive_recommendation_learning
                );

            case "KEEP_CURRENT_SETTINGS":

                return getString(
                        R.string.adaptive_recommendation_keep
                );

            case "INCREASE_PUMP_DURATION":

                return getString(
                        R.string.adaptive_recommendation_increase
                );

            case "DECREASE_PUMP_DURATION":

                return getString(
                        R.string.adaptive_recommendation_decrease
                );

            case "INCREASE_COOLDOWN":

                return getString(
                        R.string.adaptive_recommendation_increase_cooldown
                );

            case "DECREASE_COOLDOWN":

                return getString(
                        R.string.adaptive_recommendation_decrease_cooldown
                );

            default:

                return getString(
                        R.string.adaptive_recommendation_waiting
                );
        }
    }

    private String formatAdaptiveConfidence(
            String confidenceLevel
    ) {

        if (confidenceLevel == null) {
            return getString(
                    R.string.adaptive_confidence_low
            );
        }

        switch (confidenceLevel) {

            case "HIGH":

                return getString(
                        R.string.adaptive_confidence_high
                );

            case "MEDIUM":

                return getString(
                        R.string.adaptive_confidence_medium
                );

            case "LOW":

            default:

                return getString(
                        R.string.adaptive_confidence_low
                );
        }
    }

    private String formatAdaptivePumpDuration(
            long currentSeconds,
            long recommendedSeconds
    ) {

        String current =
                formatDurationForAdaptive(
                        currentSeconds
                );

        String recommended =
                formatDurationForAdaptive(
                        recommendedSeconds
                );

        if (currentSeconds == recommendedSeconds) {

            return getString(
                    R.string.adaptive_pump_same_format,
                    current
            );
        }

        return getString(
                R.string.adaptive_pump_change_format,
                current,
                recommended
        );
    }

    private String formatDurationForAdaptive(
            long seconds
    ) {

        long safeSeconds =
                Math.max(
                        0,
                        seconds
                );

        long hours =
                safeSeconds / 3600;

        long minutes =
                (safeSeconds % 3600) / 60;

        long remainingSeconds =
                safeSeconds % 60;

        if (hours > 0 && minutes > 0) {

            return getString(
                    R.string.adaptive_duration_hours_minutes,
                    hours,
                    minutes
            );
        }

        if (hours > 0) {

            return getString(
                    R.string.adaptive_duration_hours,
                    hours
            );
        }

        if (minutes > 0 && remainingSeconds > 0) {

            return getString(
                    R.string.adaptive_duration_minutes_seconds,
                    minutes,
                    remainingSeconds
            );
        }

        if (minutes > 0) {

            return getString(
                    R.string.adaptive_duration_minutes,
                    minutes
            );
        }

        return getString(
                R.string.adaptive_duration_seconds,
                safeSeconds
        );
    }

    private String formatAdaptiveUpdatedAt(
            String updatedAt
    ) {

        if (
                updatedAt == null
                        || updatedAt.trim().isEmpty()
        ) {

            return getString(
                    R.string.adaptive_updated_default
            );
        }

        try {

            int separatorIndex =
                    updatedAt.indexOf(
                            "T"
                    );

            if (
                    separatorIndex < 0
                            || updatedAt.length()
                            < separatorIndex + 6
            ) {

                return getString(
                        R.string.adaptive_updated_default
                );
            }

            return updatedAt.substring(
                    separatorIndex + 1,
                    separatorIndex + 6
            );

        } catch (RuntimeException exception) {

            return getString(
                    R.string.adaptive_updated_default
            );
        }
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

        latestStatus = status;

        renderEffectiveOnlineStatus();

        relayOn = status.isRelay();

        updatePumpUi(
                relayOn
        );
    }

    private void renderEffectiveOnlineStatus() {

        if (latestStatus == null) {

            updateOnlineUi(false);
            return;
        }

        boolean effectivelyOnline =
                latestStatus.isOnline()
                        && isHeartbeatFresh(
                        latestStatus.getLastSeenEpoch()
                );

        updateOnlineUi(
                effectivelyOnline
        );
    }

    private boolean isHeartbeatFresh(
            long lastSeenEpochSeconds
    ) {

        if (lastSeenEpochSeconds <= 0) {
            return false;
        }

        long currentEpochSeconds =
                System.currentTimeMillis() / 1000L;

        long heartbeatAgeSeconds =
                currentEpochSeconds
                        - lastSeenEpochSeconds;

        /*
         * Telefon saati geçici olarak Pi saatinin gerisindeyse
         * küçük negatif farkı çevrim dışı kabul etmiyoruz.
         */
        if (heartbeatAgeSeconds < 0) {
            return true;
        }

        return heartbeatAgeSeconds
                <= ONLINE_TIMEOUT_SECONDS;
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

        btnMainMenu.setOnClickListener(
                view -> showMainMenu()
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
    }
    private void showMainMenu() {

        String tag =
                "MainMenuBottomSheet";

        if (
                getSupportFragmentManager()
                        .findFragmentByTag(tag)
                        != null
        ) {
            return;
        }

        MainMenuBottomSheet bottomSheet =
                new MainMenuBottomSheet();

        bottomSheet.show(
                getSupportFragmentManager(),
                tag
        );
    }
    @Override
    protected void onStart() {

        super.onStart();

        onlineStatusHandler.removeCallbacks(
                onlineStatusChecker
        );

        onlineStatusHandler.post(
                onlineStatusChecker
        );
    }


    @Override
    protected void onStop() {

        onlineStatusHandler.removeCallbacks(
                onlineStatusChecker
        );

        super.onStop();
    }
    private int color(int colorResource) {

        return ContextCompat.getColor(
                this,
                colorResource
        );
    }
}