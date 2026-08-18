package com.ali.smartgarden.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.ali.smartgarden.notifications.NotificationSwipeCallback;
import com.ali.smartgarden.R;
import com.ali.smartgarden.notifications.NotificationCenterAdapter;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.notifications.NotificationSettingsStore;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.ali.smartgarden.firebase.FirebaseRepository;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Chronological AVORA notification center with state and category filters. */
public class NotificationCenterActivity extends AppCompatActivity {
    private static final String ALL = "ALL", SAVED = "SAVED", READ = "READ", UNREAD = "UNREAD";
    private RecyclerView list;
    private NotificationCenterAdapter adapter;
    private TextView summary, empty;
    private GardenNotificationManager manager;
    private String statusFilter = ALL;
    private final Set<String> categoryFilters = new HashSet<>();
    private final BroadcastReceiver notificationChangedReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    render(manager.localNotifications());
                }
            };
    private MaterialButton all, saved, read, unread, category;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_notification_center);
        manager = new GardenNotificationManager(this);
        new FirebaseRepository()
                .observeGardenNotifications()
                .observe(this, values -> {

                    if (values == null) {
                        return;
                    }

                    manager.applyCloudSnapshot(values);
                    render(manager.localNotifications());
                });
        list = findViewById(R.id.listNotifications);

        adapter = new NotificationCenterAdapter(
                this,
                new NotificationCenterAdapter.Listener() {

                    @Override
                    public void onNotificationClick(
                            GardenNotification value
                    ) {

                        openDetail(value);
                    }

                    @Override
                    public void onSaveClick(
                            GardenNotification value
                    ) {

                        manager.setState(
                                value,
                                value.isRead(),
                                !value.isSaved()
                        );

                        render(
                                manager.localNotifications()
                        );
                    }

                    @Override
                    public void onDeleteClick(
                            GardenNotification value
                    ) {

                        confirmDeleteNotification(
                                value
                        );
                    }
                }
        );

        list.setLayoutManager(
                new LinearLayoutManager(this)
        );

        list.setAdapter(adapter);
        NotificationSwipeCallback swipeCallback =
                new NotificationSwipeCallback(
                        adapter
                );

        new ItemTouchHelper(
                swipeCallback
        ).attachToRecyclerView(list);

        new ItemTouchHelper(
                swipeCallback
        ).attachToRecyclerView(list);
        summary = findViewById(R.id.txtNotificationSummary);
        empty = findViewById(R.id.txtNotificationEmpty);
        findViewById(R.id.btnNotificationBack)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnNotificationSettings)
                .setOnClickListener(v ->
                        startActivity(
                                new Intent(
                                        this,
                                        NotificationSettingsActivity.class
                                )
                        )
                );

        findViewById(R.id.btnNotificationClear)
                .setOnClickListener(v -> showClearNotificationsDialog());
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

    @Override
    protected void onStart() {
        super.onStart();

        ContextCompat.registerReceiver(
                this,
                notificationChangedReceiver,
                new IntentFilter(
                        GardenNotificationManager.ACTION_NOTIFICATIONS_CHANGED
                ),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        unregisterReceiver(notificationChangedReceiver);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        render(manager.localNotifications());

        manager.restoreCloudBackup(imported ->
                runOnUiThread(() -> {

                    render(manager.localNotifications());

                    // Önce cloud state alındıktan sonra
                    // local-only kayıtları yedekle.
                    manager.syncLocalBackup();
                })
        );
    }

    private void render(List<GardenNotification> values) {

        List<GardenNotification> visible =
                new ArrayList<>();

        int unreadCount = 0;

        if (values != null) {

            for (GardenNotification value : values) {

                if (value == null) {
                    continue;
                }

                if (!matches(value)) {
                    continue;
                }

                visible.add(value);

                if (!value.isRead()) {
                    unreadCount++;
                }
            }
        }

        adapter.submitList(visible);

        int shown = visible.size();

        summary.setText(
                shown
                        + " bildirim gösteriliyor"
                        + (
                        unreadCount == 0
                                ? ". Tümü okundu."
                                : " · "
                                  + unreadCount
                                  + " okunmamış."
                )
        );

        empty.setVisibility(
                shown == 0
                        ? View.VISIBLE
                        : View.GONE
        );

        empty.setText(
                values == null || values.isEmpty()
                        ? "Henüz bildirim yok."
                        : "Bu filtreye uygun bildirim bulunamadı."
        );

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

    private void showClearNotificationsDialog() {

        List<GardenNotification> notifications =
                manager.localNotifications();

        int removableCount = 0;

        for (GardenNotification value : notifications) {
            if (value != null && !value.isSaved()) {
                removableCount++;
            }
        }

        if (removableCount == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Bildirimleri temizle")
                    .setMessage(
                            "Silinebilecek bildirim yok.\n\n" +
                                    "Kaydedilmiş bildirimler korunuyor."
                    )
                    .setPositiveButton("Tamam", null)
                    .show();

            return;
        }

        int finalRemovableCount = removableCount;

        new AlertDialog.Builder(this)
                .setTitle("Bildirimleri temizle")
                .setMessage(
                        finalRemovableCount +
                                " bildirim kalıcı olarak silinecek.\n\n" +
                                "Kaydedilmiş bildirimler korunacak."
                )
                .setNegativeButton("İptal", null)
                .setPositiveButton("Temizle", (dialog, which) -> {

                    manager.clearUnsavedNotifications(result ->

                            runOnUiThread(() -> {

                                if (result < 0) {

                                    new AlertDialog.Builder(this)
                                            .setTitle("Silinemedi")
                                            .setMessage(
                                                    "Bildirimler silinirken " +
                                                            "Firebase bağlantı hatası oluştu."
                                            )
                                            .setPositiveButton("Tamam", null)
                                            .show();

                                    return;
                                }

                                render(manager.localNotifications());

                                new AlertDialog.Builder(this)
                                        .setTitle("Bildirimler temizlendi")
                                        .setMessage(
                                                result +
                                                        " bildirim silindi.\n\n" +
                                                        "Kaydedilmiş bildirimler korundu."
                                        )
                                        .setPositiveButton("Tamam", null)
                                        .show();
                            })
                    );
                })
                .show();
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

    private void openDetail(GardenNotification value) {
        String applicationId = FertilizerOutcomeFollowUpPolicy.applicationIdFromSource(
                value.getSource_key()
        );
        if (!applicationId.isBlank()) {
            manager.setState(value, true, value.isSaved());
            startActivity(new Intent(this, FertilizerHistoryActivity.class)
                    .putExtra("outcome_application_id", applicationId)
                    .putExtra("zone_id", value.getZone_id()));
            return;
        }
        Intent intent = new Intent(this, NotificationDetailActivity.class);
        intent.putExtra("id", value.getId()).putExtra("type", value.getType()).putExtra("priority", value.getPriority())
                .putExtra("zone_id", value.getZone_id()).putExtra("title", value.getTitle()).putExtra("description", value.getDescription())
                .putExtra("source_key", value.getSource_key())
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

    private void confirmDeleteNotification(
            GardenNotification value
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Bildirimi sil")
                .setMessage(
                        "Bu bildirimi kalıcı olarak silmek istiyor musunuz?"
                )
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Sil",
                        (dialog, which) ->

                                manager.deleteNotification(
                                        value,
                                        success ->

                                                runOnUiThread(() -> {

                                                    if (!success) {

                                                        new AlertDialog.Builder(this)
                                                                .setTitle("Silinemedi")
                                                                .setMessage(
                                                                        "Bildirim silinirken Firebase bağlantı hatası oluştu."
                                                                )
                                                                .setPositiveButton(
                                                                        "Tamam",
                                                                        null
                                                                )
                                                                .show();

                                                        return;
                                                    }

                                                    render(
                                                            manager.localNotifications()
                                                    );
                                                })
                                )
                )
                .show();
    }
}
