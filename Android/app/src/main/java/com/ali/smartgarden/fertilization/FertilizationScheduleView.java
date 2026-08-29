package com.ali.smartgarden.fertilization;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ali.smartgarden.R;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders the zone fertilization plan and owns its expandable schedule state. */
public final class FertilizationScheduleView {
    private final Context context;
    private final MaterialCardView card;
    private final TextView planStatus;
    private final TextView summary;
    private final TextView toggle;
    private final View details;
    private final TextView nutrition;
    private final TextView organic;
    private final TextView conditioner;
    private final TextView biostimulant;
    private boolean expanded;

    public FertilizationScheduleView(@NonNull View root) {
        context = root.getContext();
        card = root.findViewById(R.id.cardZoneApplicationSchedule);
        planStatus = root.findViewById(R.id.txtPlanStatus);
        summary = root.findViewById(R.id.txtScheduleSummary);
        toggle = root.findViewById(R.id.txtScheduleToggle);
        details = root.findViewById(R.id.layoutScheduleDetails);
        nutrition = root.findViewById(R.id.txtNutritionSchedule);
        organic = root.findViewById(R.id.txtOrganicSchedule);
        conditioner = root.findViewById(R.id.txtConditionerSchedule);
        biostimulant = root.findViewById(R.id.txtBiostimulantSchedule);
        root.findViewById(R.id.layoutScheduleHeader).setOnClickListener(
                ignored -> toggleDetails());
    }

    public void renderAdvice(FertilizerAdvice advice) {
        if (advice == null) {
            summary.setText(R.string.fertilization_unified_plan_preparing);
            nutrition.setText(
                    R.string.fertilization_unified_plan_preparing_detail);
            organic.setVisibility(View.GONE);
            conditioner.setVisibility(View.GONE);
            biostimulant.setVisibility(View.GONE);
            return;
        }

        FertilizerAdvice.Recommendation recommendation = advice.getRecommendation();
        nutrition.setText(context.getString(
                R.string.fertilization_unified_need,
                recommendation.isAvailable()
                        ? recommendation.getNeed()
                        : localizedAdviceStatus(advice.getStatus())));
        organic.setVisibility(View.VISIBLE);
        organic.setText(context.getString(
                R.string.fertilization_unified_product,
                recommendation.isAvailable()
                        ? recommendation.getProductName()
                        : context.getString(
                        R.string.fertilization_unified_no_product)));
        conditioner.setVisibility(View.VISIBLE);
        conditioner.setText(timingText(advice, recommendation));
        biostimulant.setVisibility(View.VISIBLE);
        biostimulant.setText(context.getString(
                R.string.fertilization_unified_basis,
                advice.getContext() == null || advice.getContext().isBlank()
                        ? advice.getReason()
                        : advice.getContext()));
        renderSummary(advice, recommendation);
    }

    public void renderPlanStatus(
            boolean seasonEnded,
            boolean enabled,
            boolean configured
    ) {
        card.setVisibility(seasonEnded ? View.GONE : View.VISIBLE);
        if (seasonEnded) {
            collapse();
        }

        int message;
        int color = R.color.textSecondary;
        if (seasonEnded) {
            message = R.string.fertilization_plan_season_end_description;
        } else if (!enabled) {
            message = R.string.fertilization_plan_disabled_description;
        } else if (!configured) {
            message = R.string.fertilization_plan_missing_description;
            color = R.color.warning;
        } else {
            message = R.string.fertilization_plan_active_description;
            color = R.color.primary;
        }
        planStatus.setText(message);
        planStatus.setTextColor(context.getColor(color));
    }

    private void renderSummary(
            FertilizerAdvice advice,
            FertilizerAdvice.Recommendation recommendation
    ) {
        if (isApplicationReady(advice, recommendation)) {
            summary.setText(context.getString(
                    R.string.fertilization_unified_summary_today,
                    recommendation.getNeed()));
            summary.setTextColor(context.getColor(R.color.warning));
        } else if (recommendation.isAvailable()
                && recommendation.getWaitDays() > 0L) {
            summary.setText(context.getString(
                    R.string.fertilization_unified_summary_wait,
                    recommendation.getNeed(),
                    recommendation.getWaitDays()));
            summary.setTextColor(context.getColor(R.color.textSecondary));
        } else {
            summary.setText(context.getString(
                    R.string.fertilization_unified_summary_status,
                    localizedAdviceStatus(advice.getStatus())));
            summary.setTextColor(context.getColor(R.color.textSecondary));
        }
    }

    private String timingText(
            FertilizerAdvice advice,
            FertilizerAdvice.Recommendation recommendation
    ) {
        if (recommendation.isAvailable()
                && recommendation.getWaitDays() > 0L) {
            LocalDate next = LocalDate.now().plusDays(
                    recommendation.getWaitDays());
            return context.getString(
                    R.string.fertilization_unified_timing_wait,
                    recommendation.getWaitDays(),
                    next.format(DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy", Locale.getDefault())));
        }
        if (isApplicationReady(advice, recommendation)) {
            return context.getString(R.string.fertilization_unified_timing_today);
        }
        return context.getString(
                R.string.fertilization_unified_timing_safety,
                advice.getReason());
    }

    private boolean isApplicationReady(
            FertilizerAdvice advice,
            FertilizerAdvice.Recommendation recommendation
    ) {
        return FertilizerAdvice.STATUS_TODAY_ADVICE.equals(advice.getStatus())
                && recommendation.isAvailable()
                && recommendation.isApplicationReady();
    }

    private String localizedAdviceStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case FertilizerAdvice.STATUS_ORGANIC_REQUIRED:
                return context.getString(R.string.runtime_status_organic_required);
            case FertilizerAdvice.STATUS_PREPARATION_REQUIRED:
                return context.getString(R.string.runtime_status_preparation_required);
            case FertilizerAdvice.STATUS_TOO_EARLY:
                return context.getString(R.string.runtime_status_too_early);
            case FertilizerAdvice.STATUS_SEASON_COMPLETED:
                return context.getString(R.string.runtime_status_season_completed);
            case FertilizerAdvice.STATUS_PLAN_NOT_READY:
                return context.getString(R.string.runtime_status_plan_not_ready);
            case FertilizerAdvice.STATUS_PLAN_INACTIVE:
                return context.getString(R.string.runtime_status_plan_inactive);
            case FertilizerAdvice.STATUS_REFRESH_DATA:
                return context.getString(R.string.runtime_status_refresh_data);
            case FertilizerAdvice.STATUS_WATERING_FIRST:
                return context.getString(R.string.runtime_status_watering_first);
            case FertilizerAdvice.STATUS_TODAY_ADVICE:
                return context.getString(R.string.runtime_status_today_advice);
            default:
                return status;
        }
    }

    private void toggleDetails() {
        expanded = !expanded;
        details.setVisibility(expanded ? View.VISIBLE : View.GONE);
        toggle.setText(expanded
                ? R.string.fertilization_schedule_hide
                : R.string.fertilization_schedule_show);
    }

    private void collapse() {
        expanded = false;
        details.setVisibility(View.GONE);
        toggle.setText(R.string.fertilization_schedule_show);
    }
}
