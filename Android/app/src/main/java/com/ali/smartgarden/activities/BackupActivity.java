package com.ali.smartgarden.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.backup.AvoraBackupManager;
import com.ali.smartgarden.config.AppInfo;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Creates and safely restores portable AVORA backup files. */
public class BackupActivity extends AppCompatActivity {
    private static final String PREFS = "backup_preferences";
    private static final String LAST_BACKUP_TIME = "last_backup_epoch_ms";
    private static final String LAST_BACKUP_NAME = "last_backup_file_name";
    private static final String LAST_RESTORE_TIME = "last_restore_epoch_ms";
    private static final String LAST_RESTORE_NAME = "last_restore_file_name";

    private final ExecutorService fileExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> createDocumentLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private AvoraBackupManager backupManager;
    private SharedPreferences preferences;
    private MaterialButton createButton;
    private MaterialButton restoreButton;
    private LinearProgressIndicator progress;
    private TextView operationStatus;
    private TextView lastBackupValue;
    private TextView lastRestoreValue;
    private JSONObject pendingBackup;
    private String pendingBackupName;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_backup);
        backupManager = new AvoraBackupManager(this);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        registerFileLaunchers();
        applyWindowInsets();
        bindViews();
        configureToolbar();
        configureActions();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);
        renderStoredState();
    }

    private void registerFileLaunchers() {
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                this::writeBackupFile);
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::readBackupFile);
    }

    private void bindViews() {
        createButton = findViewById(R.id.btnCreateBackup);
        restoreButton = findViewById(R.id.btnRestoreBackup);
        progress = findViewById(R.id.progressBackup);
        operationStatus = findViewById(R.id.txtBackupOperationStatus);
        LinearLayout values = findViewById(R.id.layoutBackupValues);
        lastBackupValue = addValueRow(values, R.string.backup_last_created_label, false);
        lastRestoreValue = addValueRow(values, R.string.backup_last_restored_label, true);
        addValueRow(values, R.string.backup_device_label, true).setText(AppInfo.DEVICE_ID);
        addValueRow(values, R.string.backup_format_label, true)
                .setText(R.string.backup_format_value);
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
        ((TextView) findViewById(R.id.txtSettingsToolbarTitle)).setText(R.string.backup_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
    }

    private void configureActions() {
        createButton.setOnClickListener(view -> prepareBackup());
        restoreButton.setOnClickListener(view -> openDocumentLauncher.launch(
                new String[]{"application/json", "text/plain", "application/octet-stream"}));
    }

    private void prepareBackup() {
        setBusy(true);
        showOperation(R.string.backup_preparing, R.color.textSecondary);
        backupManager.createBackup()
                .addOnSuccessListener(backup -> {
                    pendingBackup = backup;
                    pendingBackupName = buildBackupFileName();
                    setBusy(false);
                    createDocumentLauncher.launch(pendingBackupName);
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showError(getString(R.string.backup_create_error, safeMessage(error)));
                });
    }

    private void writeBackupFile(@Nullable Uri uri) {
        if (uri == null || pendingBackup == null) {
            pendingBackup = null;
            pendingBackupName = null;
            showOperation(R.string.backup_file_cancelled, R.color.textSecondary);
            return;
        }
        JSONObject backup = pendingBackup;
        String fallbackName = pendingBackupName;
        setBusy(true);
        showOperation(R.string.backup_writing, R.color.textSecondary);
        fileExecutor.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) {
                    throw new IllegalStateException(getString(R.string.backup_file_open_error));
                }
                output.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));
                output.flush();
                String name = displayName(uri, fallbackName);
                runOnUiThread(() -> onBackupWritten(name));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showError(getString(R.string.backup_write_error, safeMessage(error)));
                });
            }
        });
    }

    private void onBackupWritten(String displayName) {
        long now = System.currentTimeMillis();
        preferences.edit().putLong(LAST_BACKUP_TIME, now)
                .putString(LAST_BACKUP_NAME, displayName).apply();
        pendingBackup = null;
        pendingBackupName = null;
        setBusy(false);
        renderStoredState();
        showOperation(getString(R.string.backup_create_success, displayName), R.color.online);
        Toast.makeText(this, R.string.backup_create_success_short, Toast.LENGTH_LONG).show();
    }

    private void readBackupFile(@Nullable Uri uri) {
        if (uri == null) {
            showOperation(R.string.backup_file_cancelled, R.color.textSecondary);
            return;
        }
        setBusy(true);
        showOperation(R.string.backup_reading, R.color.textSecondary);
        fileExecutor.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IllegalStateException(getString(R.string.backup_file_open_error));
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
                JSONObject backup = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
                String name = displayName(uri, getString(R.string.backup_unknown_file));
                runOnUiThread(() -> validateAndConfirm(backup, name));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showError(getString(R.string.backup_read_error, safeMessage(error)));
                });
            }
        });
    }

    private void validateAndConfirm(JSONObject backup, String displayName) {
        setBusy(false);
        AvoraBackupManager.ValidationResult result = backupManager.validate(backup);
        if (!result.valid) {
            showError(result.message);
            return;
        }
        String date = result.createdAtEpochMs > 0L
                ? formatDateTime(result.createdAtEpochMs)
                : getString(R.string.backup_unknown_date);
        String version = result.appVersion.isBlank()
                ? getString(R.string.backup_unknown_version) : result.appVersion;
        String message = getString(R.string.backup_restore_confirmation,
                displayName, date, version, result.zoneCount, result.recordCount);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_restore_dialog_title)
                .setMessage(message)
                .setNegativeButton(R.string.backup_cancel, null)
                .setPositiveButton(R.string.backup_restore_confirm,
                        (dialog, which) -> restoreBackup(backup, displayName))
                .show();
    }

    private void restoreBackup(JSONObject backup, String displayName) {
        setBusy(true);
        showOperation(R.string.backup_restoring, R.color.textSecondary);
        backupManager.restoreBackup(backup)
                .addOnSuccessListener(unused -> {
                    long now = System.currentTimeMillis();
                    preferences.edit().putLong(LAST_RESTORE_TIME, now)
                            .putString(LAST_RESTORE_NAME, displayName).apply();
                    setBusy(false);
                    renderStoredState();
                    showOperation(R.string.backup_restore_success, R.color.online);
                    Toast.makeText(this, R.string.backup_restore_success_short,
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showError(getString(R.string.backup_restore_error, safeMessage(error)));
                });
    }

    private void renderStoredState() {
        lastBackupValue.setText(formatStoredOperation(LAST_BACKUP_TIME, LAST_BACKUP_NAME));
        lastRestoreValue.setText(formatStoredOperation(LAST_RESTORE_TIME, LAST_RESTORE_NAME));
    }

    private String formatStoredOperation(String timeKey, String nameKey) {
        long epoch = preferences.getLong(timeKey, 0L);
        String name = preferences.getString(nameKey, "");
        if (epoch <= 0L) {
            return getString(R.string.backup_never);
        }
        return name == null || name.isBlank() ? formatDateTime(epoch)
                : getString(R.string.backup_operation_value, formatDateTime(epoch), name);
    }

    private void setBusy(boolean busy) {
        createButton.setEnabled(!busy);
        restoreButton.setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        showOperation(message, R.color.error);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showOperation(int messageRes, int colorRes) {
        showOperation(getString(messageRes), colorRes);
    }

    private void showOperation(String message, int colorRes) {
        operationStatus.setVisibility(View.VISIBLE);
        operationStatus.setText(message);
        operationStatus.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private String buildBackupFileName() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
                .format(new Date());
        return "AVORA-" + AppInfo.DEVICE_ID + "-" + timestamp + ".avora.json";
    }

    private String displayName(Uri uri, String fallback) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            // Some content providers do not expose a display name.
        }
        return fallback;
    }

    private String formatDateTime(long epochMillis) {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm", new Locale("tr", "TR"))
                .format(new Date(epochMillis));
    }

    private String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank()
                ? getString(R.string.backup_unknown_error) : message.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.backupRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

    @Override
    protected void onDestroy() {
        fileExecutor.shutdownNow();
        super.onDestroy();
    }
}
