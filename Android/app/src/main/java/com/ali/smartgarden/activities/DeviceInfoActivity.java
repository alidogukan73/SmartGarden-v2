package com.ali.smartgarden.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.config.AppInfo;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.ali.smartgarden.viewmodels.DeviceInfoViewModel;
import com.ali.smartgarden.viewmodels.DeviceInfoViewModel.DeviceInfoState;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Read-only device identity, connection and installed hardware overview. */
public class DeviceInfoActivity extends AppCompatActivity {
    private TextView connectionStatus;
    private TextView connectionSummary;
    private TextView backendVersion;
    private TextView espFirmware;
    private TextView ipAddress;
    private TextView lastConnection;
    private TextView uptime;
    private TextView zoneCount;
    private TextView sensorCount;
    private TextView physicalValveCount;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_info);
        applyWindowInsets();
        bindViews();
        configureToolbar();
        configureLinks();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);
        observeDevice();
    }

    private void bindViews() {
        connectionStatus = findViewById(R.id.txtDeviceInfoConnectionStatus);
        connectionSummary = findViewById(R.id.txtDeviceInfoConnectionSummary);

        LinearLayout identity = findViewById(R.id.layoutDeviceInfoIdentity);
        TextView deviceId = addValueRow(identity, R.string.device_info_device_id, false);
        backendVersion = addValueRow(identity, R.string.device_info_backend_version, true);
        espFirmware = addValueRow(identity, R.string.device_info_esp_firmware, true);

        LinearLayout connection = findViewById(R.id.layoutDeviceInfoConnection);
        ipAddress = addValueRow(connection, R.string.device_info_ip_address, false);
        lastConnection = addValueRow(connection, R.string.device_info_last_connection, true);
        uptime = addValueRow(connection, R.string.device_info_uptime, true);

        LinearLayout hardware = findViewById(R.id.layoutDeviceInfoHardware);
        zoneCount = addValueRow(hardware, R.string.device_info_zone_count, false);
        sensorCount = addValueRow(hardware, R.string.device_info_sensor_count, true);
        physicalValveCount = addValueRow(hardware, R.string.device_info_valve_count, true);

        deviceId.setText(AppInfo.DEVICE_ID);
        setWaitingValues();
    }

    private void setWaitingValues() {
        backendVersion.setText(R.string.device_info_waiting);
        espFirmware.setText(R.string.device_info_waiting);
        ipAddress.setText(R.string.device_info_waiting);
        lastConnection.setText(R.string.device_info_waiting);
        uptime.setText(R.string.device_info_waiting);
        zoneCount.setText(R.string.device_info_waiting);
        sensorCount.setText(R.string.device_info_waiting);
        physicalValveCount.setText(R.string.device_info_waiting);
    }

    private TextView addValueRow(LinearLayout container, int labelRes, boolean dividerAbove) {
        if (dividerAbove) {
            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            container.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dividerHeightPx()));
        }
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_device_info_value, container, false);
        ((TextView) row.findViewById(R.id.txtDeviceInfoRowLabel)).setText(labelRes);
        TextView value = row.findViewById(R.id.txtDeviceInfoRowValue);
        container.addView(row);
        return value;
    }

    private void configureToolbar() {
        ((TextView) findViewById(R.id.txtSettingsToolbarTitle))
                .setText(R.string.device_info_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
    }

    private void configureLinks() {
        MaterialCardView healthLink = findViewById(R.id.deviceInfoHealthLink);
        MaterialCardView sensorsLink = findViewById(R.id.deviceInfoSensorsLink);
        configureLink(healthLink, R.drawable.ic_device_health_24,
                R.string.device_info_open_health,
                R.string.device_info_open_health_subtitle,
                () -> startActivity(new Intent(this, DeviceHealthActivity.class)));
        configureLink(sensorsLink, R.drawable.ic_ai_sensor_24,
                R.string.device_info_open_sensors,
                R.string.device_info_open_sensors_subtitle,
                () -> startActivity(new Intent(this, SensorPointsActivity.class)));
    }

    private void configureLink(MaterialCardView card, int iconRes, int titleRes,
                               int subtitleRes, Runnable action) {
        ((AppCompatImageView) card.findViewById(R.id.imgSettingsRowIcon))
                .setImageResource(iconRes);
        ((TextView) card.findViewById(R.id.txtSettingsRowTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.txtSettingsRowSubtitle)).setText(subtitleRes);
        card.setOnClickListener(view -> action.run());
    }

    private void observeDevice() {
        DeviceInfoViewModel viewModel =
                new ViewModelProvider(this).get(DeviceInfoViewModel.class);
        viewModel.getReadError().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            connectionStatus.setText(R.string.device_info_status_offline);
            connectionStatus.setTextColor(ContextCompat.getColor(
                    DeviceInfoActivity.this, R.color.offline));
            connectionSummary.setText(R.string.device_info_read_error);
        });
        viewModel.getState().observe(this, this::render);
    }

    private void render(DeviceInfoState value) {
        if (value == null) return;
        Status status = value.status;
        Health health = value.health;
        renderConnection(value.connected);

        backendVersion.setText(firstMeaningful(
                health == null ? null : health.getFirmware(),
                status == null ? null : status.getVersion(),
                getString(R.string.device_info_waiting)));
        ipAddress.setText(firstMeaningful(
                health == null ? null : health.getIpAddress(),
                getString(R.string.device_info_unavailable)));
        lastConnection.setText(value.lastSeenMillis > 0L
                ? formatDateTime(value.lastSeenMillis)
                : getString(R.string.device_info_waiting));
        uptime.setText(health != null && health.getUptimeSeconds() > 0L
                ? formatDuration(health.getUptimeSeconds())
                : getString(R.string.device_info_waiting));
        zoneCount.setText(getString(R.string.device_info_zone_count_value, value.enabledZones));
        sensorCount.setText(getString(
                R.string.device_info_sensor_count_value, value.enabledSensors));
        physicalValveCount.setText(getString(
                R.string.device_info_valve_count_value, value.physicalValves));
        espFirmware.setText(value.firmwareVersions.isEmpty()
                ? getString(R.string.device_info_sensor_firmware_waiting)
                : String.join(" / ", value.firmwareVersions));
    }

    private void renderConnection(boolean connected) {
        connectionStatus.setText(connected
                ? R.string.device_info_status_online
                : R.string.device_info_status_offline);
        connectionSummary.setText(connected
                ? R.string.device_info_summary_online
                : R.string.device_info_summary_offline);
        connectionStatus.setTextColor(ContextCompat.getColor(this,
                connected ? R.color.online : R.color.warning));
    }

    private String formatDateTime(long epochMillis) {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
                .format(new Date(epochMillis));
    }

    private String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        if (days > 0L) {
            return getString(R.string.device_info_duration_days, days, hours);
        }
        if (hours > 0L) {
            return getString(R.string.device_info_duration_hours, hours, minutes);
        }
        return getString(R.string.device_info_duration_minutes, Math.max(1L, minutes));
    }

    private String firstMeaningful(String first, String fallback) {
        return isMeaningful(first) ? first.trim() : fallback;
    }

    private String firstMeaningful(String first, String second, String fallback) {
        if (isMeaningful(first)) {
            return first.trim();
        }
        return isMeaningful(second) ? second.trim() : fallback;
    }

    private boolean isMeaningful(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int dividerHeightPx() {
        return Math.round(getResources().getDisplayMetrics().density);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deviceInfoRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

}
