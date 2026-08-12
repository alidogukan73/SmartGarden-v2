package com.ali.smartgarden.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.config.AppInfo;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Verifies the real Firebase session and refreshes AVORA's essential device summaries. */
public class DataSyncActivity extends AppCompatActivity {
    private static final String PREFS = "data_sync_preferences";
    private static final String PREF_AUTO_SYNC = "automatic_summary_sync";
    private static final String PREF_LAST_SUCCESS = "last_success_epoch_ms";
    private static final String PREF_LAST_DEVICE_DATA = "last_device_data_epoch_ms";
    private static final String PREF_LAST_SCOPE = "last_verified_scope";
    private static final long CONNECTION_TIMEOUT_MS = 12_000L;

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference deviceRef = database.getReference("devices")
            .child(AppInfo.DEVICE_ID);
    private final DatabaseReference connectionRef = database.getReference(".info/connected");
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView headline;
    private TextView connectionValue;
    private TextView lastSuccessValue;
    private TextView lastDeviceDataValue;
    private TextView verifiedScopeValue;
    private TextView operationStatus;
    private MaterialSwitch automaticSwitch;
    private MaterialButton syncButton;
    private LinearProgressIndicator progress;
    private SharedPreferences preferences;
    private ValueEventListener connectionListener;
    private boolean firebaseConnected;
    private boolean manualSyncPending;
    private boolean remoteReadInFlight;
    private boolean suppressSwitchCallback;

    private final Runnable connectionTimeout = () -> {
        if (manualSyncPending) {
            manualSyncPending = false;
            remoteReadInFlight = false;
            setSyncing(false);
            showOperation(R.string.data_sync_offline_error, R.color.warning);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_data_sync);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyWindowInsets();
        bindViews();
        configureToolbar();
        configureAutomaticSync();
        configureActions();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);
        renderStoredState();
        observeConnection();
        if (automaticSwitch.isChecked()) {
            startManualSync();
        }
    }

    private void bindViews() {
        headline = findViewById(R.id.txtDataSyncConnectionHeadline);
        automaticSwitch = findViewById(R.id.switchDataSyncAutomatic);
        syncButton = findViewById(R.id.btnDataSyncNow);
        progress = findViewById(R.id.progressDataSync);
        operationStatus = findViewById(R.id.txtDataSyncOperationStatus);

        LinearLayout values = findViewById(R.id.layoutDataSyncValues);
        connectionValue = addValueRow(values, R.string.data_sync_connection_label, false);
        lastSuccessValue = addValueRow(values, R.string.data_sync_last_success_label, true);
        lastDeviceDataValue = addValueRow(values, R.string.data_sync_device_data_label, true);
        verifiedScopeValue = addValueRow(values, R.string.data_sync_scope_label, true);
    }

    private TextView addValueRow(LinearLayout container, int labelRes, boolean dividerAbove) {
        if (dividerAbove) {
            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            container.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
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
                .setText(R.string.data_sync_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
    }

    private void configureAutomaticSync() {
        boolean enabled = preferences.getBoolean(PREF_AUTO_SYNC, true);
        suppressSwitchCallback = true;
        automaticSwitch.setChecked(enabled);
        suppressSwitchCallback = false;
        applyPinnedSync(enabled);
        automaticSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            preferences.edit().putBoolean(PREF_AUTO_SYNC, checked).apply();
            applyPinnedSync(checked);
            Toast.makeText(this, checked
                    ? R.string.data_sync_auto_enabled
                    : R.string.data_sync_auto_disabled, Toast.LENGTH_LONG).show();
            if (checked) {
                startManualSync();
            }
        });
    }

    private void applyPinnedSync(boolean enabled) {
        deviceRef.child("status").keepSynced(enabled);
        deviceRef.child("health").keepSynced(enabled);
        deviceRef.child("zones").keepSynced(enabled);
        deviceRef.child("weather").keepSynced(enabled);
    }

    private void configureActions() {
        syncButton.setOnClickListener(view -> startManualSync());
    }

    private void observeConnection() {
        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                firebaseConnected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                renderConnection();
                if (firebaseConnected && manualSyncPending) {
                    performRemoteRead();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                firebaseConnected = false;
                renderConnection();
                if (manualSyncPending) {
                    manualSyncPending = false;
                    handler.removeCallbacks(connectionTimeout);
                    setSyncing(false);
                    showOperation(getString(R.string.data_sync_read_error,
                            safeMessage(error.getMessage())), R.color.warning);
                }
            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    private void startManualSync() {
        if (manualSyncPending) {
            return;
        }
        manualSyncPending = true;
        setSyncing(true);
        showOperation(R.string.data_sync_syncing, R.color.textSecondary);
        database.goOnline();
        handler.removeCallbacks(connectionTimeout);
        handler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT_MS);
        if (firebaseConnected) {
            performRemoteRead();
        }
    }

    private void performRemoteRead() {
        if (remoteReadInFlight) {
            return;
        }
        remoteReadInFlight = true;
        List<Task<DataSnapshot>> reads = new ArrayList<>();
        reads.add(deviceRef.child("status").get());
        reads.add(deviceRef.child("health").get());
        reads.add(deviceRef.child("zones").get());
        reads.add(deviceRef.child("weather").child("forecast").get());

        Tasks.whenAllSuccess(reads).addOnSuccessListener(results -> {
            if (!manualSyncPending) {
                return;
            }
            manualSyncPending = false;
            remoteReadInFlight = false;
            handler.removeCallbacks(connectionTimeout);
            if (!firebaseConnected || results.size() < 4) {
                setSyncing(false);
                showOperation(R.string.data_sync_offline_error, R.color.warning);
                return;
            }

            DataSnapshot status = (DataSnapshot) results.get(0);
            DataSnapshot health = (DataSnapshot) results.get(1);
            DataSnapshot zones = (DataSnapshot) results.get(2);
            DataSnapshot weather = (DataSnapshot) results.get(3);
            if (!status.exists() && !health.exists() && !zones.exists() && !weather.exists()) {
                setSyncing(false);
                showOperation(R.string.data_sync_empty_error, R.color.warning);
                return;
            }

            int zoneCount = (int) zones.getChildrenCount();
            long lastDeviceEpoch = resolveDeviceEpoch(status, health);
            int scopeText = status.exists() && health.exists() && weather.exists()
                    ? R.string.data_sync_scope_status_health_weather
                    : status.exists() && health.exists()
                    ? R.string.data_sync_scope_status_health
                    : R.string.data_sync_scope_partial;
            long now = System.currentTimeMillis();
            String scope = getString(R.string.data_sync_scope_value,
                    zoneCount, getString(scopeText));
            preferences.edit()
                    .putLong(PREF_LAST_SUCCESS, now)
                    .putLong(PREF_LAST_DEVICE_DATA, lastDeviceEpoch)
                    .putString(PREF_LAST_SCOPE, scope)
                    .apply();
            renderStoredState();
            setSyncing(false);
            showOperation(getString(R.string.data_sync_success, zoneCount), R.color.online);
        }).addOnFailureListener(error -> {
            if (!manualSyncPending) {
                return;
            }
            manualSyncPending = false;
            remoteReadInFlight = false;
            handler.removeCallbacks(connectionTimeout);
            setSyncing(false);
            showOperation(getString(R.string.data_sync_read_error,
                    safeMessage(error.getMessage())), R.color.warning);
        });
    }

    private void renderStoredState() {
        long lastSuccess = preferences.getLong(PREF_LAST_SUCCESS, 0L);
        long lastDeviceData = preferences.getLong(PREF_LAST_DEVICE_DATA, 0L);
        String scope = preferences.getString(PREF_LAST_SCOPE, "");
        lastSuccessValue.setText(lastSuccess > 0L
                ? formatDateTime(lastSuccess)
                : getString(R.string.data_sync_never));
        lastDeviceDataValue.setText(lastDeviceData > 0L
                ? formatDateTime(lastDeviceData)
                : getString(R.string.data_sync_no_device_data));
        verifiedScopeValue.setText(scope == null || scope.trim().isEmpty()
                ? getString(R.string.data_sync_scope_waiting)
                : scope);
    }

    private void renderConnection() {
        headline.setText(firebaseConnected
                ? R.string.data_sync_intro_connected
                : R.string.data_sync_intro_offline);
        headline.setTextColor(ContextCompat.getColor(this,
                firebaseConnected ? R.color.online : R.color.warning));
        connectionValue.setText(firebaseConnected
                ? R.string.data_sync_connected
                : R.string.data_sync_disconnected);
        connectionValue.setTextColor(ContextCompat.getColor(this,
                firebaseConnected ? R.color.online : R.color.warning));
    }

    private long resolveDeviceEpoch(DataSnapshot statusSnapshot, DataSnapshot healthSnapshot) {
        Status status = statusSnapshot.getValue(Status.class);
        if (status != null) {
            long epoch = status.getLastSeenEpoch();
            if (epoch > 0L) {
                return normalizeEpoch(epoch);
            }
            long parsed = parseTimestamp(status.getLastSeen());
            if (parsed > 0L) {
                return parsed;
            }
        }
        return parseTimestamp(healthSnapshot.child("updated_at").getValue(String.class));
    }

    private long normalizeEpoch(long epoch) {
        return epoch < 100_000_000_000L ? epoch * 1000L : epoch;
    }

    private long parseTimestamp(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Instant.parse(raw.trim()).toEpochMilli();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(raw.trim())
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            } catch (Exception ignoredAgain) {
                return 0L;
            }
        }
    }

    private String formatDateTime(long epochMillis) {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm", new Locale("tr", "TR"))
                .format(new Date(epochMillis));
    }

    private void setSyncing(boolean syncing) {
        syncButton.setEnabled(!syncing);
        progress.setVisibility(syncing ? View.VISIBLE : View.GONE);
    }

    private void showOperation(int messageRes, int colorRes) {
        showOperation(getString(messageRes), colorRes);
    }

    private void showOperation(String message, int colorRes) {
        operationStatus.setVisibility(View.VISIBLE);
        operationStatus.setText(message);
        operationStatus.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private String safeMessage(@Nullable String message) {
        return message == null || message.trim().isEmpty()
                ? getString(R.string.data_sync_disconnected)
                : message.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dataSyncRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(connectionTimeout);
        if (connectionListener != null) {
            connectionRef.removeEventListener(connectionListener);
        }
        super.onDestroy();
    }
}