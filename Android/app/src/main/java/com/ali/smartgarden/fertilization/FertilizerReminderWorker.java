package com.ali.smartgarden.fertilization;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ali.smartgarden.R;
import com.ali.smartgarden.activities.FertilizationCalendarActivity;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FertilizerReminderWorker extends Worker {

    private static final String DEVICE_ID = "smartgarden-001";
    private static final String CHANNEL_ID =
            "fertilizer_reminders";
    private static final String PREFS =
            "fertilizer_reminder_state";

    private static class DoseInfo {
        double min;
        double max;
        String unit;
    }

    public FertilizerReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters
    ) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createChannel(context);
        if (!canNotify(context)) {
            return Result.success();
        }
        try {
            com.google.firebase.database.DatabaseReference deviceRef =
                    FirebaseDatabase.getInstance()
                            .getReference("devices")
                            .child(DEVICE_ID);
            DataSnapshot snapshot = Tasks.await(
                    deviceRef.child("zones").get(),
                    20,
                    TimeUnit.SECONDS
            );
            DataSnapshot productSnapshot = Tasks.await(
                    deviceRef.child("fertilizer_products").get(),
                    20,
                    TimeUnit.SECONDS
            );
            DataSnapshot recommendationSnapshot = Tasks.await(
                    deviceRef.child("fertilization")
                            .child("recommendations")
                            .get(),
                    20,
                    TimeUnit.SECONDS
            );
            Map<String, FertilizerProduct> products = new HashMap<>();
            for (DataSnapshot child
                    : productSnapshot.getChildren()) {
                FertilizerProduct product = child.getValue(
                        FertilizerProduct.class
                );
                if (product != null) {
                    products.put(
                            safe(product.getProduct_id(), child.getKey()),
                            product
                    );
                }
            }
            List<FertilizerRecommendation> recommendations =
                    new ArrayList<>();
            collectRecommendations(
                    recommendationSnapshot,
                    recommendations
            );
            for (DataSnapshot child : snapshot.getChildren()) {
                GardenZone zone = child.getValue(GardenZone.class);
                notifyIfDue(
                        context,
                        zone,
                        products,
                        recommendations
                );
            }
            return Result.success();
        } catch (Exception error) {
            return Result.retry();
        }
    }

    private void notifyIfDue(
            Context context,
            GardenZone zone,
            Map<String, FertilizerProduct> products,
            List<FertilizerRecommendation> recommendations
    ) {
        if (zone == null || !zone.isEnabled()) {
            return;
        }
        FertilizationProfile profile = zone.getFertilization();
        if (profile == null
                || !profile.isEnabled()
                || !profile.isReminder_enabled()
                || profile.getNext_application_at_epoch() <= 0L) {
            return;
        }

        LocalDate due = Instant.ofEpochSecond(
                profile.getNext_application_at_epoch()
        ).atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
        if (days > 0L) {
            return;
        }

        String zoneId = safe(zone.getZone_id(), "unknown");
        String today = LocalDate.now().toString();
        String preferenceKey = zoneId + ":" + today;
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
        if (preferences.getBoolean(preferenceKey, false)) {
            return;
        }

        String zoneName = safe(zone.getName(), zoneId);
        String title = days == 0L
                ? context.getString(
                        R.string.fertilizer_notification_today_title
                )
                : context.getString(
                        R.string.fertilizer_notification_overdue_title
                );
        String message = days == 0L
                ? context.getString(
                        R.string.fertilizer_notification_today_message,
                        zoneName
                )
                : context.getString(
                        R.string.fertilizer_notification_overdue_message,
                        zoneName,
                        Math.abs(days)
                );
        FertilizerProduct product = products.get(
                profile.getActive_product_id()
        );
        if (product != null) {
            DoseInfo dose = calculateDose(
                    zone,
                    profile,
                    product,
                    recommendations
            );
            if (dose == null) {
                message += context.getString(
                        R.string
                                .fertilizer_notification_measurement_missing
                );
            } else {
                message += context.getString(
                        R.string.fertilizer_notification_dose,
                        product.getName(),
                        format(dose.min),
                        format(dose.max),
                        dose.unit
                );
                String stockUnit = safe(product.getStock_unit(), "");
                if (stockUnit.isBlank()
                        || !stockUnit.equalsIgnoreCase(dose.unit)) {
                    message += context.getString(
                            R.string
                                    .fertilizer_notification_stock_unknown
                    );
                } else if (product.getStock_amount() < dose.max) {
                    message += context.getString(
                            R.string
                                    .fertilizer_notification_stock_short,
                            format(
                                    dose.max
                                            - product.getStock_amount()
                            ),
                            dose.unit
                    );
                } else {
                    message += context.getString(
                            R.string.fertilizer_notification_stock_ok
                    );
                }
            }
        }

        Intent intent = new Intent(
                context,
                FertilizationCalendarActivity.class
        );
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                zoneId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);
        NotificationManagerCompat.from(context).notify(
                41000 + Math.abs(zoneId.hashCode() % 10000),
                notification.build()
        );
        preferences.edit().putBoolean(preferenceKey, true).apply();
    }

    private static DoseInfo calculateDose(
            GardenZone zone,
            FertilizationProfile profile,
            FertilizerProduct product,
            List<FertilizerRecommendation> recommendations
    ) {
        double min = product.getLabel_dosage_min() > 0.0
                ? product.getLabel_dosage_min()
                : product.getLabel_dosage();
        double max = product.getLabel_dosage_max() > 0.0
                ? product.getLabel_dosage_max()
                : product.getLabel_dosage();
        String sourceUnit = safe(product.getDosage_unit(), "");
        for (FertilizerRecommendation recommendation
                : recommendations) {
            if (safe(zone.getPlant_type(), "").equals(
                    safe(recommendation.getPlant_type(), "")
            ) && safe(profile.getGrowth_stage(), "").equals(
                    safe(recommendation.getGrowth_stage(), "")
            ) && safe(product.getProduct_id(), "").equals(
                    safe(recommendation.getProduct_id(), "")
            )) {
                min = recommendation.getDose_min();
                max = recommendation.getDose_max();
                sourceUnit = safe(
                        recommendation.getDose_unit(),
                        sourceUnit
                );
                break;
            }
        }
        if (max <= 0.0) {
            max = min;
        }
        String normalized = sourceUnit.toLowerCase(Locale.ROOT)
                .replace(" ", "");
        DoseInfo result = new DoseInfo();
        if (normalized.contains("kg/dekar")
                && profile.getArea_m2() > 0.0) {
            result.min = min * profile.getArea_m2();
            result.max = max * profile.getArea_m2();
            result.unit = "g";
            return result;
        }
        if (normalized.contains("l/dekar")
                && profile.getArea_m2() > 0.0) {
            result.min = min * profile.getArea_m2();
            result.max = max * profile.getArea_m2();
            result.unit = "ml";
            return result;
        }
        if (normalized.contains("ml/100l")
                && profile.getTank_liters() > 0.0) {
            result.min = min * profile.getTank_liters() / 100.0;
            result.max = max * profile.getTank_liters() / 100.0;
            result.unit = "ml";
            return result;
        }
        return null;
    }

    private static void collectRecommendations(
            DataSnapshot snapshot,
            List<FertilizerRecommendation> output
    ) {
        FertilizerRecommendation direct = snapshot.getValue(
                FertilizerRecommendation.class
        );
        if (direct != null
                && direct.getProduct_id() != null
                && !direct.getProduct_id().isBlank()) {
            output.add(direct);
            return;
        }
        for (DataSnapshot child : snapshot.getChildren()) {
            collectRecommendations(child, output);
        }
    }

    private static String format(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(
                        R.string.fertilizer_notification_channel
                ),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(
                context.getString(
                        R.string.fertilizer_notification_channel_description
                )
        );
        NotificationManager manager = context.getSystemService(
                NotificationManager.class
        );
        manager.createNotificationChannel(channel);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
