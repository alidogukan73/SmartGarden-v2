package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Displays one durable AVORA notification and its read/saved state. */
public class NotificationDetailActivity extends AppCompatActivity {
    private GardenNotification value; private GardenNotificationManager manager; private MaterialButton save, read;
    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_notification_detail); manager = new GardenNotificationManager(this);
        value = new GardenNotification(); value.setId(getIntent().getStringExtra("id")); value.setType(getIntent().getStringExtra("type")); value.setPriority(getIntent().getStringExtra("priority")); value.setZone_id(getIntent().getStringExtra("zone_id")); value.setTitle(getIntent().getStringExtra("title")); value.setDescription(getIntent().getStringExtra("description")); value.setCreated_at_epoch(getIntent().getLongExtra("created_at_epoch", 0L)); value.setRead(getIntent().getBooleanExtra("read", false)); value.setSaved(getIntent().getBooleanExtra("saved", false));
        ((TextView) findViewById(R.id.txtNotificationDetailIcon)).setText(icon(value.getType())); ((TextView) findViewById(R.id.txtNotificationDetailTitle)).setText(value.getTitle()); ((TextView) findViewById(R.id.txtNotificationDetailTime)).setText(new SimpleDateFormat("dd MMMM yyyy · HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date(value.getCreated_at_epoch() * 1000L))); ((TextView) findViewById(R.id.txtNotificationDetailMeta)).setText("Kategori: " + value.getType() + "\nÖncelik: " + value.getPriority() + "\nİlgili bölge: " + (value.getZone_id().isBlank() ? "Genel" : value.getZone_id())); ((TextView) findViewById(R.id.txtNotificationDetailDescription)).setText(value.getDescription());
        save = findViewById(R.id.btnNotificationSave); read = findViewById(R.id.btnNotificationMarkRead); findViewById(R.id.btnNotificationDetailBack).setOnClickListener(v -> finish());
        save.setOnClickListener(v -> { value.setSaved(!value.isSaved()); manager.setState(value, value.isRead(), value.isSaved()); renderState(); });
        read.setOnClickListener(v -> { value.setRead(true); manager.setState(value, true, value.isSaved()); renderState(); });
        manager.setState(value, true, value.isSaved()); value.setRead(true); renderState(); PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.NOTIFICATIONS);
    }
    private void renderState() { save.setText(value.isSaved() ? "Kaydedildi" : "Kaydet"); read.setText(value.isRead() ? "Okundu" : "Okundu olarak işaretle"); read.setEnabled(!value.isRead()); }
    private String icon(String type) { if ("IRRIGATION".equals(type)) return getString(R.string.symbol_water_drop); if ("FERTILIZATION".equals(type)) return getString(R.string.symbol_plant); if ("STOCK".equals(type)) return getString(R.string.symbol_warning); if ("PHOTO_FOLLOW_UP".equals(type)) return getString(R.string.symbol_camera); if ("PLANT_ASSISTANT".equals(type)) return getString(R.string.symbol_sparkle); return getString(R.string.symbol_bullet); }
}
