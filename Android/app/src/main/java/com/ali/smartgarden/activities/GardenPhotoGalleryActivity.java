package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Full-screen local gallery with multi-select deletion. */
public class GardenPhotoGalleryActivity extends AppCompatActivity {
    public static final String EXTRA_PICK_MODE = "pick_mode";
    public static final String EXTRA_SELECTED_PHOTO_PATH = "selected_photo_path";
    public static final String EXTRA_SELECTED_PHOTO_ID = "selected_photo_id";

    private LocalGardenPhotoStore photoStore;
    private final List<GardenPhoto> photos = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private GridLayout grid;
    private TextView empty;
    private TextView count;
    private TextView selection;
    private MaterialButton selectAll;
    private MaterialButton deleteSelected;
    private boolean pickMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garden_photo_gallery);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);
        photoStore = new LocalGardenPhotoStore(this);
        bindViews();
        findViewById(R.id.btnGalleryBack).setOnClickListener(view -> closeGallery());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeGallery();
            }
        });
        if (pickMode) {
            selectAll.setVisibility(View.GONE);
            deleteSelected.setVisibility(View.GONE);
            selection.setText("Analize eklemek için bir fotoğrafa dokunun.");
        } else {
            selectAll.setOnClickListener(view -> toggleSelectAll());
            deleteSelected.setOnClickListener(view -> confirmDeleteSelected());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void bindViews() {
        grid = findViewById(R.id.gridGardenPhotos);
        empty = findViewById(R.id.txtGalleryEmpty);
        count = findViewById(R.id.txtGalleryCount);
        selection = findViewById(R.id.txtGallerySelection);
        selectAll = findViewById(R.id.btnSelectAllGardenPhotos);
        deleteSelected = findViewById(R.id.btnDeleteSelectedGardenPhotos);
    }

    private void reload() {
        photos.clear();
        photos.addAll(photoStore.load());
        selectedIds.retainAll(idsOf(photos));
        render();
    }

    private Set<String> idsOf(List<GardenPhoto> source) {
        Set<String> ids = new HashSet<>();
        for (GardenPhoto photo : source) ids.add(photo.getId());
        return ids;
    }

    private void render() {
        grid.removeAllViews();
        boolean hasPhotos = !photos.isEmpty();
        empty.setVisibility(hasPhotos ? View.GONE : View.VISIBLE);
        count.setText(photos.size() + " fotoğraf");
        for (GardenPhoto photo : photos) addPhoto(photo);
        updateSelectionUi();
    }

    private void addPhoto(GardenPhoto photo) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_garden_photo_selectable, grid, false);
        int itemWidth = (getResources().getDisplayMetrics().widthPixels - dp(42)) / 2;
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = itemWidth;
        params.height = dp(202);
        item.setLayoutParams(params);

        ImageView image = item.findViewById(R.id.imgSelectableGardenPhoto);
        TextView date = item.findViewById(R.id.txtSelectableGardenPhotoDate);
        TextView check = item.findViewById(R.id.txtSelectableGardenPhotoCheck);
        MaterialCardView card = (MaterialCardView) item;
        image.setImageURI(android.net.Uri.fromFile(new File(photo.getLocal_path())));
        date.setText(new SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault())
                .format(new Date(photo.getCaptured_at_epoch() * 1000L)));
        boolean selected = selectedIds.contains(photo.getId());
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
        card.setStrokeWidth(dp(selected ? 3 : 1));
        card.setStrokeColor(getColor(selected ? R.color.primary : R.color.textSecondary));
        item.setOnClickListener(view -> {
            if (pickMode) {
                android.content.Intent data = new android.content.Intent();
                data.putExtra(EXTRA_SELECTED_PHOTO_PATH, photo.getLocal_path());
                data.putExtra(EXTRA_SELECTED_PHOTO_ID, photo.getId());
                setResult(RESULT_OK, data);
                finish();
            } else {
                toggle(photo);
            }
        });
        grid.addView(item);
    }

    private void toggle(GardenPhoto photo) {
        if (selectedIds.contains(photo.getId())) selectedIds.remove(photo.getId());
        else selectedIds.add(photo.getId());
        render();
    }

    private void toggleSelectAll() {
        if (selectedIds.size() == photos.size()) selectedIds.clear();
        else selectedIds.addAll(idsOf(photos));
        render();
    }

    private void updateSelectionUi() {
        if (pickMode) return;
        int selected = selectedIds.size();
        selection.setText(selected == 0 ? "Silmek için fotoğrafları seçin"
                : selected + " fotoğraf seçildi");
        selectAll.setText(selected == photos.size() && !photos.isEmpty()
                ? "Seçimi kaldır" : "Tümünü seç");
        deleteSelected.setEnabled(selected > 0);
        deleteSelected.setAlpha(selected > 0 ? 1f : .45f);
    }

    private void confirmDeleteSelected() {
        int selected = selectedIds.size();
        if (selected == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Seçilen fotoğraflar silinsin mi?")
                .setMessage(selected + " fotoğraf Bitki Günlüğü’nden ve bu telefondan kalıcı olarak silinir.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (dialog, which) -> deleteSelected())
                .show();
    }

    private void deleteSelected() {
        new Thread(() -> {
            int deleted = 0;
            for (GardenPhoto photo : new ArrayList<>(photos)) {
                if (selectedIds.contains(photo.getId()) && photoStore.delete(photo)) deleted++;
            }
            int countDeleted = deleted;
            runOnUiThread(() -> {
                selectedIds.clear();
                reload();
                Toast.makeText(this, countDeleted + " fotoğraf silindi.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void closeGallery() {
        finish();
    }
}
