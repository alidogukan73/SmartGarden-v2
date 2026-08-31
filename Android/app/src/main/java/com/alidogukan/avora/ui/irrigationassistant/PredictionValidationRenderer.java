package com.alidogukan.avora.ui.irrigationassistant;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.PredictionValidationStatus;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.alidogukan.avora.ui.irrigationassistant.IrrigationAssistantCodes.IDLE;
import static com.alidogukan.avora.ui.irrigationassistant.IrrigationAssistantCodes.WAITING;

/** Owns the prediction-validation card and translates its model into view state. */
public final class PredictionValidationRenderer {
    private static final String TAG = "PredictionValidation";

    private final Context context;
    private final IrrigationAssistantFormatter formatter;
    private final MaterialCardView card;
    private final MaterialCardView statusBadge;
    private final TextView status;
    private final TextView remaining;
    private final TextView percent;
    private final TextView target;
    private final TextView pending;
    private final TextView nextTime;
    private final TextView updatedAt;
    private final LinearProgressIndicator progress;

    public PredictionValidationRenderer(
            @NonNull View root,
            @NonNull IrrigationAssistantFormatter formatter
    ) {
        context = root.getContext();
        this.formatter = formatter;
        card = root.findViewById(R.id.cardPredictionValidation);
        statusBadge = root.findViewById(R.id.cardPredictionValidationStatusBadge);
        status = root.findViewById(R.id.txtPredictionValidationStatus);
        remaining = root.findViewById(R.id.txtPredictionValidationRemaining);
        percent = root.findViewById(R.id.txtPredictionValidationPercent);
        target = root.findViewById(R.id.txtPredictionValidationTarget);
        pending = root.findViewById(R.id.txtPredictionValidationPending);
        nextTime = root.findViewById(R.id.txtPredictionValidationNextTime);
        updatedAt = root.findViewById(R.id.txtPredictionValidationUpdatedAt);
        progress = root.findViewById(R.id.progressPredictionValidation);
    }

    public void setVisible(boolean visible) {
        card.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void render(PredictionValidationStatus model) {
        if (model == null) {
            renderIdle();
            return;
        }
        String validationStatus = formatter.safeText(
                        model.getValidation_status(), IDLE)
                .trim()
                .toUpperCase(Locale.ROOT);
        long pendingCount = Math.max(0, model.getPending_count());
        long targetMinutes = Math.max(0, model.getTarget_minutes());
        long remainingSeconds = Math.max(0, model.getRemaining_seconds());
        boolean waitingForValidation = WAITING.equals(validationStatus)
                && pendingCount > 0;

        if (!waitingForValidation) {
            renderIdle();
            updatedAt.setText(formatDateTime(model.getUpdated_at()));
            return;
        }

        status.setText(R.string.ai_runtime_validation_waiting);
        remaining.setText(formatRemainingTime(remainingSeconds));
        target.setText(context.getString(
                R.string.ai_runtime_target_minutes, targetMinutes));
        pending.setText(String.valueOf(pendingCount));
        nextTime.setText(formatTime(model.getNext_validation_at()));
        updatedAt.setText(formatDateTime(model.getUpdated_at()));

        int progressValue = calculateProgress(targetMinutes, remainingSeconds);
        progress.setProgressCompat(progressValue, true);
        percent.setText(NumberFormat.getPercentInstance().format(
                progressValue / 100.0));
        applyWaitingStyle();
    }

    private void renderIdle() {
        status.setText(R.string.ai_runtime_idle_upper);
        remaining.setText(R.string.ai_runtime_no_pending_validation);
        percent.setText(formatter.unavailableValue());
        target.setText(formatter.unavailableValue());
        pending.setText(String.valueOf(0));
        nextTime.setText(formatter.unavailableValue());
        updatedAt.setText(R.string.ai_runtime_waiting);
        progress.setProgressCompat(0, false);
        applyIdleStyle();
    }

    static int calculateProgress(long targetMinutes, long remainingSeconds) {
        if (targetMinutes <= 0) {
            return 0;
        }
        long totalSeconds = targetMinutes * 60L;
        long elapsedSeconds = Math.max(
                0,
                Math.min(totalSeconds, totalSeconds - remainingSeconds));
        return (int) Math.round(
                elapsedSeconds / (double) totalSeconds * 100.0);
    }

    private String formatRemainingTime(long remainingSeconds) {
        long safeSeconds = Math.max(0, remainingSeconds);
        long hours = safeSeconds / 3600;
        long minutes = safeSeconds % 3600 / 60;
        long seconds = safeSeconds % 60;
        if (hours > 0) {
            return String.format(
                    Locale.getDefault(),
                    context.getString(R.string.ai_runtime_hours_minutes_seconds),
                    hours, minutes, seconds);
        }
        return String.format(
                Locale.getDefault(),
                context.getString(R.string.ai_runtime_minutes_seconds),
                minutes, seconds);
    }

    private String formatTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return formatter.unavailableValue();
        }
        try {
            return LocalDateTime.parse(isoDateTime).format(
                    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        } catch (RuntimeException exception) {
            Log.w(TAG, "Prediction validation time could not be formatted.",
                    exception);
            return formatter.unavailableValue();
        }
    }

    private String formatDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return context.getString(R.string.ai_runtime_waiting);
        }
        try {
            return LocalDateTime.parse(isoDateTime).format(
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy HH:mm:ss", Locale.getDefault()));
        } catch (RuntimeException exception) {
            Log.w(TAG, "Prediction validation update time could not be formatted.",
                    exception);
            return context.getString(R.string.ai_runtime_waiting);
        }
    }

    private void applyWaitingStyle() {
        int primary = ContextCompat.getColor(context, R.color.primary);
        int primaryLight = ContextCompat.getColor(context, R.color.primaryLight);
        statusBadge.setCardBackgroundColor(primaryLight);
        statusBadge.setStrokeColor(primary);
        status.setTextColor(primary);
    }

    private void applyIdleStyle() {
        int textSecondary = ContextCompat.getColor(
                context, R.color.textSecondary);
        int surfaceSoft = ContextCompat.getColor(context, R.color.surfaceSoft);
        statusBadge.setCardBackgroundColor(surfaceSoft);
        statusBadge.setStrokeColor(textSecondary);
        status.setTextColor(textSecondary);
    }
}
