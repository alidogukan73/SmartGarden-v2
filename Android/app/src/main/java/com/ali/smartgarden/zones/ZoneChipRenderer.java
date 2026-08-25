package com.ali.smartgarden.zones;

import android.content.Context;
import android.view.View;

import androidx.annotation.StringRes;

import com.ali.smartgarden.models.GardenZone;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.function.Consumer;

/** Renders zone filters from Firebase instead of fixed crop assumptions. */
public final class ZoneChipRenderer {
    private static final String ALL_TAG = "";

    private ZoneChipRenderer() { }

    public static void render(Context context, ChipGroup group,
                              List<GardenZone> zones, String selectedZoneId,
                              @StringRes int allText,
                              Consumer<String> onSelected) {
        group.setOnCheckedStateChangeListener(null);
        group.removeAllViews();
        group.setSingleSelection(true);
        group.setSelectionRequired(true);

        Chip all = chip(context, context.getString(allText), ALL_TAG);
        group.addView(all);
        int selectedChipId = all.getId();

        if (zones != null) {
            for (GardenZone zone : zones) {
                if (zone == null || ZoneCapacityPolicy.isInactive(zone)) continue;
                String zoneId = safe(zone.getZone_id());
                if (!ZoneCapacityPolicy.isValidZoneId(zoneId)) continue;
                String name = safe(zone.getName());
                if (name.isEmpty()) name = zoneId;
                String emoji = safe(zone.getEmoji());
                Chip chip = chip(context,
                        emoji.isEmpty() ? name : emoji + " " + name, zoneId);
                group.addView(chip);
                if (zoneId.equals(selectedZoneId)) selectedChipId = chip.getId();
            }
        }

        group.check(selectedChipId);
        group.setOnCheckedStateChangeListener((ignored, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            View checked = group.findViewById(checkedIds.get(0));
            Object tag = checked == null ? null : checked.getTag();
            onSelected.accept(tag == null ? "" : String.valueOf(tag));
        });
    }

    private static Chip chip(Context context, String text, String zoneId) {
        Chip chip = new Chip(context);
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setTag(zoneId);
        chip.setCheckable(true);
        chip.setEnsureMinTouchTargetSize(true);
        return chip;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
