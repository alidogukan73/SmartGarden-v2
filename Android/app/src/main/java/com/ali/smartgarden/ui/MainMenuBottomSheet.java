package com.ali.smartgarden.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.AIAssistantActivity;
import com.ali.smartgarden.activities.AboutActivity;
import com.ali.smartgarden.activities.DeviceHealthActivity;
import com.ali.smartgarden.activities.FertilizationCalendarActivity;
import com.ali.smartgarden.activities.GardenPhotoArchiveActivity;
import com.ali.smartgarden.activities.PlantDoctorActivity;
import com.ali.smartgarden.activities.SeasonReportActivity;
import com.ali.smartgarden.activities.SettingsActivity;
import com.ali.smartgarden.activities.StatisticsActivity;
import com.ali.smartgarden.activities.WateringHistoryActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

/** Compact navigation dashboard shown from the three-dot menu. */
public class MainMenuBottomSheet extends BottomSheetDialogFragment {
    private boolean navigationInProgress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_main_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bind(view, R.id.menuCardPlantDoctor, PlantDoctorActivity.class);
        bind(view, R.id.menuCardAIAssistant, AIAssistantActivity.class);
        bind(view, R.id.menuCardFertilization, FertilizationCalendarActivity.class);
        bind(view, R.id.menuCardStatistics, StatisticsActivity.class);
        bind(view, R.id.menuCardHistory, WateringHistoryActivity.class);
        bind(view, R.id.menuCardPlantJournal, GardenPhotoArchiveActivity.class);
        bind(view, R.id.menuCardSeasonReport, SeasonReportActivity.class);
        bind(view, R.id.menuCardHealth, DeviceHealthActivity.class);
        bind(view, R.id.menuCardSettings, SettingsActivity.class);
        bind(view, R.id.menuCardAbout, AboutActivity.class);

    }

    private void bind(View root, int id, Class<?> screen) {
        MaterialCardView card = root.findViewById(id);
        card.setOnClickListener(view -> openActivity(screen));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setDismissWithAnimation(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(ignored -> {
            FrameLayout sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) return;
            sheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setFitToContents(true);
            behavior.setDraggable(true);
        });
        return dialog;
    }

    private void openActivity(Class<?> screen) {
        if (navigationInProgress) return;
        navigationInProgress = true;
        startActivity(new Intent(requireContext(), screen));
        dismissAllowingStateLoss();
    }
}
