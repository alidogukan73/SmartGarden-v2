package com.ali.smartgarden.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import androidx.annotation.ColorRes;
import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.AIAssistantActivity;
import com.ali.smartgarden.activities.MainActivity;
import com.ali.smartgarden.activities.PlantListActivity;
import com.ali.smartgarden.activities.SettingsHubActivity;
import com.ali.smartgarden.activities.DeviceHealthActivity;

/** Shared fixed navigation used by the main AVORA workspaces. */
public final class PrimaryBottomNavigation {
    public static final int HOME = 0, PLANTS = 1, ASSISTANT = 2, DEVICE_HEALTH = 3, SETTINGS = 4;
    public static final int NOTIFICATIONS = -1;
    private static final int[] CONTAINERS = {R.id.navPrimaryHome, R.id.navPrimaryPlants, R.id.navPrimaryAssistant, R.id.navPrimaryNotifications, R.id.navPrimarySettings};
    private PrimaryBottomNavigation() { }

    public static void bind(Activity activity, int active) {
        applySelection(activity, active);
        activity.findViewById(R.id.navPrimaryHome).setOnClickListener(v -> open(activity, MainActivity.class));
        activity.findViewById(R.id.navPrimaryPlants).setOnClickListener(v -> open(activity, PlantListActivity.class));
        activity.findViewById(R.id.navPrimaryAssistant).setOnClickListener(v -> openAiTools(activity, active));
        activity.findViewById(R.id.navPrimarySettings).setOnClickListener(v -> open(activity, SettingsHubActivity.class));
        activity.findViewById(R.id.navPrimaryNotifications).setOnClickListener(v -> open(activity, DeviceHealthActivity.class));
    }

    private static void applySelection(Activity activity, int active) {
        for (int i = 0; i < CONTAINERS.length; i++) {
            boolean isActive = i == active;
            int color = activity.getColor(isActive ? R.color.primary : R.color.textSecondary);
            View container = activity.findViewById(CONTAINERS[i]);
            container.setBackground(isActive ? activeBackground(activity) : null);
            TextView item = (TextView) container;
            Drawable[] drawables = item.getCompoundDrawablesRelative();
            Drawable top = drawables[1];
            if (top != null) {
                int iconSize = Math.round(20 * activity.getResources().getDisplayMetrics().density);
                top.setBounds(0, 0, iconSize, iconSize);
                item.setCompoundDrawablesRelative(drawables[0], top, drawables[2], drawables[3]);
            }
            item.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            item.setTextColor(color);
            item.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

    }
    private static void open(Activity activity, Class<?> target) {
        if (activity.getClass() == target) return;
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static void openAiTools(Activity activity, int previousActive) {
        View anchor = activity.findViewById(R.id.navPrimaryAssistant);
        if (anchor != null) {
            applySelection(activity, ASSISTANT);
            AiToolsBottomSheet.show(activity, anchor,
                    () -> applySelection(activity, previousActive));
        }
    }

    private static GradientDrawable activeBackground(Activity activity) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(activity.getColor(R.color.onlineBackground));
        background.setCornerRadius(dp(activity, 9));
        background.setStroke(dp(activity, 1), activity.getColor(R.color.primary), dp(activity, 3), dp(activity, 2));
        return background;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
