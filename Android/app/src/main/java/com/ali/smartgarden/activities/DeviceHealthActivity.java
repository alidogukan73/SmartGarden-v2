package com.ali.smartgarden.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.viewmodels.DeviceHealthViewModel;
import com.ali.smartgarden.viewmodels.MainViewModel;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Collections;

public class DeviceHealthActivity extends AppCompatActivity {

    private DeviceHealthViewModel viewModel;

    private Handler soilHandler =
            new Handler(Looper.getMainLooper());

    private Runnable soilChecker;

    private MaterialButton btnBack;
    private MaterialButton btnRestartDevice;

    private MaterialCardView cardHealthSummary;
    private MaterialCardView cardOverallHealth;
    private MaterialCardView cardOverallHealthBadge;
    private MaterialCardView cardWifiStatus;
    private MaterialCardView cardThrottling;
    private MaterialCardView cardThrottlingBadge;

    private TextView txtHealthSummaryIcon;
    private TextView txtOverallHealth;
    private TextView txtLastHealthUpdate;
    private TextView txtOverallHealthBadge;

    private TextView txtCpuTemperature;
    private TextView txtCpuUsage;
    private ProgressBar progressCpu;

    private TextView txtRamUsage;
    private ProgressBar progressRam;
    private TextView txtRamDetail;

    private TextView txtDiskUsage;
    private ProgressBar progressDisk;
    private TextView txtDiskDetail;

    private TextView txtWifiSignal;
    private TextView txtWifiQuality;
    private TextView txtWifiStatusIcon;
    private TextView txtIpAddress;

    private TextView txtThrottlingDescription;
    private TextView txtThrottlingStatus;

    private TextView txtFirmware;
    private TextView txtUptime;

    private TextView txtSoilSensorId;
    private TextView txtSoilFirmware;
    private TextView txtSoilRssi;
    private TextView txtSoilUptime;

    private TextView txtSoilConnection;
    private TextView txtSoilLastUpdate;

    private String lastSoilUpdateTime;


    private MaterialCardView cardSoilConnection;

    private TextView txtCurrentPowerEvents;
    private TextView txtHistoricalPowerEvents;
    private TextView txtThrottlingRaw;
    private TextView txtDiagnosticsSummary;
    private LinearLayout layoutDiagnostics;
    private Health latestHealth;
    private Status latestStatus;
    private List<GardenZone> latestZones =
            Collections.emptyList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_device_health);

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeActions();
        startSoilConnectionMonitor();
    }

    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.healthRoot),
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
        txtCurrentPowerEvents =
                findViewById(R.id.txtCurrentPowerEvents);

        txtHistoricalPowerEvents =
                findViewById(R.id.txtHistoricalPowerEvents);

        txtThrottlingRaw =
                findViewById(R.id.txtThrottlingRaw);

        txtDiagnosticsSummary =
                findViewById(R.id.txtDiagnosticsSummary);

        layoutDiagnostics =
                findViewById(R.id.layoutDiagnostics);

        btnBack = findViewById(R.id.btnBack);

        btnRestartDevice =
                findViewById(
                        R.id.btnRestartDevice
                );

        cardHealthSummary =
                findViewById(R.id.cardHealthSummary);

        cardOverallHealth =
                findViewById(R.id.cardOverallHealth);

        cardOverallHealthBadge =
                findViewById(R.id.cardOverallHealthBadge);

        cardWifiStatus =
                findViewById(R.id.cardWifiStatus);

        cardThrottling =
                findViewById(R.id.cardThrottling);

        cardThrottlingBadge =
                findViewById(R.id.cardThrottlingBadge);

        txtHealthSummaryIcon =
                findViewById(R.id.txtHealthSummaryIcon);

        txtOverallHealth =
                findViewById(R.id.txtOverallHealth);

        txtLastHealthUpdate =
                findViewById(R.id.txtLastHealthUpdate);

        txtOverallHealthBadge =
                findViewById(R.id.txtOverallHealthBadge);

        txtCpuTemperature =
                findViewById(R.id.txtCpuTemperature);

        txtCpuUsage =
                findViewById(R.id.txtCpuUsage);

        progressCpu =
                findViewById(R.id.progressCpu);

        txtRamUsage =
                findViewById(R.id.txtRamUsage);

        progressRam =
                findViewById(R.id.progressRam);

        txtRamDetail =
                findViewById(R.id.txtRamDetail);

        txtDiskUsage =
                findViewById(R.id.txtDiskUsage);

        progressDisk =
                findViewById(R.id.progressDisk);

        txtDiskDetail =
                findViewById(R.id.txtDiskDetail);

        txtWifiSignal =
                findViewById(R.id.txtWifiSignal);

        txtWifiQuality =
                findViewById(R.id.txtWifiQuality);

        txtWifiStatusIcon =
                findViewById(R.id.txtWifiStatusIcon);

        txtIpAddress =
                findViewById(R.id.txtIpAddress);

        txtThrottlingDescription =
                findViewById(R.id.txtThrottlingDescription);

        txtThrottlingStatus =
                findViewById(R.id.txtThrottlingStatus);

        txtFirmware =
                findViewById(R.id.txtFirmware);

        txtUptime =
                findViewById(R.id.txtUptime);

    }

    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(DeviceHealthViewModel.class);
    }

    private void observeViewModel() {

        viewModel.getHealth().observe(
                this,
                this::renderHealth
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

        MainViewModel mainViewModel =
                new ViewModelProvider(this)
                        .get(MainViewModel.class);

        mainViewModel.getStatus().observe(
                this,
                status -> {
                    latestStatus = status;
                    renderDiagnostics();
                }
        );

        mainViewModel.getGardenZones().observe(
                this,
                zones -> {
                    latestZones = zones == null
                            ? Collections.emptyList()
                            : zones;
                    renderDiagnostics();
                }
        );
    }

    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnRestartDevice.setOnClickListener(
                view -> showRestartConfirmationDialog()
        );
    }
    private void showRestartConfirmationDialog() {

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Cihaz yeniden başlatılsın mı?"
                )
                .setMessage(
                        "Raspberry Pi yeniden başlatılacak. "
                                + "Bu sırada sulama ve sensör bağlantısı "
                                + "kısa süreliğine kesilecektir."
                )
                .setNegativeButton(
                        "İptal",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Yeniden Başlat",
                        (dialog, which) -> {

                            viewModel.restartDevice();

                            Toast.makeText(
                                    this,
                                    "Yeniden başlatma komutu gönderildi.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }
    private void renderHealth(Health health) {

        if (health == null) {
            return;
        }

        latestHealth = health;

        renderCpu(health);
        renderMemory(health);
        renderDisk(health);
        renderNetwork(health);
        renderThrottling(health);
        renderSystemInfo(health);
        renderOverallHealth(health);
        renderPowerDetails(health);
        renderDiagnostics();
    }

    private void renderDiagnostics() {
        if (
                layoutDiagnostics == null
                        || txtDiagnosticsSummary == null
        ) {
            return;
        }

        layoutDiagnostics.removeAllViews();
        int normalCount = 0;
        final int totalChecks = 5;
        long nowEpoch = System.currentTimeMillis() / 1000L;

        boolean piOnline =
                latestStatus != null
                        && latestStatus.isOnline()
                        && latestStatus.getLastSeenEpoch() > 0L
                        && nowEpoch
                        - latestStatus.getLastSeenEpoch() <= 30L;
        addDiagnosticRow(
                piOnline,
                piOnline
                        ? R.string.diagnostics_pi_ok
                        : R.string.diagnostics_pi_error
        );
        if (piOnline) normalCount++;

        boolean sensorFresh = false;
        for (GardenZone zone : latestZones) {
            if (
                    zone.getUpdated_at_epoch() > 0L
                            && nowEpoch
                            - zone.getUpdated_at_epoch() <= 90L
            ) {
                sensorFresh = true;
                break;
            }
        }
        addDiagnosticRow(
                sensorFresh,
                sensorFresh
                        ? R.string.diagnostics_sensor_ok
                        : R.string.diagnostics_sensor_error
        );
        if (sensorFresh) normalCount++;

        boolean relaySafe =
                latestStatus != null
                        && !latestStatus.isRelay();
        addDiagnosticRow(
                relaySafe,
                relaySafe
                        ? R.string.diagnostics_relay_ok
                        : R.string.diagnostics_relay_active
        );
        if (relaySafe) normalCount++;

        boolean simulationMode =
                !latestZones.isEmpty();
        for (GardenZone zone : latestZones) {
            if ("PHYSICAL".equals(zone.getValve_mode())) {
                simulationMode = false;
                break;
            }
        }
        addDiagnosticRow(
                simulationMode,
                simulationMode
                        ? R.string.diagnostics_valve_simulation
                        : R.string.diagnostics_valve_physical
        );
        if (simulationMode) normalCount++;

        String lastError = latestStatus == null
                ? ""
                : latestStatus.getLastError();
        boolean errorClear =
                lastError == null || lastError.trim().isEmpty();
        addDiagnosticRow(
                errorClear,
                errorClear
                        ? getString(
                                R.string.diagnostics_error_clear
                        )
                        : getString(
                                R.string.diagnostics_error_active,
                                lastError.trim()
                        )
        );
        if (errorClear) normalCount++;

        txtDiagnosticsSummary.setText(
                getString(
                        R.string.diagnostics_summary,
                        normalCount,
                        totalChecks
                )
        );
        txtDiagnosticsSummary.setTextColor(
                color(
                        normalCount == totalChecks
                                ? R.color.online
                                : R.color.warning
                )
        );
    }

    private void addDiagnosticRow(
            boolean healthy,
            int textResource
    ) {
        addDiagnosticRow(healthy, getString(textResource));
    }

    private void addDiagnosticRow(
            boolean healthy,
            String message
    ) {
        TextView row = new TextView(this);
        row.setText((healthy ? "✓ " : "⚠ ") + message);
        row.setTextColor(
                color(
                        healthy
                                ? R.color.online
                                : R.color.warning
                )
        );
        row.setTextSize(13f);
        row.setPadding(0, 7, 0, 7);
        layoutDiagnostics.addView(row);
    }
    private void renderPowerDetails(
            Health health
    ) {

        String currentEvents =
                buildCurrentPowerEvents(
                        health
                );

        String historicalEvents =
                buildHistoricalPowerEvents(
                        health
                );

        boolean hasCurrentEvents =
                health.isUnderVoltageNow()
                        || health.isFrequencyCappedNow()
                        || health.isThrottledNow()
                        || health.isSoftTemperatureLimitNow();

        boolean hasHistoricalEvents =
                health.isUnderVoltageHistory()
                        || health.isFrequencyCappedHistory()
                        || health.isThrottledHistory()
                        || health.isSoftTemperatureLimitHistory();

        txtCurrentPowerEvents.setText(
                currentEvents
        );

        txtCurrentPowerEvents.setTextColor(
                color(
                        hasCurrentEvents
                                ? R.color.offline
                                : R.color.online
                )
        );

        txtHistoricalPowerEvents.setText(
                historicalEvents
        );

        txtHistoricalPowerEvents.setTextColor(
                color(
                        hasHistoricalEvents
                                ? R.color.warning
                                : R.color.textSecondary
                )
        );

        txtThrottlingRaw.setText(
                getString(
                        R.string.health_throttling_raw_format,
                        health.getThrottledRaw()
                )
        );
    }

    private String buildCurrentPowerEvents(
            Health health
    ) {

        StringBuilder builder =
                new StringBuilder();

        appendPowerEvent(
                builder,
                health.isUnderVoltageNow(),
                getString(
                        R.string.health_event_under_voltage_now
                )
        );

        appendPowerEvent(
                builder,
                health.isFrequencyCappedNow(),
                getString(
                        R.string.health_event_frequency_capped_now
                )
        );

        appendPowerEvent(
                builder,
                health.isThrottledNow(),
                getString(
                        R.string.health_event_throttled_now
                )
        );

        appendPowerEvent(
                builder,
                health.isSoftTemperatureLimitNow(),
                getString(
                        R.string.health_event_temperature_limit_now
                )
        );

        if (builder.length() == 0) {

            return "✓ "
                    + getString(
                    R.string.health_no_current_events
            );
        }

        return builder.toString();
    }

    private String buildHistoricalPowerEvents(
            Health health
    ) {

        StringBuilder builder =
                new StringBuilder();

        appendPowerEvent(
                builder,
                health.isUnderVoltageHistory(),
                getString(
                        R.string.health_event_under_voltage_history
                )
        );

        appendPowerEvent(
                builder,
                health.isFrequencyCappedHistory(),
                getString(
                        R.string.health_event_frequency_capped_history
                )
        );

        appendPowerEvent(
                builder,
                health.isThrottledHistory(),
                getString(
                        R.string.health_event_throttled_history
                )
        );

        appendPowerEvent(
                builder,
                health.isSoftTemperatureLimitHistory(),
                getString(
                        R.string.health_event_temperature_limit_history
                )
        );

        if (builder.length() == 0) {

            return "✓ "
                    + getString(
                    R.string.health_no_history_events
            );
        }

        return builder.toString();
    }

    private void appendPowerEvent(
            StringBuilder builder,
            boolean condition,
            String eventText
    ) {

        if (!condition) {
            return;
        }

        if (builder.length() > 0) {

            builder.append(
                    "\n"
            );
        }

        builder.append(
                "⚠ "
        );

        builder.append(
                eventText
        );
    }
    private void renderCpu(Health health) {

        double temperature =
                Math.max(0, health.getCpuTemperature());

        double usage =
                clampPercentage(health.getCpuUsage());

        txtCpuTemperature.setText(
                getString(
                        R.string.health_temperature_format,
                        temperature
                )
        );

        txtCpuUsage.setText(
                getString(
                        R.string.health_percentage_format,
                        usage
                )
        );

        int cpuUsage =
                (int) Math.round(
                        health.getCpuUsage()
                );

        int visualCpuProgress;

        if (cpuUsage <= 0) {

            visualCpuProgress = 0;

        } else {

            visualCpuProgress =
                    Math.max(
                            cpuUsage,
                            4
                    );
        }

        progressCpu.setProgress(
                visualCpuProgress
        );

        int color =
                usage >= 85 || temperature >= 75
                        ? color(R.color.offline)
                        : usage >= 65 || temperature >= 65
                        ? color(R.color.warning)
                        : color(R.color.online);

        progressCpu.setProgressTintList(
                ColorStateList.valueOf(color)
        );

        txtCpuTemperature.setTextColor(color);
        txtCpuUsage.setTextColor(color);
    }

    private void renderMemory(Health health) {

        double memoryUsage =
                clampPercentage(health.getMemoryUsage());

        txtRamUsage.setText(
                getString(
                        R.string.health_percentage_format,
                        memoryUsage
                )
        );

        txtRamDetail.setText(
                getString(
                        R.string.health_memory_summary_format,
                        memoryUsage
                )
        );

        progressRam.setProgress(
                (int) Math.round(memoryUsage)
        );

        int color =
                resourceColorForUsage(memoryUsage);

        txtRamUsage.setTextColor(color);

        progressRam.setProgressTintList(
                ColorStateList.valueOf(color)
        );
    }

    private void renderDisk(Health health) {

        double diskUsage =
                clampPercentage(health.getDiskUsage());

        txtDiskUsage.setText(
                getString(
                        R.string.health_percentage_format,
                        diskUsage
                )
        );

        txtDiskDetail.setText(
                getString(
                        R.string.health_disk_summary_format,
                        diskUsage
                )
        );

        progressDisk.setProgress(
                (int) Math.round(diskUsage)
        );

        int color =
                resourceColorForUsage(diskUsage);

        txtDiskUsage.setTextColor(color);

        progressDisk.setProgressTintList(
                ColorStateList.valueOf(color)
        );
    }

    private void renderNetwork(Health health) {

        long wifiSignal =
                health.getWifiSignal();

        txtWifiSignal.setText(
                getString(
                        R.string.health_signal_format,
                        wifiSignal
                )
        );

        String quality;
        int statusColor;
        int backgroundColor;

        if (wifiSignal >= -55) {

            quality =
                    getString(R.string.health_wifi_excellent);

            statusColor =
                    color(R.color.online);

            backgroundColor =
                    color(R.color.onlineBackground);

        } else if (wifiSignal >= -67) {

            quality =
                    getString(R.string.health_wifi_good);

            statusColor =
                    color(R.color.primary);

            backgroundColor =
                    color(R.color.primaryLight);

        } else if (wifiSignal >= -75) {

            quality =
                    getString(R.string.health_wifi_weak);

            statusColor =
                    color(R.color.warning);

            backgroundColor =
                    color(R.color.warningBackground);

        } else {

            quality =
                    getString(R.string.health_wifi_poor);

            statusColor =
                    color(R.color.offline);

            backgroundColor =
                    color(R.color.offlineBackground);
        }

        txtWifiQuality.setText(quality);
        txtWifiQuality.setTextColor(statusColor);

        txtWifiStatusIcon.setTextColor(statusColor);

        cardWifiStatus.setCardBackgroundColor(
                backgroundColor
        );

        cardWifiStatus.setStrokeColor(
                statusColor
        );

        String ipAddress =
                health.getIpAddress();

        txtIpAddress.setText(
                ipAddress == null || ipAddress.isBlank()
                        ? getString(R.string.health_default_ip)
                        : ipAddress
        );
    }

    private void renderThrottling(Health health) {

        boolean throttled =
                health.isThrottled();

        int statusColor =
                throttled
                        ? color(R.color.offline)
                        : color(R.color.online);

        int backgroundColor =
                throttled
                        ? color(R.color.offlineBackground)
                        : color(R.color.onlineBackground);

        txtThrottlingStatus.setText(
                throttled
                        ? R.string.health_throttling_active
                        : R.string.health_throttling_clear
        );

        txtThrottlingDescription.setText(
                throttled
                        ? R.string.health_throttling_active_description
                        : R.string.health_throttling_clear_description
        );

        txtThrottlingStatus.setTextColor(
                statusColor
        );

        txtThrottlingDescription.setTextColor(
                statusColor
        );

        cardThrottlingBadge.setCardBackgroundColor(
                backgroundColor
        );

        cardThrottlingBadge.setStrokeColor(
                statusColor
        );

        cardThrottling.setStrokeColor(
                throttled
                        ? statusColor
                        : color(R.color.border)
        );
    }

    private void renderSystemInfo(Health health) {

        String firmware = health.getFirmware();

        txtFirmware.setText(
                firmware == null || firmware.isBlank()
                        ? getString(R.string.health_firmware_unavailable)
                        : firmware
        );

        txtUptime.setText(
                formatUptime(
                        health.getUptimeSeconds()
                )
        );

        txtLastHealthUpdate.setText(
                formatUpdatedAt(
                        health.getUpdatedAt()
                )
        );
    }

    private void updateSoilLastUpdate(long age) {

        if(age < 5) {

            txtSoilLastUpdate.setText(
                    "Az önce"
            );

        }
        else if(age < 60) {

            txtSoilLastUpdate.setText(
                    age + " sn önce"
            );

        }
        else {

            long minutes = age / 60;

            txtSoilLastUpdate.setText(
                    minutes + " dk önce"
            );
        }
    }

    private void updateSoilConnectionStatus(long age) {


        if(age < 20) {

            txtSoilConnection.setText(
                    "● Bağlı"
            );

            cardSoilConnection.setCardBackgroundColor(
                    getColor(R.color.online)
            );


        }
        else if(age < 60) {

            txtSoilConnection.setText(
                    "● Beklemede"
            );

            cardSoilConnection.setCardBackgroundColor(
                    getColor(R.color.warning)
            );


        }
        else {

            txtSoilConnection.setText(
                    "● Bağlantı Yok"
            );

            cardSoilConnection.setCardBackgroundColor(
                    getColor(R.color.offline)
            );
        }
    }


    private void renderOverallHealth(Health health) {

        boolean critical =
                health.isThrottled()
                        || health.getCpuTemperature() >= 75
                        || health.getCpuUsage() >= 85
                        || health.getMemoryUsage() >= 90
                        || health.getDiskUsage() >= 90
                        || health.getWifiSignal() < -80;

        boolean warning =
                health.getCpuTemperature() >= 65
                        || health.getCpuUsage() >= 65
                        || health.getMemoryUsage() >= 75
                        || health.getDiskUsage() >= 75
                        || health.getWifiSignal() < -67;

        int statusColor;
        int backgroundColor;
        int titleResource;
        int badgeResource;

        if (critical) {

            statusColor =
                    color(R.color.offline);

            backgroundColor =
                    color(R.color.offlineBackground);

            titleResource =
                    R.string.health_overall_critical;

            badgeResource =
                    R.string.health_badge_critical;

        } else if (warning) {

            statusColor =
                    color(R.color.warning);

            backgroundColor =
                    color(R.color.warningBackground);

            titleResource =
                    R.string.health_overall_warning;

            badgeResource =
                    R.string.health_badge_warning;

        } else {

            statusColor =
                    color(R.color.online);

            backgroundColor =
                    color(R.color.onlineBackground);

            titleResource =
                    R.string.health_overall_good;

            badgeResource =
                    R.string.health_badge_good;
        }

        txtOverallHealth.setText(
                titleResource
        );

        txtOverallHealth.setTextColor(
                statusColor
        );

        txtOverallHealthBadge.setText(
                badgeResource
        );

        txtOverallHealthBadge.setTextColor(
                statusColor
        );

        cardOverallHealth.setStrokeColor(
                statusColor
        );

        cardOverallHealthBadge.setCardBackgroundColor(
                backgroundColor
        );

        cardOverallHealthBadge.setStrokeColor(
                statusColor
        );

        cardHealthSummary.setCardBackgroundColor(
                backgroundColor
        );

        cardHealthSummary.setStrokeColor(
                statusColor
        );

        txtHealthSummaryIcon.setTextColor(
                statusColor
        );
    }

    private double clampPercentage(double value) {

        return Math.max(
                0,
                Math.min(
                        100,
                        value
                )
        );
    }

    private int resourceColorForUsage(double usage) {

        if (usage >= 90) {
            return color(R.color.offline);
        }

        if (usage >= 75) {
            return color(R.color.warning);
        }

        return color(R.color.online);
    }

    private String formatUptime(long totalSeconds) {

        long safeSeconds =
                Math.max(0, totalSeconds);

        long days =
                safeSeconds / 86400;

        long hours =
                (safeSeconds % 86400) / 3600;

        long minutes =
                (safeSeconds % 3600) / 60;

        if (days > 0) {

            return getString(
                    R.string.health_uptime_days_format,
                    days,
                    hours
            );
        }

        if (hours > 0) {

            return getString(
                    R.string.health_uptime_hours_format,
                    hours,
                    minutes
            );
        }

        return getString(
                R.string.health_uptime_minutes_format,
                minutes
        );
    }

    private String formatUpdatedAt(String updatedAt) {

        if (
                updatedAt == null
                        || updatedAt.isBlank()
        ) {

            return getString(
                    R.string.health_update_waiting
            );
        }

        try {

            String normalized =
                    updatedAt.length() >= 19
                            ? updatedAt.substring(0, 19)
                            : updatedAt;

            SimpleDateFormat sourceFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss",
                            Locale.US
                    );

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "dd-MM-yyyy HH:mm",
                            new Locale("tr", "TR")
                    );

            Date date =
                    sourceFormat.parse(normalized);

            if (date == null) {

                return getString(
                        R.string.health_update_waiting
                );
            }

            return getString(
                    R.string.health_last_update_format,
                    displayFormat.format(date)
            );

        } catch (ParseException exception) {

            return getString(
                    R.string.health_update_waiting
            );
        }
    }

    private int color(int colorResource) {

        return ContextCompat.getColor(
                this,
                colorResource
        );
    }

    private long getSecondsSinceUpdate(
            String updatedAt
    ) {

        if (updatedAt == null || updatedAt.isBlank()) {
            return Long.MAX_VALUE;
        }


        try {

            String normalized =
                    updatedAt.substring(0, 19);


            LocalDateTime updateTime =
                    LocalDateTime.parse(
                            normalized
                    );


            long updateSeconds =
                    updateTime.atZone(
                            ZoneId.systemDefault()
                    ).toEpochSecond();


            long nowSeconds =
                    Instant.now()
                            .getEpochSecond();


            long age =
                    nowSeconds - updateSeconds;


            if (age < 0) {

                age = 0;
            }


            return age;


        } catch (Exception e) {

            Log.e(
                    "SOIL_TIME",
                    "Timestamp parse hatası: "
                            + updatedAt,
                    e
            );

            return Long.MAX_VALUE;
        }
    }

    private void startSoilConnectionMonitor() {

        soilChecker = new Runnable() {

            @Override
            public void run() {

                if(lastSoilUpdateTime != null) {

                    long age =
                            getSecondsSinceUpdate(
                                    lastSoilUpdateTime
                            );

                    updateSoilConnectionStatus(age);
                    updateSoilLastUpdate(age);
                }


                soilHandler.postDelayed(
                        this,
                        5000
                );
            }
        };


        soilHandler.post(soilChecker);
    }

    private String getRssiQuality(int rssi) {

        if (rssi >= -55) {

            return "Mükemmel (" + rssi + " dBm)";

        } else if (rssi >= -70) {

            return "İyi (" + rssi + " dBm)";

        } else if (rssi >= -80) {

            return "Zayıf (" + rssi + " dBm)";

        } else {

            return "Çok zayıf (" + rssi + " dBm)";
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if(soilChecker != null) {

            soilHandler.removeCallbacks(
                    soilChecker
            );
        }
    }


}
