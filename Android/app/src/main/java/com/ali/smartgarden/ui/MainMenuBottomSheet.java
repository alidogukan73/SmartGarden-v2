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
import com.ali.smartgarden.activities.AboutActivity;
import com.ali.smartgarden.activities.DeviceHealthActivity;
import com.ali.smartgarden.activities.SettingsActivity;
import com.ali.smartgarden.activities.StatisticsActivity;
import com.ali.smartgarden.activities.WateringHistoryActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

public class MainMenuBottomSheet extends BottomSheetDialogFragment {

    private boolean navigationInProgress = false;


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.bottom_sheet_main_menu,
                container,
                false
        );
    }


    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );

        MaterialCardView menuCardStatistics =
                view.findViewById(R.id.menuCardStatistics);

        MaterialCardView menuCardHistory =
                view.findViewById(R.id.menuCardHistory);

        MaterialCardView menuCardHealth =
                view.findViewById(R.id.menuCardHealth);

        MaterialCardView menuCardSettings =
                view.findViewById(R.id.menuCardSettings);

        MaterialCardView menuCardAbout =
                view.findViewById(R.id.menuCardAbout);

        menuCardStatistics.setOnClickListener(
                clickedView ->
                        openActivity(
                                StatisticsActivity.class
                        )
        );

        menuCardHistory.setOnClickListener(
                clickedView ->
                        openActivity(
                                WateringHistoryActivity.class
                        )
        );

        menuCardHealth.setOnClickListener(
                clickedView ->
                        openActivity(
                                DeviceHealthActivity.class
                        )
        );

        menuCardSettings.setOnClickListener(
                clickedView ->
                        openActivity(
                                SettingsActivity.class
                        )
        );

        menuCardAbout.setOnClickListener(
                clickedView ->
                        openActivity(
                                AboutActivity.class
                        )
        );
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(
            @Nullable Bundle savedInstanceState
    ) {

        BottomSheetDialog dialog =
                (BottomSheetDialog) super.onCreateDialog(
                        savedInstanceState
                );

        dialog.setDismissWithAnimation(
                true
        );

        dialog.setCanceledOnTouchOutside(
                true
        );

        dialog.setOnShowListener(
                dialogInterface -> {

                    FrameLayout bottomSheet =
                            dialog.findViewById(
                                    com.google.android.material.R.id.design_bottom_sheet
                            );

                    if (bottomSheet == null) {
                        return;
                    }

                    bottomSheet.setBackgroundResource(
                            android.R.color.transparent
                    );

                    BottomSheetBehavior<FrameLayout> behavior =
                            BottomSheetBehavior.from(
                                    bottomSheet
                            );

                    behavior.setState(
                            BottomSheetBehavior.STATE_EXPANDED
                    );

                    behavior.setSkipCollapsed(
                            true
                    );

                    behavior.setFitToContents(
                            true
                    );

                    behavior.setDraggable(
                            true
                    );
                }
        );

        return dialog;
    }


    private void openActivity(
            Class<?> activityClass
    ) {

        if (navigationInProgress) {
            return;
        }

        navigationInProgress = true;

        Intent intent =
                new Intent(
                        requireContext(),
                        activityClass
                );

        startActivity(
                intent
        );

        dismissAllowingStateLoss();
    }
}