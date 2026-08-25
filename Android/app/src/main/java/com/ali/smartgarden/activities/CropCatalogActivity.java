package com.ali.smartgarden.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.crop.CropCatalog;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.season.SeasonStartConfiguration;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Manages reusable products without mutating any already archived season snapshot. */
public final class CropCatalogActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private LinearLayout systemContainer;
    private LinearLayout userContainer;
    private TextView userEmpty;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_crop_catalog);
        systemContainer = findViewById(R.id.layoutSystemCrops);
        userContainer = findViewById(R.id.layoutUserCrops);
        userEmpty = findViewById(R.id.txtUserCropsEmpty);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnAddCrop).setOnClickListener(view -> showEditor(null));
        repository.observeCropCatalogItems().observe(this, this::render);
    }

    private void render(List<CropCatalogItem> userItems) {
        systemContainer.removeAllViews();
        userContainer.removeAllViews();
        for (CropCatalogItem item : CropCatalog.builtIns()) addCard(systemContainer, item);
        int visibleUsers = 0;
        if (userItems != null) {
            for (CropCatalogItem item : userItems) {
                if (item == null || !item.isEnabled()) continue;
                addCard(userContainer, item);
                visibleUsers++;
            }
        }
        userEmpty.setVisibility(visibleUsers == 0 ? View.VISIBLE : View.GONE);
    }

    private void addCard(LinearLayout parent, CropCatalogItem item) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surfaceElevated));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(9);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView emoji = text(symbol(item), 30, R.color.textPrimary, Typeface.NORMAL);
        emoji.setGravity(android.view.Gravity.CENTER);
        row.addView(emoji, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        copyParams.setMarginStart(dp(10));
        TextView title = text(item.getName(), 16, R.color.textPrimary, Typeface.BOLD);
        TextView detail = text(getString(R.string.crop_catalog_card_detail,
                item.getIdeal_moisture_min(), item.getIdeal_moisture_max()),
                12, R.color.textSecondary, Typeface.NORMAL);
        TextView source = text(item.isSystemItem()
                        ? getString(R.string.crop_catalog_system_badge)
                        : getString(R.string.crop_catalog_user_badge),
                11, R.color.primary, Typeface.BOLD);
        copy.addView(title);
        copy.addView(detail);
        copy.addView(source);
        row.addView(copy, copyParams);
        if (!item.isSystemItem()) {
            TextView edit = text(getString(R.string.crop_catalog_edit), 12,
                    R.color.primary, Typeface.BOLD);
            row.addView(edit);
            card.setOnClickListener(view -> showEditor(item));
        }
        card.addView(row);
        parent.addView(card);
    }

    private void showEditor(@Nullable CropCatalogItem existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(6);
        form.setPadding(padding, padding, padding, 0);
        EditText name = field(R.string.crop_catalog_name_hint, InputType.TYPE_CLASS_TEXT);
        EditText emoji = field(R.string.crop_catalog_emoji_hint, InputType.TYPE_CLASS_TEXT);
        EditText min = field(R.string.crop_catalog_min_hint, InputType.TYPE_CLASS_NUMBER);
        EditText max = field(R.string.crop_catalog_max_hint, InputType.TYPE_CLASS_NUMBER);
        form.addView(name);
        form.addView(emoji);
        form.addView(min);
        form.addView(max);
        if (existing == null) {
            min.setText(Integer.toString(40));
            max.setText(Integer.toString(60));
        } else {
            name.setText(existing.getName());
            emoji.setText(existing.getEmoji());
            min.setText(String.valueOf(existing.getIdeal_moisture_min()));
            max.setText(String.valueOf(existing.getIdeal_moisture_max()));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.crop_catalog_add_dialog_title
                        : R.string.crop_catalog_edit_dialog_title)
                .setView(form)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_save, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.crop_catalog_deactivate, null);
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String cropName = value(name);
                if (cropName.isBlank()) {
                    name.setError(getString(R.string.crop_catalog_name_required));
                    return;
                }
                int low = number(min, -1);
                int high = number(max, -1);
                if (low < 0 || high > 100 || low >= high) {
                    Toast.makeText(this, R.string.crop_catalog_range_invalid, Toast.LENGTH_LONG).show();
                    return;
                }
                String cropEmoji = value(emoji);
                if (cropEmoji.isBlank()) {
                    cropEmoji = SeasonStartConfiguration.suggestedCropEmoji(cropName);
                }
                CropCatalogItem item = existing == null
                        ? CropCatalog.newUserItem(cropName, cropEmoji, low, high)
                        : existing;
                item.setName(cropName);
                item.setEmoji(cropEmoji);
                item.setPlant_type(SeasonStartConfiguration.customPlantType(cropName));
                item.setIdeal_moisture_min(low);
                item.setIdeal_moisture_max(high);
                repository.saveCropCatalogItem(item)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, R.string.crop_catalog_saved, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(error -> Toast.makeText(this,
                                getString(R.string.crop_catalog_save_failed, error.getMessage()),
                                Toast.LENGTH_LONG).show());
            });
            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view ->
                        confirmDeactivate(existing, dialog));
            }
        });
        dialog.show();
    }

    private void confirmDeactivate(CropCatalogItem item, AlertDialog editor) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.crop_catalog_deactivate_title)
                .setMessage(getString(R.string.crop_catalog_deactivate_message, item.getName()))
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.crop_catalog_deactivate, (dialog, which) ->
                        repository.deactivateCropCatalogItem(item.getCrop_id())
                                .addOnSuccessListener(unused -> editor.dismiss())
                                .addOnFailureListener(error -> Toast.makeText(this,
                                        R.string.crop_catalog_deactivate_failed,
                                        Toast.LENGTH_LONG).show()))
                .show();
    }

    private EditText field(int hintRes, int type) {
        EditText field = new EditText(this);
        field.setHint(hintRes);
        field.setInputType(type);
        field.setSingleLine(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(8);
        field.setLayoutParams(params);
        return field;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(ContextCompat.getColor(this, color));
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private String symbol(CropCatalogItem item) {
        return item.getEmoji() == null || item.getEmoji().isBlank() ? "🌱" : item.getEmoji();
    }

    private static String value(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static int number(EditText field, int fallback) {
        try { return Integer.parseInt(value(field)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
