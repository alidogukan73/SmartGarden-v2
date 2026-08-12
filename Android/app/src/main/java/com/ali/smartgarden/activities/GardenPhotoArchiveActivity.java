package com.ali.smartgarden.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.File;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class GardenPhotoArchiveActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private LocalGardenPhotoStore localPhotoStore;
    private LocalGardenEventStore localEventStore;
    private final List<GardenZone> zones = new ArrayList<>();
    private final Map<String, GardenZone> zonesByLabel = new HashMap<>();
    private List<GardenPhoto> allPhotos = new ArrayList<>();
    private MaterialAutoCompleteTextView dropdownZone;
    private TextInputEditText inputNote;
    private ImageView imagePreview;
    private MaterialButton btnUpload;
    private LinearLayout photoList;
    private LinearLayout eventList;
    private TextView empty;
    private Uri selectedImage;
    private Bitmap selectedPhotoBitmap;
    private String relatedApplicationId = "";
    private String requestedZoneId = "";

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                selectedImage = uri;
                selectedPhotoBitmap = null;
                if (uri == null) {
                    return;
                }
                imagePreview.setImageURI(uri);
                imagePreview.setVisibility(View.VISIBLE);
            });

    private final ActivityResultLauncher<Void> photoCamera =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap == null) return;
                selectedImage = null;
                selectedPhotoBitmap = bitmap;
                imagePreview.setImageBitmap(bitmap);
                imagePreview.setVisibility(View.VISIBLE);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garden_photo_archive);
        relatedApplicationId = getIntent().getStringExtra(
                "related_application_id");
        requestedZoneId = getIntent().getStringExtra("zone_id");
        bindViews();
        findViewById(R.id.btnPhotoArchiveBack).setOnClickListener(view -> finish());
        bindActions();
        localPhotoStore = new LocalGardenPhotoStore(this);
        localEventStore = new LocalGardenEventStore(this);
        syncJournalBackup();
        repository.observeGardenZones().observe(this, this::renderZones);
        renderPhotos(localPhotoStore.load());
        renderEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localPhotoStore != null) renderPhotos(localPhotoStore.load());
        if (localEventStore != null) renderEvents();
    }

    private void bindViews() {
        dropdownZone = findViewById(R.id.dropdownPhotoZone);
        inputNote = findViewById(R.id.inputGardenPhotoNote);
        imagePreview = findViewById(R.id.imgSelectedGardenPhoto);
        btnUpload = findViewById(R.id.btnUploadGardenPhoto);
        photoList = findViewById(R.id.layoutGardenPhotoList);
        eventList = findViewById(R.id.layoutGardenEventList);
        empty = findViewById(R.id.txtGardenPhotoEmpty);
        findViewById(R.id.cardPickGardenPhoto).setOnClickListener(view -> pickPhoto());
        findViewById(R.id.btnPickGardenPhotoHeader).setOnClickListener(view -> pickPhoto());
        findViewById(R.id.btnPickGardenPhoto).setOnClickListener(view -> pickPhoto());
        findViewById(R.id.btnTakeGardenPhoto).setOnClickListener(view -> photoCamera.launch(null));
        findViewById(R.id.btnAddGardenEvent).setOnClickListener(view -> showAddEventDialog());
        findViewById(R.id.btnShowAllGardenPhotos).setOnClickListener(view -> {
            startActivity(new Intent(this, GardenPhotoGalleryActivity.class));
        });
    }

    private void bindActions() {
        btnUpload.setOnClickListener(view -> uploadPhoto());
    }

    private void pickPhoto() {
        photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }

    private void renderZones(List<GardenZone> remoteZones) {
        zones.clear();
        zonesByLabel.clear();
        List<String> labels = new ArrayList<>();
        if (remoteZones != null) {
            zones.addAll(remoteZones);
            for (GardenZone zone : zones) {
                String label = (zone.getEmoji() == null ? getString(R.string.symbol_plant) : zone.getEmoji())
                        + " " + zone.getName();
                labels.add(label);
                zonesByLabel.put(label, zone);
                if (zone.getZone_id() != null && zone.getZone_id().equals(requestedZoneId)) {
                    dropdownZone.setText(label, false);
                }
            }
        }
        dropdownZone.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        dropdownZone.setOnItemClickListener((parent, view, position, id) -> {
            requestedZoneId = "";
            renderPhotos(allPhotos);
            renderEvents();
        });
        renderEvents();
    }

    private void showAddEventDialog() {
        GardenZone zone = zonesByLabel.get(String.valueOf(dropdownZone.getText()));
        if (zone == null) {
            Toast.makeText(this, "Önce bir bölge seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] eventTypes = {
                getString(R.string.symbol_plant) + " Dikim / fide şaşırtma",
                getString(R.string.symbol_flower) + " İlk çiçek",
                "🍅 İlk meyve",
                "🍂 Yaprak veya bitki sorunu",
                "🧪 Gübreleme notu",
                getString(R.string.symbol_water_drop) + " Sulama / ekipman notu",
                "🧺 Hasat başlangıcı",
                "📝 Genel not"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Önemli olay türü")
                .setItems(eventTypes, (dialog, position) -> showEventNoteDialog(zone, eventTypes[position]))
                .show();
    }

    private void showEventNoteDialog(GardenZone zone, String eventType) {
        EditText note = new EditText(this);
        note.setHint("Kısa not ekleyin (isteğe bağlı)");
        note.setTextColor(getColor(R.color.textPrimary));
        note.setHintTextColor(getColor(R.color.textSecondary));
        note.setPadding(48, 28, 48, 16);
        new MaterialAlertDialogBuilder(this)
                .setTitle(eventType)
                .setMessage((zone.getEmoji() == null ? getString(R.string.symbol_plant) : zone.getEmoji()) + " " + zone.getName()
                        + " · Bugünün tarihiyle kaydedilecek")
                .setView(note)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    GardenEvent saved = localEventStore.add(zone.getZone_id(), eventType, String.valueOf(note.getText()));
                    repository.saveGardenEvent(saved);
                    renderEvents();
                    Toast.makeText(this, "Önemli olay günlüğe eklendi.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void renderEvents() {
        if (eventList == null || localEventStore == null) return;
        eventList.removeAllViews();
        String selectedZoneId = selectedZoneId();
        List<GardenEvent> events = localEventStore.load();
        int shown = 0;
        for (GardenEvent event : events) {
            if (!selectedZoneId.isEmpty() && !selectedZoneId.equals(event.getZone_id())) continue;
            TextView row = new TextView(this);
            row.setText(event.getType() + "\n"
                    + new SimpleDateFormat("dd-MM-yyyy", Locale.forLanguageTag("tr-TR"))
                    .format(new Date(event.getOccurred_at_epoch() * 1000L))
                    + (event.getNote().isBlank() ? "" : " · " + event.getNote()));
            row.setTextColor(getColor(R.color.textPrimary));
            row.setTextSize(13f);
            row.setPadding(0, 8, 0, 8);
            eventList.addView(row);
            shown++;
            if (shown >= 4) break;
        }
        if (shown == 0) {
            TextView emptyEvents = new TextView(this);
            emptyEvents.setText("Henüz önemli olay kaydı yok.");
            emptyEvents.setTextColor(getColor(R.color.textSecondary));
            emptyEvents.setTextSize(13f);
            emptyEvents.setPadding(0, 8, 0, 4);
            eventList.addView(emptyEvents);
        }
    }

    private void uploadPhoto() {
        GardenZone zone = zonesByLabel.get(String.valueOf(dropdownZone.getText()));
        if (zone == null) {
            Toast.makeText(this, "Önce bir bölge seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImage == null && selectedPhotoBitmap == null) {
            Toast.makeText(this, "Önce bir fotoğraf seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnUpload.setEnabled(false);
        btnUpload.setText("Kaydediliyor...");
        String note = inputNote.getText() == null ? "" : inputNote.getText().toString();
        Uri imageToSave = selectedImage;
        Bitmap bitmapToSave = selectedPhotoBitmap;
        new Thread(() -> {
            try {
                GardenPhoto saved = imageToSave != null
                        ? localPhotoStore.save(imageToSave, zone.getZone_id(), note, relatedApplicationId)
                        : localPhotoStore.save(bitmapToSave, zone.getZone_id(), note, relatedApplicationId);
                repository.saveGardenPhotoMetadata(saved);
                runOnUiThread(() -> {
                    selectedImage = null;
                    selectedPhotoBitmap = null;
                    imagePreview.setImageDrawable(null);
                    imagePreview.setVisibility(View.GONE);
                    inputNote.setText("");
                    renderPhotos(localPhotoStore.load());
                    Toast.makeText(this, "Fotoğraf bu telefondaki arşive kaydedildi.", Toast.LENGTH_SHORT).show();
                    finishUpload();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Fotoğraf kaydedilemedi.", Toast.LENGTH_LONG).show();
                    finishUpload();
                });
            }
        }).start();
    }

    private void renderPhotos(List<GardenPhoto> photos) {
        allPhotos = photos == null ? new ArrayList<>() : new ArrayList<>(photos);
        photoList.removeAllViews();
        String selectedZoneId = selectedZoneId();
        List<GardenPhoto> visiblePhotos = new ArrayList<>();
        for (GardenPhoto photo : allPhotos) {
            if (selectedZoneId.isEmpty() || selectedZoneId.equals(photo.getZone_id())) {
                visiblePhotos.add(photo);
            }
        }
        boolean hasPhotos = !visiblePhotos.isEmpty();
        empty.setVisibility(hasPhotos ? View.GONE : View.VISIBLE);
        if (!hasPhotos) {
            empty.setText(selectedZoneId.isEmpty()
                    ? "Henüz gelişim fotoğrafı yok."
                    : "Bu bölge için henüz gelişim fotoğrafı yok.");
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (GardenPhoto photo : visiblePhotos) {
            View row = inflater.inflate(R.layout.item_garden_photo, photoList, false);
            GardenZone zone = findZone(photo.getZone_id());
            TextView title = row.findViewById(R.id.txtGardenPhotoZone);
            TextView date = row.findViewById(R.id.txtGardenPhotoDate);
            TextView note = row.findViewById(R.id.txtGardenPhotoNote);
            ImageView thumbnail = row.findViewById(R.id.imgGardenPhotoThumb);
            MaterialButton open = row.findViewById(R.id.btnOpenGardenPhoto);
            String zoneLabel = zone == null ? "Bahçe bölgesi" :
                    (zone.getEmoji() == null ? getString(R.string.symbol_plant) : zone.getEmoji()) + " " + zone.getName();
            title.setText(zoneLabel);
            date.setText(new SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault())
                    .format(new Date(photo.getCaptured_at_epoch() * 1000L)));
            note.setText(photo.getNote() == null || photo.getNote().isBlank()
                    ? "Gözlem notu eklenmedi." : photo.getNote());
            File photoFile = new File(photo.getLocal_path());
            thumbnail.setImageURI(Uri.fromFile(photoFile));
            open.setOnClickListener(view -> showPhotoActions(photo, zoneLabel, photoFile));
            row.setOnClickListener(view -> openPhoto(photoFile, zoneLabel));
            photoList.addView(row);
        }
    }

    private void showPhotoActions(GardenPhoto photo, String zoneLabel, File photoFile) {
        boolean hasAnalysis = photo.getAnalysis_title() != null
                && !photo.getAnalysis_title().isBlank();
        String[] actions = hasAnalysis
                ? new String[]{"Fotoğrafı aç", "AI analizini gör", "Fotoğrafı sil"}
                : new String[]{"Fotoğrafı aç", "Fotoğrafı sil"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fotoğraf işlemleri")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        openPhoto(photoFile, zoneLabel);
                    } else if (hasAnalysis && which == 1) {
                        showAnalysis(photo);
                    } else {
                        confirmDelete(photo);
                    }
                })
                .show();
    }

    private void showAnalysis(GardenPhoto photo) {
        String message = safe(photo.getAnalysis_meta())
                + "\n\n" + safe(photo.getAnalysis_context())
                + "\n\nÖneri\n" + safe(photo.getAnalysis_advice());
        new MaterialAlertDialogBuilder(this)
                .setTitle(safe(photo.getAnalysis_title()))
                .setMessage(message.trim())
                .setPositiveButton("Kapat", null)
                .show();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void openPhoto(File photoFile, String zoneLabel) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setImageURI(Uri.fromFile(photoFile));
        new MaterialAlertDialogBuilder(this)
                .setTitle(zoneLabel)
                .setView(image)
                .setPositiveButton("Kapat", null)
                .show();
    }

    private void confirmDelete(GardenPhoto photo) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fotoğraf silinsin mi?")
                .setMessage("Bu fotoğraf Bitki Günlüğü’nden ve bu telefondan kalıcı olarak silinir.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (dialog, which) -> new Thread(() -> {
                    boolean deleted = localPhotoStore.delete(photo);
                    runOnUiThread(() -> {
                        if (deleted) {
                            repository.deleteGardenPhotoMetadata(photo.getId());
                            renderPhotos(localPhotoStore.load());
                            Toast.makeText(this, "Fotoğraf silindi.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Fotoğraf silinemedi.", Toast.LENGTH_LONG).show();
                        }
                    });
                }).start())
                .show();
    }

    private GardenZone findZone(String zoneId) {
        for (GardenZone zone : zones) {
            if (zone.getZone_id() != null && zone.getZone_id().equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }

    private String selectedZoneId() {
        if (requestedZoneId != null && !requestedZoneId.isBlank()) {
            return requestedZoneId;
        }
        GardenZone selected = zonesByLabel.get(String.valueOf(dropdownZone.getText()));
        return selected == null || selected.getZone_id() == null ? "" : selected.getZone_id();
    }

    private void finishUpload() {
        btnUpload.setEnabled(true);
        btnUpload.setText("Arşive kaydet");
    }

    /** Mirrors the existing local journal index; Firebase writes are idempotent by record id. */
    private void syncJournalBackup() {
        if (localEventStore != null) {
            for (GardenEvent event : localEventStore.load()) repository.saveGardenEvent(event);
        }
        if (localPhotoStore != null) {
            for (GardenPhoto photo : localPhotoStore.load()) repository.saveGardenPhotoMetadata(photo);
        }
    }
}
