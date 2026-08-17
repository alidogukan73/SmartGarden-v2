package com.ali.smartgarden.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Manual season record entry point for a single plant journal. */
public final class NewJournalRecordActivity extends AppCompatActivity {
    public static final String EXTRA_ZONE_ID = "zone_id";
    public static final String EXTRA_INITIAL_TYPE = "initial_record_type";
    public static final String EXTRA_RELATED_APPLICATION_ID = "related_application_id";
    public static final String RECORD_TYPE_PHOTO = "Fotoğraf";
    private static final String[] TYPES = {"Gözlem", "Sulama", "Gübreleme", "Fotoğraf", "Olay"};
    private static final int[] TYPE_CARDS = {R.id.cardRecordObservation, R.id.cardRecordWatering, R.id.cardRecordFertilizer, R.id.cardRecordPhoto, R.id.cardRecordEvent};
    private final Calendar selectedDateTime = Calendar.getInstance();
    private String zoneId = "";
    private String relatedApplicationId = "";
    private String selectedType = TYPES[0];
    private static final int MAX_PHOTOS_PER_RECORD = 5;
    private final List<Uri> selectedPhotos = new ArrayList<>();
    private final List<Bitmap> selectedPhotoBitmaps = new ArrayList<>();
    private TextView dateText, timeText, photoState;
    private TextInputEditText noteInput;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS_PER_RECORD), uris -> {
                if (uris == null || uris.isEmpty()) return;
                selectedPhotos.clear();
                selectedPhotoBitmaps.clear();
                selectedPhotos.addAll(uris.subList(0, Math.min(MAX_PHOTOS_PER_RECORD, uris.size())));
                showSelectedPhotoState();
            });

    private final ActivityResultLauncher<Void> photoCamera =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap == null) return;
                if (selectedPhotos.size() + selectedPhotoBitmaps.size() >= MAX_PHOTOS_PER_RECORD) {
                    Toast.makeText(this, "Bir kayda en fazla 5 fotoğraf eklenebilir.", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedPhotoBitmaps.add(bitmap);
                showSelectedPhotoState();
            });

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_new_journal_record);
        zoneId = getIntent().getStringExtra(EXTRA_ZONE_ID);
        if (zoneId == null) zoneId = "";
        relatedApplicationId = getIntent().getStringExtra(EXTRA_RELATED_APPLICATION_ID);
        if (relatedApplicationId == null) relatedApplicationId = "";
        String initialType = getIntent().getStringExtra(EXTRA_INITIAL_TYPE);
        dateText = findViewById(R.id.txtNewRecordDate);
        timeText = findViewById(R.id.txtNewRecordTime);
        photoState = findViewById(R.id.txtNewRecordPhotoState);
        noteInput = findViewById(R.id.inputNewRecordNote);
        findViewById(R.id.btnNewRecordBack).setOnClickListener(v -> finish());
        findViewById(R.id.cardNewRecordDate).setOnClickListener(v -> chooseDate());
        findViewById(R.id.cardNewRecordTime).setOnClickListener(v -> chooseTime());
        findViewById(R.id.cardNewRecordPhotoUpload).setOnClickListener(v -> showPhotoSourceDialog());
        findViewById(R.id.btnNewRecordSave).setOnClickListener(v -> save());
        for (int i = 0; i < TYPE_CARDS.length; i++) {
            final int index = i;
            findViewById(TYPE_CARDS[i]).setOnClickListener(v -> selectType(index));
        }
        refreshDateTime();
        selectType(typeIndex(initialType));
    }

    private int typeIndex(String requestedType) {
        if (requestedType == null || requestedType.isBlank()) return 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(requestedType)) return i;
        }
        return 0;
    }

    private void selectType(int index) {
        selectedType = TYPES[index];
        for (int i = 0; i < TYPE_CARDS.length; i++) {
            MaterialCardView card = findViewById(TYPE_CARDS[i]);
            boolean active = i == index;
            card.setStrokeColor(getColor(active ? R.color.primary : R.color.border));
            card.setStrokeWidth(active ? dp(2) : dp(1));
            card.setCardBackgroundColor(getColor(active ? R.color.surfaceGreen : R.color.card));
        }
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDateTime.set(year, month, day);
            refreshDateTime();
        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void chooseTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
            selectedDateTime.set(Calendar.MINUTE, minute);
            refreshDateTime();
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show();
    }

    private void showPhotoSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fotoğraf ekle")
                .setItems(new String[]{"Fotoğraf çek", "Galeriden seç"}, (dialog, which) -> {
                    if (which == 0) {
                        photoCamera.launch(null);
                    } else {
                        choosePhotoFromGallery();
                    }
                })
                .show();
    }

    private void choosePhotoFromGallery() {
        photoPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    private void showSelectedPhotoState() {
        if (photoState == null) return;
        int count = selectedPhotos.size() + selectedPhotoBitmaps.size();
        photoState.setText(getString(R.string.symbol_check) + " " + count + " fotoğraf seçildi\nEn fazla 5 fotoğraf · değiştirmek için dokunun");
    }

    private void refreshDateTime() {
        dateText.setText(new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")).format(selectedDateTime.getTime()));
        timeText.setText(new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(selectedDateTime.getTime()));
    }

    private void save() {
        if (zoneId.isBlank()) { Toast.makeText(this, "Bölge bilgisi bulunamadı.", Toast.LENGTH_SHORT).show(); return; }
        String note = noteInput.getText() == null ? "" : noteInput.getText().toString().trim();
        long epoch = selectedDateTime.getTimeInMillis() / 1000L;
        try {
            boolean hasPhoto = !selectedPhotos.isEmpty() || !selectedPhotoBitmaps.isEmpty();
            if (hasPhoto) {
                String photoGroupId = relatedApplicationId.isBlank()
                        ? "journal_record_" + UUID.randomUUID()
                        : relatedApplicationId;
                LocalGardenPhotoStore store = new LocalGardenPhotoStore(this);
                FirebaseRepository repository = new FirebaseRepository();
                for (Uri photo : selectedPhotos) {
                    GardenPhoto saved = store.save(photo, zoneId, note, photoGroupId);
                    repository.saveGardenPhotoMetadata(saved);
                }
                for (Bitmap bitmap : selectedPhotoBitmaps) {
                    GardenPhoto saved = store.save(bitmap, zoneId, note, photoGroupId);
                    repository.saveGardenPhotoMetadata(saved);
                }
            }
            // Fotoğraflı kayıt, zaman çizelgesinde tek bir gelişim kaydı olarak gösterilir.
            if (!hasPhoto && !"Fotoğraf".equals(selectedType)) {
                GardenEvent event = new LocalGardenEventStore(this).add(zoneId, selectedType, note, epoch);
                new FirebaseRepository().saveGardenEvent(event);
            }
            Toast.makeText(this, "Günlük kaydı eklendi.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception error) {
            Toast.makeText(this, "Kayıt eklenemedi.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
