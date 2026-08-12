package com.ali.smartgarden.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.notifications.NotificationSettingsStore;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Chronological AVORA notification center with state and category filters. */
public class NotificationCenterActivity extends AppCompatActivity {
    private static final String ALL = "ALL", SAVED = "SAVED", READ = "READ", UNREAD = "UNREAD";
    private LinearLayout list;
    private TextView summary, empty;
    private GardenNotificationManager manager;
    private String statusFilter = ALL;
    private final Set<String> categoryFilters = new HashSet<>();
    private MaterialButton all, saved, read, unread, category;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_notification_center);
        manager = new GardenNotificationManager(this);
        list = findViewById(R.id.layoutNotificationList);
        summary = findViewById(R.id.txtNotificationSummary);
        empty = findViewById(R.id.txtNotificationEmpty);
        findViewById(R.id.btnNotificationBack).setOnClickListener(v -> finish());
        ((ViewGroup) findViewById(R.id.btnNotificationBack).getParent()).getChildAt(2)
                .setOnClickListener(v -> startActivity(new Intent(this, NotificationSettingsActivity.class)));
        all = findViewById(R.id.btnNotificationFilterAll);
        saved = findViewById(R.id.btnNotificationFilterSaved);
        read = findViewById(R.id.btnNotificationFilterRead);
        unread = findViewById(R.id.btnNotificationFilterUnread);
        category = findViewById(R.id.btnNotificationFilterCategory);
        all.setOnClickListener(v -> selectStatus(ALL));
        saved.setOnClickListener(v -> selectStatus(SAVED));
        read.setOnClickListener(v -> selectStatus(READ));
        unread.setOnClickListener(v -> selectStatus(UNREAD));
        category.setOnClickListener(v -> showCategoryFilter());
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.NOTIFICATIONS);
    }

    @Override protected void onResume() {
        super.onResume();
        manager.syncLocalBackup();
        manager.restoreCloudBackup(imported -> runOnUiThread(() -> render(manager.localNotifications())));
        render(manager.localNotifications());
    }

    private void render(List<GardenNotification> values) {
        list.removeAllViews();
        int unreadCount = 0, shown = 0;
        String lastDay = "";
        for (GardenNotification value : values) {
            if (!value.isRead()) unreadCount++;
            if (!matches(value)) continue;
            shown++;
            String day = dayLabel(value.getCreated_at_epoch());
            if (!day.equals(lastDay)) {
                TextView header = text(day, 15, R.color.textPrimary);
                header.setTypeface(null, android.graphics.Typeface.BOLD);
                header.setPadding(0, dp(14), 0, dp(7));
                list.addView(header);
                lastDay = day;
            }
            list.addView(card(value));
        }
        summary.setText(shown + " bildirim g\u00f6steriliyor" + (unreadCount == 0 ? ". T\u00fcm\u00fc okundu." : " \u00b7 " + unreadCount + " okunmam\u0131\u015f."));
        empty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        empty.setText(values.isEmpty() ? "Hen\u00fcz bildirim yok." : "Bu filtreye uygun bildirim bulunamad\u0131.");
        updateFilterButtons();
    }

    private boolean matches(GardenNotification value) {
        if (SAVED.equals(statusFilter) && !value.isSaved()) return false;
        if (READ.equals(statusFilter) && !value.isRead()) return false;
        if (UNREAD.equals(statusFilter) && value.isRead()) return false;
        return categoryFilters.isEmpty() || categoryFilters.contains(NotificationSettingsStore.categoryFor(value.getType()));
    }

    private void selectStatus(String value) {
        statusFilter = value;
        render(manager.localNotifications());
    }

    private void showCategoryFilter() {
        String[] labels = {"Sulama", "G\u00fcbreleme", "Bitki Asistan\u0131", "Hava durumu", "Cihaz ve sistem", "Stok"};
        String[] keys = {"irrigation", "fertilization", "plant", "weather", "device", "stock"};
        boolean[] checked = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) checked[i] = categoryFilters.contains(keys[i]);
        new AlertDialog.Builder(this).setTitle("Kategorileri filtrele")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (isChecked) categoryFilters.add(keys[which]); else categoryFilters.remove(keys[which]);
                })
                .setNegativeButton("Temizle", (dialog, which) -> { categoryFilters.clear(); render(manager.localNotifications()); })
                .setPositiveButton("Uygula", (dialog, which) -> render(manager.localNotifications())).show();
    }

    private void updateFilterButtons() {
        decorate(all, ALL.equals(statusFilter));
        decorate(saved, SAVED.equals(statusFilter));
        decorate(read, READ.equals(statusFilter));
        decorate(unread, UNREAD.equals(statusFilter));
        category.setText(categoryFilters.isEmpty() ? "Filtrele" : "Kategori (" + categoryFilters.size() + ")");
        decorate(category, !categoryFilters.isEmpty());
    }

    private void decorate(MaterialButton button, boolean active) {
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(active ? R.color.surfaceGreen : R.color.card)));
        button.setStrokeColor(ColorStateList.valueOf(getColor(active ? R.color.primary : R.color.border)));
        button.setTextColor(getColor(active ? R.color.primary : R.color.textPrimary));
    }

    private View card(GardenNotification value) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardBackgroundColor(getColor(value.isRead() ? R.color.card : R.color.surfaceGreen));
        card.setStrokeColor(getColor(R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(-1, -2);
        outer.bottomMargin = dp(8);
        card.setLayoutParams(outer);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(12), dp(13), dp(12));
        TextView icon = text(icon(value.getType()), 23, R.color.primary);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(48)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(value.getTitle(), 14, R.color.textPrimary);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView description = text(value.getDescription(), 12, R.color.textSecondary);
        description.setMaxLines(2);
        info.addView(title); info.addView(description);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        TextView time = text(new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date(value.getCreated_at_epoch() * 1000L)), 11, R.color.textSecondary);
        row.addView(time);
        card.setOnClickListener(v -> openDetail(value));
        card.addView(row);
        return card;
    }

    private void openDetail(GardenNotification value) {
        Intent intent = new Intent(this, NotificationDetailActivity.class);
        intent.putExtra("id", value.getId()).putExtra("type", value.getType()).putExtra("priority", value.getPriority())
                .putExtra("zone_id", value.getZone_id()).putExtra("title", value.getTitle()).putExtra("description", value.getDescription())
                .putExtra("created_at_epoch", value.getCreated_at_epoch()).putExtra("read", value.isRead()).putExtra("saved", value.isSaved());
        startActivity(intent);
    }

    private String dayLabel(long epoch) {
        long today = System.currentTimeMillis() / 86400000L;
        long day = epoch * 1000L / 86400000L;
        if (day == today) return "Bug\u00fcn";
        if (day == today - 1) return "D\u00fcn";
        return new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")).format(new Date(epoch * 1000L));
    }
    private String icon(String type) {
        if ("IRRIGATION".equals(type)) return "\uD83D\uDCA7";
        if ("FERTILIZATION".equals(type)) return "\uD83C\uDF31";
        if ("STOCK".equals(type)) return "\u26A0";
        if ("PHOTO_FOLLOW_UP".equals(type)) return "\uD83D\uDCF7";
        if ("PLANT_ASSISTANT".equals(type)) return "\u2726";
        if ("WEATHER".equals(type)) return "\u2600";
        if ("DEVICE".equals(type)) return "\u25A3";
        return "\u2022";
    }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(getColor(color)); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
