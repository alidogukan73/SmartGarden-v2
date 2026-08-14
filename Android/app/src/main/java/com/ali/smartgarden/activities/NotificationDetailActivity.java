package com.ali.smartgarden.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Displays one durable AVORA notification and its read/saved state. */
public class NotificationDetailActivity extends AppCompatActivity {

    private GardenNotification value;
    private GardenNotificationManager manager;
    private MaterialButton saveButton;
    private MaterialButton readButton;
    private TextView zoneView;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_detail);
        applyWindowInsets();

        manager = new GardenNotificationManager(this);
        value = notificationFromIntent();
        bindViews();
        renderNotification();
        observeZoneName();
        configureActions();

        manager.setState(value, true, value.isSaved());
        value.setRead(true);
        renderState();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.NOTIFICATIONS);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.notificationDetailRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                }
        );
    }

    private GardenNotification notificationFromIntent() {
        GardenNotification notification = new GardenNotification();
        notification.setId(getIntent().getStringExtra("id"));
        notification.setType(getIntent().getStringExtra("type"));
        notification.setPriority(getIntent().getStringExtra("priority"));
        notification.setZone_id(getIntent().getStringExtra("zone_id"));
        notification.setTitle(getIntent().getStringExtra("title"));
        notification.setDescription(getIntent().getStringExtra("description"));
        notification.setCreated_at_epoch(getIntent().getLongExtra("created_at_epoch", 0L));
        notification.setRead(getIntent().getBooleanExtra("read", false));
        notification.setSaved(getIntent().getBooleanExtra("saved", false));
        return notification;
    }

    private void bindViews() {
        saveButton = findViewById(R.id.btnNotificationSave);
        readButton = findViewById(R.id.btnNotificationMarkRead);
        zoneView = findViewById(R.id.txtNotificationDetailZone);
    }

    private void renderNotification() {
        ((TextView) findViewById(R.id.txtNotificationDetailIcon)).setText(icon(value.getType()));
        ((TextView) findViewById(R.id.txtNotificationDetailTitle)).setText(value.getTitle());
        ((TextView) findViewById(R.id.txtNotificationDetailTime)).setText(formatDate(value.getCreated_at_epoch()));
        ((TextView) findViewById(R.id.txtNotificationDetailCategory)).setText(categoryLabel(value.getType()));

        TextView priorityView = findViewById(R.id.txtNotificationDetailPriority);
        priorityView.setText(priorityLabel(value.getPriority()));
        priorityView.setTextColor(getColor(priorityColor(value.getPriority())));

        zoneView.setText(value.getZone_id().isBlank()
                ? R.string.notification_detail_general
                : R.string.notification_detail_zone_loading);
        ((TextView) findViewById(R.id.txtNotificationDetailDescription)).setText(value.getDescription());
    }

    private void observeZoneName() {
        String zoneId = value.getZone_id();
        if (zoneId.isBlank()) return;

        new FirebaseRepository().observeGardenZones().observe(this, zones -> {
            GardenZone zone = findZone(zones, zoneId);
            if (zone == null) {
                zoneView.setText(readableZoneFallback(zoneId));
                return;
            }
            String name = zone.getName() == null ? "" : zone.getName().trim();
            String emoji = zone.getEmoji() == null ? "" : zone.getEmoji().trim();
            String label = (emoji + " " + name).trim();
            zoneView.setText(label.isBlank() ? readableZoneFallback(zoneId) : label);
        });
    }

    private GardenZone findZone(List<GardenZone> zones, String zoneId) {
        if (zones == null) return null;
        for (GardenZone zone : zones) {
            if (zone != null && zoneId.equals(zone.getZone_id())) return zone;
        }
        return null;
    }

    private String readableZoneFallback(String zoneId) {
        String digits = zoneId.replaceAll("\\D+", "");
        if (!digits.isBlank()) {
            try {
                return "Bölge " + Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
                // Use the general label below when a legacy id cannot be parsed.
            }
        }
        return getString(R.string.notification_detail_general);
    }

    private void configureActions() {
        findViewById(R.id.btnNotificationDetailBack).setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> {
            value.setSaved(!value.isSaved());
            manager.setState(value, value.isRead(), value.isSaved());
            renderState();
        });
        readButton.setOnClickListener(view -> {
            value.setRead(true);
            manager.setState(value, true, value.isSaved());
            renderState();
        });
    }

    private void renderState() {
        saveButton.setText(value.isSaved()
                ? R.string.notification_detail_saved
                : R.string.notification_detail_save);
        readButton.setText(value.isRead()
                ? R.string.notification_detail_read
                : R.string.notification_detail_mark_read);
        readButton.setEnabled(!value.isRead());
    }

    private String formatDate(long epochSeconds) {
        long timestamp = epochSeconds > 0L ? epochSeconds * 1000L : System.currentTimeMillis();
        return new SimpleDateFormat("dd MMMM yyyy · HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }

    private String categoryLabel(String type) {
        if ("IRRIGATION".equals(type)) return getString(R.string.notification_category_irrigation);
        if ("FERTILIZATION".equals(type)) return getString(R.string.notification_category_fertilization);
        if ("STOCK".equals(type)) return getString(R.string.notification_category_stock);
        if ("PHOTO_FOLLOW_UP".equals(type)) return getString(R.string.notification_category_photo);
        if ("PLANT_ASSISTANT".equals(type)) return getString(R.string.notification_category_plant_assistant);
        if ("WEATHER".equals(type)) return getString(R.string.notification_category_weather);
        if ("DEVICE".equals(type)) return getString(R.string.notification_category_device);
        return getString(R.string.notification_category_system);
    }

    private String priorityLabel(String priority) {
        if ("HIGH".equals(priority)) return getString(R.string.notification_priority_high);
        if ("LOW".equals(priority)) return getString(R.string.notification_priority_low);
        return getString(R.string.notification_priority_normal);
    }

    private int priorityColor(String priority) {
        if ("HIGH".equals(priority)) return R.color.error;
        if ("LOW".equals(priority)) return R.color.primary;
        return R.color.warning;
    }

    private String icon(String type) {
        if ("IRRIGATION".equals(type)) return getString(R.string.symbol_water_drop);
        if ("FERTILIZATION".equals(type)) return getString(R.string.symbol_plant);
        if ("STOCK".equals(type)) return getString(R.string.symbol_warning);
        if ("PHOTO_FOLLOW_UP".equals(type)) return getString(R.string.symbol_camera);
        if ("PLANT_ASSISTANT".equals(type)) return getString(R.string.symbol_sparkle);
        if ("WEATHER".equals(type)) return getString(R.string.symbol_sun);
        if ("DEVICE".equals(type)) return getString(R.string.symbol_notification);
        return getString(R.string.symbol_bullet);
    }
}