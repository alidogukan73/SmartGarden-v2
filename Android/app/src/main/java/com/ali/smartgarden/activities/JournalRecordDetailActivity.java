package com.ali.smartgarden.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Detail view for one live plant-journal timeline record. */
public class JournalRecordDetailActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private String manualEventId = "", manualEventType = "", zoneId = "", currentDetail = "";
    private String selectedPhotoPath = "", selectedAdvice = "", photoGroupId = "";
    private GardenPhoto selectedPhotoRecord;
    private long recordEpoch;
    private LinearLayout photosLayout, linksLayout;
    private TextView photosTitle, assistantHeading, assistantText;
    private List<FertilizerApplication> fertilizers = new ArrayList<>();
    private List<WateringHistory> waterings = new ArrayList<>();
    private List<GardenPhoto> relatedPhotos = new ArrayList<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> extraPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (uris == null || uris.isEmpty()) return;
                saveExtraPhotos(uris, null);
            });
    private final ActivityResultLauncher<Void> extraPhotoCamera =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) saveExtraPhotos(null, bitmap);
            });
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_journal_record_detail);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.PLANTS);
        bindIntent();
        bindViews();
        renderStaticDetail();
        renderPhotosAndAnalysis();
        repository.observeFertilizerHistory().observe(this, values -> {
            fertilizers = values == null ? new ArrayList<>() : values;
            renderLinks();
        });
        repository.observeWateringHistory().observe(this, values -> {
            waterings = values == null ? new ArrayList<>() : values;
            renderLinks();
        });
    }

    private void bindIntent() {
        manualEventId = safe(getIntent().getStringExtra("manual_event_id"));
        manualEventType = safe(getIntent().getStringExtra("manual_event_type"));
        zoneId = safe(getIntent().getStringExtra("zone_id"));
        currentDetail = safe(getIntent().getStringExtra("detail"));
        selectedPhotoPath = safe(getIntent().getStringExtra("photo_path"));
        photoGroupId = safe(getIntent().getStringExtra("photo_group_id"));
        selectedAdvice = safe(getIntent().getStringExtra("advice"));
        recordEpoch = getIntent().getLongExtra("time", System.currentTimeMillis() / 1000L);
    }

    private void bindViews() {
        photosLayout = findViewById(R.id.layoutRecordPhotos);
        linksLayout = findViewById(R.id.layoutRecordLinks);
        photosTitle = findViewById(R.id.txtPhotosTitle);
        assistantHeading = findViewById(R.id.txtAssistantHeading);
        assistantText = findViewById(R.id.txtRecordAssistant);
        findViewById(R.id.btnRecordBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRecordEdit).setOnClickListener(v -> editManualRecord());
        findViewById(R.id.btnRecordDelete).setOnClickListener(v -> confirmDelete());
    }

    private void renderStaticDetail() {
        String title = safe(getIntent().getStringExtra("title"));
        String icon = safe(getIntent().getStringExtra("icon"));
        ((TextView) findViewById(R.id.txtRecordTitle)).setText(title.isBlank() ? "Kayıt" : title);
        ((TextView) findViewById(R.id.txtRecordDetail)).setText(currentDetail.isBlank() ? "Açıklama eklenmedi." : currentDetail);
        ((TextView) findViewById(R.id.txtRecordIcon)).setText(icon.isBlank() ? "•" : icon);
        ((TextView) findViewById(R.id.txtRecordDate)).setText(dateTime(recordEpoch));
        boolean editable = !manualEventId.isBlank();
        findViewById(R.id.btnRecordEdit).setVisibility(editable ? View.VISIBLE : View.GONE);
        updateDeleteAction();
    }

    private void renderPhotosAndAnalysis() {
        List<GardenPhoto> related = new ArrayList<>();
        for (GardenPhoto photo : new LocalGardenPhotoStore(this).load()) {
            if (!zoneId.equals(photo.getZone_id())) continue;
            if (!photoGroupId.isBlank() && photoGroupId.equals(photo.getRelated_application_id())) {
                related.add(photo);
                continue;
            }
            if (!selectedPhotoPath.isBlank() && selectedPhotoPath.equals(photo.getLocal_path())) {
                related.add(photo);
                break;
            }
        }
        relatedPhotos = related;
        photosLayout.removeAllViews();
        if (related.isEmpty()) {
            photosTitle.setVisibility(View.GONE);
            photosLayout.setVisibility(View.GONE);
        } else {
            photosTitle.setVisibility(View.VISIBLE);
            photosLayout.setVisibility(View.VISIBLE);
            photosTitle.setText("Fotoğraflar · " + related.size() + " fotoğraf");
            for (GardenPhoto photo : related) addPhoto(photo);
            if (related.size() < 5) addPhotoAddTile();
        }
        GardenPhoto analyzed = related.isEmpty() ? null : related.get(0);
        selectedPhotoRecord = analyzed;
        updateDeleteAction();
        String advice = !selectedAdvice.isBlank() ? selectedAdvice : analyzed == null ? "" : safe(analyzed.getAnalysis_advice());
        String title = analyzed == null ? "" : safe(analyzed.getAnalysis_title());
        boolean hasAdvice = !advice.isBlank() || !title.isBlank();
        assistantHeading.setVisibility(hasAdvice ? View.VISIBLE : View.GONE);
        findViewById(R.id.cardRecordAssistant).setVisibility(hasAdvice ? View.VISIBLE : View.GONE);
        assistantText.setText((title.isBlank() ? "" : title + "\n\n") + advice);
        findViewById(R.id.txtFollowupHeading).setVisibility(View.GONE);
        findViewById(R.id.cardRecordFollowup).setVisibility(View.GONE);
    }

    private void addPhoto(GardenPhoto photo) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageURI(Uri.fromFile(new File(photo.getLocal_path())));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(108), dp(108));
        params.setMarginEnd(dp(8));
        image.setLayoutParams(params);
        image.setOnClickListener(v -> showPhoto(photo));
        photosLayout.addView(image);
    }

    private void addPhotoAddTile() {
        TextView add = new TextView(this);
        add.setText("＋\nFotoğraf ekle");
        add.setTextSize(12); add.setGravity(Gravity.CENTER); add.setTextColor(getColor(R.color.primary));
        add.setBackgroundColor(getColor(R.color.surfaceGreen));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(108), dp(108));
        params.setMarginEnd(dp(8)); add.setLayoutParams(params);
        add.setOnClickListener(v -> showExtraPhotoSourceDialog());
        photosLayout.addView(add);
    }

    private void showExtraPhotoSourceDialog() {
        int remaining = 5 - relatedPhotos.size();
        if (remaining <= 0) { Toast.makeText(this, "Bu kayıtta en fazla 5 fotoğraf olabilir.", Toast.LENGTH_SHORT).show(); return; }
        new MaterialAlertDialogBuilder(this).setTitle("Fotoğraf ekle")
                .setItems(new String[]{"Fotoğraf çek", "Galeriden seç"}, (dialog, which) -> {
                    if (which == 0) extraPhotoCamera.launch(null);
                    else extraPhotoPicker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                }).show();
    }

    private void saveExtraPhotos(List<Uri> uris, Bitmap bitmap) {
        if (selectedPhotoRecord == null) return;
        int remaining = 5 - relatedPhotos.size();
        if (remaining <= 0) return;
        try {
            LocalGardenPhotoStore store = new LocalGardenPhotoStore(this);
            if (!photoGroupId.startsWith("journal_record_")) {
                photoGroupId = "journal_record_" + UUID.randomUUID();
                store.updateRelatedApplicationId(selectedPhotoRecord.getId(), photoGroupId);
            }
            if (bitmap != null) store.save(bitmap, zoneId, currentDetail, photoGroupId);
            else if (uris != null) {
                for (int i = 0; i < Math.min(remaining, uris.size()); i++) store.save(uris.get(i), zoneId, currentDetail, photoGroupId);
            }
            renderPhotosAndAnalysis();
            Toast.makeText(this, "Fotoğraf kayda eklendi.", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Fotoğraf eklenemedi.", Toast.LENGTH_SHORT).show();
        }
    }
    private void showPhoto(GardenPhoto photo) {
        ImageView full = new ImageView(this);
        full.setAdjustViewBounds(true);
        full.setImageURI(Uri.fromFile(new File(photo.getLocal_path())));
        new MaterialAlertDialogBuilder(this).setTitle("Gelişim fotoğrafı").setView(full).setPositiveButton("Kapat", null).show();
    }

    private void renderLinks() {
        linksLayout.removeAllViews();
        int count = 0;
        for (FertilizerApplication item : fertilizers) {
            if (!zoneId.equals(item.getZone_id()) || isSameRecord(item.getApplied_at_epoch())) continue;
            addLinkedCard("🌿", "Gübreleme", safe(item.getProduct_name()) + " · " + trimNumber(item.getApplied_dose()) + " " + safe(item.getDose_unit()), item.getApplied_at_epoch());
            if (++count == 2) return;
        }
        for (WateringHistory item : waterings) {
            long when = parseWateringTime(item.getFinishedAt());
            if (!zoneId.equals(item.getZoneId()) || !item.isCompleted() || isSameRecord(when)) continue;
            addLinkedCard(getString(R.string.symbol_water_drop), "Sulama", "Süre: " + item.getDuration() + " sn", when);
            if (++count == 2) return;
        }
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("Bu kayıtla ilişkili sulama veya gübreleme kaydı yok.");
            empty.setTextColor(getColor(R.color.textSecondary));
            empty.setTextSize(12);
            empty.setPadding(dp(6), dp(10), dp(6), dp(6));
            linksLayout.addView(empty);
        }
    }

    private boolean isSameRecord(long time) { return time > 0L && Math.abs(time - recordEpoch) < 90L; }

    private void addLinkedCard(String icon, String title, String detail, long epoch) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(12));
        card.setCardBackgroundColor(getColor(R.color.card));
        card.setStrokeColor(getColor(R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(8);
        card.setLayoutParams(cardParams);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView mark = new TextView(this); mark.setText(icon); mark.setTextSize(19); mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        TextView heading = new TextView(this); heading.setText(title + " · " + dateTime(epoch)); heading.setTextColor(getColor(R.color.textPrimary)); heading.setTextSize(12); heading.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView text = new TextView(this); text.setText(detail); text.setTextColor(getColor(R.color.textSecondary)); text.setTextSize(12);
        info.addView(heading); info.addView(text);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = new TextView(this); arrow.setText("›"); arrow.setTextSize(28); arrow.setTextColor(getColor(R.color.textSecondary));
        row.addView(arrow, new LinearLayout.LayoutParams(dp(22), -2));
        card.addView(row);
        card.setOnClickListener(v -> openLinkedRecord(title, detail, icon, epoch));
        linksLayout.addView(card);
    }

    private void openLinkedRecord(String title, String detail, String icon, long epoch) {
        Intent intent = new Intent(this, JournalRecordDetailActivity.class);
        intent.putExtra("title", title); intent.putExtra("detail", detail); intent.putExtra("icon", icon); intent.putExtra("time", epoch); intent.putExtra("zone_id", zoneId);
        startActivity(intent);
    }

    private long parseWateringTime(String value) {
        if (value == null || value.isBlank()) return 0L;
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy HH:mm", "dd.MM.yyyy HH:mm"};
        for (String pattern : patterns) try { return new SimpleDateFormat(pattern, Locale.US).parse(value).getTime() / 1000L; } catch (Exception ignored) { }
        return 0L;
    }

    private void editManualRecord() {
        EditText input = new EditText(this); input.setText(currentDetail); input.setMinLines(3);
        new MaterialAlertDialogBuilder(this).setTitle("Kaydı düzenle").setView(input).setNegativeButton("İptal", null).setPositiveButton("Kaydet", (d, w) -> {
            String newNote = String.valueOf(input.getText());
            if (new LocalGardenEventStore(this).update(manualEventId, manualEventType, newNote)) {
                GardenEvent event = new GardenEvent(); event.setId(manualEventId); event.setZone_id(zoneId); event.setType(manualEventType); event.setNote(newNote); event.setOccurred_at_epoch(recordEpoch);
                repository.saveGardenEvent(event); finish();
            }
        }).show();
    }

    private void updateDeleteAction() {
        boolean userRecord = !manualEventId.isBlank() || selectedPhotoRecord != null;
        findViewById(R.id.btnRecordDelete).setVisibility(userRecord ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete() {
        if (manualEventId.isBlank() && selectedPhotoRecord == null) return;
        String message = !manualEventId.isBlank()
                ? "Bu kullanıcı kaydı silinecek. Otomatik sulama ve gübreleme geçmişi etkilenmez."
                : "Bu kullanıcı fotoğraf kaydı ve telefonunuzdaki kopyası silinecek.";
        new MaterialAlertDialogBuilder(this).setTitle("Kaydı sil?").setMessage(message)
                .setNegativeButton("Vazgeç", null).setPositiveButton("Sil", (d, w) -> {
                    if (!manualEventId.isBlank()) {
                        if (new LocalGardenEventStore(this).delete(manualEventId)) {
                            repository.deleteGardenEvent(manualEventId);
                        }
                    } else if (selectedPhotoRecord != null) {
                        LocalGardenPhotoStore store = new LocalGardenPhotoStore(this);
                        if (!photoGroupId.isBlank() && photoGroupId.startsWith("journal_record_")) {
                            for (GardenPhoto photo : store.load()) if (photoGroupId.equals(photo.getRelated_application_id())) store.delete(photo);
                        } else store.delete(selectedPhotoRecord);
                    }
                    finish();
                }).show();
    }

    private String dateTime(long epoch) { return new SimpleDateFormat("dd MMMM yyyy · HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date(Math.max(epoch, 1L) * 1000L)); }
    private String trimNumber(double value) { return Math.abs(value - Math.rint(value)) < 0.01 ? String.valueOf((long) value) : String.format(Locale.US, "%.1f", value); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
