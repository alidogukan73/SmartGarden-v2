package com.ali.smartgarden.activities;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.ali.smartgarden.models.GardenZone;
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
    private final List<GardenZone> zones = new ArrayList<>();
    private final Map<String, GardenZone> zonesByLabel = new HashMap<>();
    private List<GardenPhoto> allPhotos = new ArrayList<>();
    private MaterialAutoCompleteTextView dropdownZone;
    private TextInputEditText inputNote;
    private ImageView imagePreview;
    private MaterialButton btnUpload;
    private LinearLayout photoList;
    private TextView empty;
    private Uri selectedImage;
    private String relatedApplicationId = "";
    private String requestedZoneId = "";

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                selectedImage = uri;
                if (uri == null) {
                    return;
                }
                imagePreview.setImageURI(uri);
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
        repository.observeGardenZones().observe(this, this::renderZones);
        renderPhotos(localPhotoStore.load());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localPhotoStore != null) renderPhotos(localPhotoStore.load());
    }

    private void bindViews() {
        dropdownZone = findViewById(R.id.dropdownPhotoZone);
        inputNote = findViewById(R.id.inputGardenPhotoNote);
        imagePreview = findViewById(R.id.imgSelectedGardenPhoto);
        btnUpload = findViewById(R.id.btnUploadGardenPhoto);
        photoList = findViewById(R.id.layoutGardenPhotoList);
        empty = findViewById(R.id.txtGardenPhotoEmpty);
        findViewById(R.id.cardPickGardenPhoto).setOnClickListener(view -> pickPhoto());
        findViewById(R.id.btnPickGardenPhotoHeader).setOnClickListener(view -> pickPhoto());
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
                String label = (zone.getEmoji() == null ? "🌱" : zone.getEmoji())
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
        });
    }

    private void uploadPhoto() {
        GardenZone zone = zonesByLabel.get(String.valueOf(dropdownZone.getText()));
        if (zone == null) {
            Toast.makeText(this, "Önce bir bölge seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImage == null) {
            Toast.makeText(this, "Önce bir fotoğraf seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnUpload.setEnabled(false);
        btnUpload.setText("Kaydediliyor...");
        String note = inputNote.getText() == null ? "" : inputNote.getText().toString();
        Uri imageToSave = selectedImage;
        new Thread(() -> {
            try {
                localPhotoStore.save(imageToSave, zone.getZone_id(), note,
                        relatedApplicationId);
                runOnUiThread(() -> {
                    selectedImage = null;
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
                    (zone.getEmoji() == null ? "🌱" : zone.getEmoji()) + " " + zone.getName();
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
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fotoğraf işlemleri")
                .setItems(new String[]{"Fotoğrafı aç", "Fotoğrafı sil"}, (dialog, which) -> {
                    if (which == 0) {
                        openPhoto(photoFile, zoneLabel);
                    } else {
                        confirmDelete(photo);
                    }
                })
                .show();
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
}
