package com.alidogukan.avora.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.alidogukan.avora.R;
import com.alidogukan.avora.backup.AvoraBackupManager;
import com.alidogukan.avora.config.AppInfo;
import com.google.android.gms.tasks.Task;

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
import java.util.function.Consumer;

/** Owns backup generation, validation, file I/O and durable operation history. */
public final class BackupViewModel extends AndroidViewModel {
    private static final String PREFS = "backup_preferences";
    private static final String LAST_BACKUP_TIME = "last_backup_epoch_ms";
    private static final String LAST_BACKUP_NAME = "last_backup_file_name";
    private static final String LAST_RESTORE_TIME = "last_restore_epoch_ms";
    private static final String LAST_RESTORE_NAME = "last_restore_file_name";
    private final AvoraBackupManager manager;
    private final SharedPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private JSONObject pendingBackup;
    private String pendingName;

    public BackupViewModel(@NonNull Application application) {
        super(application);
        manager = new AvoraBackupManager(application);
        preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Task<JSONObject> prepareBackup() {
        return manager.createBackup().addOnSuccessListener(backup -> {
            pendingBackup = backup;
            pendingName = buildFileName();
        });
    }
    public String pendingFileName() { return pendingName; }
    public boolean hasPendingBackup() { return pendingBackup != null; }
    public void clearPendingBackup() { pendingBackup = null; pendingName = null; }

    public void writePending(Uri uri, Consumer<FileResult> completed) {
        JSONObject backup = pendingBackup;
        String fallback = pendingName;
        executor.execute(() -> {
            try (OutputStream output = getApplication().getContentResolver()
                    .openOutputStream(uri, "wt")) {
                if (output == null) throw new IllegalStateException(
                        getApplication().getString(R.string.backup_file_open_error));
                output.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));
                output.flush();
                String name = displayName(uri, fallback);
                preferences.edit().putLong(LAST_BACKUP_TIME, System.currentTimeMillis())
                        .putString(LAST_BACKUP_NAME, name).apply();
                clearPendingBackup();
                completed.accept(FileResult.success(name, null));
            } catch (Exception error) {
                completed.accept(FileResult.failure(error));
            }
        });
    }

    public void read(Uri uri, Consumer<FileResult> completed) {
        executor.execute(() -> {
            try (InputStream input = getApplication().getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalStateException(
                        getApplication().getString(R.string.backup_file_open_error));
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
                JSONObject backup = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
                completed.accept(FileResult.success(displayName(uri,
                        getApplication().getString(R.string.backup_unknown_file)), backup));
            } catch (Exception error) {
                completed.accept(FileResult.failure(error));
            }
        });
    }

    public BackupValidation validate(JSONObject backup) {
        AvoraBackupManager.ValidationResult result = manager.validate(backup);
        return new BackupValidation(result.valid, result.message, result.createdAtEpochMs,
                result.appVersion, result.zoneCount, result.recordCount);
    }

    public Task<Void> restore(JSONObject backup, String displayName) {
        return manager.restoreBackup(backup).addOnSuccessListener(unused ->
                preferences.edit().putLong(LAST_RESTORE_TIME, System.currentTimeMillis())
                        .putString(LAST_RESTORE_NAME, displayName).apply());
    }

    public StoredState storedState() {
        return new StoredState(preferences.getLong(LAST_BACKUP_TIME, 0L),
                preferences.getString(LAST_BACKUP_NAME, ""),
                preferences.getLong(LAST_RESTORE_TIME, 0L),
                preferences.getString(LAST_RESTORE_NAME, ""));
    }

    private String buildFileName() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
                .format(new Date());
        return "AVORA-" + AppInfo.DEVICE_ID + "-" + timestamp + ".avora.json";
    }

    private String displayName(Uri uri, String fallback) {
        try (Cursor cursor = getApplication().getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.isBlank()) return value;
                }
            }
        } catch (Exception ignored) { }
        return fallback;
    }

    @Override protected void onCleared() { executor.shutdown(); }

    public static final class FileResult {
        public final boolean successful;
        public final String displayName;
        public final JSONObject backup;
        public final Throwable error;
        private FileResult(boolean successful, String displayName,
                           JSONObject backup, Throwable error) {
            this.successful = successful;
            this.displayName = displayName;
            this.backup = backup;
            this.error = error;
        }
        static FileResult success(String name, JSONObject backup) {
            return new FileResult(true, name, backup, null);
        }
        static FileResult failure(Throwable error) {
            return new FileResult(false, "", null, error);
        }
    }

    public static final class StoredState {
        public final long backupTime;
        public final String backupName;
        public final long restoreTime;
        public final String restoreName;
        StoredState(long backupTime, String backupName, long restoreTime, String restoreName) {
            this.backupTime = backupTime;
            this.backupName = backupName == null ? "" : backupName;
            this.restoreTime = restoreTime;
            this.restoreName = restoreName == null ? "" : restoreName;
        }
    }

    /** Presentation-safe validation result that does not expose the backup data layer. */
    public static final class BackupValidation {
        public final boolean valid;
        public final String message;
        public final long createdAtEpochMs;
        public final String appVersion;
        public final int zoneCount;
        public final int recordCount;

        BackupValidation(boolean valid, String message, long createdAtEpochMs,
                         String appVersion, int zoneCount, int recordCount) {
            this.valid = valid;
            this.message = message == null ? "" : message;
            this.createdAtEpochMs = createdAtEpochMs;
            this.appVersion = appVersion == null ? "" : appVersion;
            this.zoneCount = zoneCount;
            this.recordCount = recordCount;
        }
    }
}
