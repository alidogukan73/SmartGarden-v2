package com.ali.smartgarden.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.fertilization.FertilizerReminderScheduler;
import com.ali.smartgarden.notifications.NotificationSettingsStore;
import com.ali.smartgarden.notifications.NotificationSignalScheduler;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

/** Controls scheduled watering, fertilization and plant follow-up reminders. */
public class ReminderSettingsActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private NotificationSettingsStore settings;
    private MaterialSwitch irrigationReminder;
    private MaterialSwitch fertilizationReminder;
    private MaterialSwitch plantReminder;
    private MaterialSwitch quietHoursSwitch;
    private MaterialButton quietStart;
    private MaterialButton quietEnd;
    private View quietHoursLayout;
    private boolean applyingCloudBackup;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reminder_settings);
        applyWindowInsets();
        settings = new NotificationSettingsStore(this);
        configureToolbar();
        bindViews();
        configureActions();
        renderLocalValues();
        restoreCloudBackup();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.SETTINGS);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reminderSettingsRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

    private void configureToolbar() {
        ((TextView) findViewById(R.id.txtSettingsToolbarTitle))
                .setText(R.string.reminder_settings_title);
        findViewById(R.id.btnSettingsToolbarBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
    }

    private void bindViews() {
        irrigationReminder = findViewById(R.id.switchReminderIrrigation);
        fertilizationReminder = findViewById(R.id.switchReminderFertilization);
        plantReminder = findViewById(R.id.switchReminderPlant);
        quietHoursSwitch = findViewById(R.id.switchReminderQuietHours);
        quietStart = findViewById(R.id.btnReminderQuietStart);
        quietEnd = findViewById(R.id.btnReminderQuietEnd);
        quietHoursLayout = findViewById(R.id.layoutReminderQuietHours);
    }

    private void configureActions() {
        irrigationReminder.setOnCheckedChangeListener((button, checked) ->
                saveReminder("irrigation", checked));
        fertilizationReminder.setOnCheckedChangeListener((button, checked) ->
                saveReminder("fertilization", checked));
        plantReminder.setOnCheckedChangeListener((button, checked) ->
                saveReminder("plant", checked));
        quietHoursSwitch.setOnCheckedChangeListener((button, checked) -> {
            quietHoursLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (applyingCloudBackup) return;
            settings.setQuietHoursEnabled(checked);
            syncAndSchedule();
        });
        quietStart.setOnClickListener(view -> pickTime(true));
        quietEnd.setOnClickListener(view -> pickTime(false));
    }

    private void renderLocalValues() {
        applyingCloudBackup = true;
        irrigationReminder.setChecked(settings.isReminderEnabled("irrigation"));
        fertilizationReminder.setChecked(settings.isReminderEnabled("fertilization"));
        plantReminder.setChecked(settings.isReminderEnabled("plant"));
        quietHoursSwitch.setChecked(settings.isQuietHoursEnabled());
        quietHoursLayout.setVisibility(settings.isQuietHoursEnabled() ? View.VISIBLE : View.GONE);
        renderTimes();
        applyingCloudBackup = false;
    }

    private void restoreCloudBackup() {
        repository.loadNotificationSettings(values -> runOnUiThread(() -> {
            if (!settings.applyBackup(values)) return;
            renderLocalValues();
        }));
    }

    private void saveReminder(String key, boolean checked) {
        if (applyingCloudBackup) return;
        settings.setReminderEnabled(key, checked);
        syncAndSchedule();
    }

    private void pickTime(boolean start) {
        int current = start ? settings.quietStartHour() : settings.quietEndHour();
        new TimePickerDialog(this, (dialog, hour, minute) -> {
            settings.setQuietHours(start ? hour : settings.quietStartHour(),
                    start ? settings.quietEndHour() : hour);
            renderTimes();
            syncAndSchedule();
        }, current, 0, true).show();
    }

    private void renderTimes() {
        quietStart.setText(String.format(Locale.getDefault(), "%02d:00", settings.quietStartHour()));
        quietEnd.setText(String.format(Locale.getDefault(), "%02d:00", settings.quietEndHour()));
    }

    private void syncAndSchedule() {
        repository.saveNotificationSettings(settings.snapshot())
                .addOnSuccessListener(unused -> {
                    FertilizerReminderScheduler.schedule(this);
                    NotificationSignalScheduler.schedule(this);
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        R.string.reminder_settings_save_failed, Toast.LENGTH_SHORT).show());
    }
}