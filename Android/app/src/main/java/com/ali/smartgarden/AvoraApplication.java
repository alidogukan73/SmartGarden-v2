package com.ali.smartgarden;

import android.app.Application;

import com.ali.smartgarden.fertilization.FertilizerReminderScheduler;
import com.ali.smartgarden.language.AvoraLanguageManager;
import com.ali.smartgarden.notifications.DeviceConnectionNotificationMonitor;
import com.ali.smartgarden.notifications.NotificationSignalScheduler;
import com.ali.smartgarden.theme.AvoraThemeManager;

public class AvoraApplication extends Application {

    private DeviceConnectionNotificationMonitor
            deviceConnectionMonitor;

    @Override
    public void onCreate() {
        super.onCreate();

        AvoraLanguageManager.applySavedLanguage(this);
        AvoraThemeManager.applySavedTheme(this);

        deviceConnectionMonitor =
                new DeviceConnectionNotificationMonitor(this);

        deviceConnectionMonitor.start();

        // Keep background notification checks scheduled regardless of the entry screen.
        NotificationSignalScheduler.schedule(this);
        FertilizerReminderScheduler.schedule(this);
    }
}