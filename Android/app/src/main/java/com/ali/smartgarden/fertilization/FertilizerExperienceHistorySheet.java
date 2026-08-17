package com.ali.smartgarden.fertilization;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenZone;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shows evaluated fertilizer outcomes for one zone and one recommended product. */
public final class FertilizerExperienceHistorySheet {
    private FertilizerExperienceHistorySheet() { }

    public static void show(
            Context context,
            GardenZone zone,
            FertilizerAdvice.Experience experience,
            List<FertilizerApplication> history
    ) {
        if (context == null || zone == null || experience == null
                || !experience.isAvailable()) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View root = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_fertilizer_experience_history,
                null,
                false
        );
        dialog.setContentView(root);

        TextView subtitle = root.findViewById(R.id.txtExperienceHistorySubtitle);
        subtitle.setText(context.getString(
                R.string.fertilizer_experience_history_subtitle,
                safe(zone.getName()),
                experience.getProductName()
        ));

        TextView summary = root.findViewById(R.id.txtExperienceHistorySummary);
        summary.setText(FertilizerExperiencePresenter.summary(context, experience));

        long now = Instant.now().getEpochSecond();
        FertilizerExperiencePatternAdvisor.Result pattern =
                FertilizerExperiencePatternAdvisor.evaluate(
                        zone.getZone_id(),
                        experience.getProductId(),
                        experience.getProductName(),
                        history,
                        now
                );
        bindPattern(context, root, pattern);

        LinearLayout entries = root.findViewById(R.id.layoutExperienceHistoryEntries);
        TextView empty = root.findViewById(R.id.txtExperienceHistoryEmpty);
        List<FertilizerApplication> matches =
                FertilizerPerformanceAdvisor.matchingOutcomes(
                        zone.getZone_id(),
                        experience.getProductId(),
                        experience.getProductName(),
                        history,
                        now
                );

        empty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(context);
        for (FertilizerApplication application : matches) {
            View item = inflater.inflate(
                    R.layout.item_fertilizer_experience_history,
                    entries,
                    false
            );
            bindItem(context, item, application);
            entries.addView(item);
        }

        root.findViewById(R.id.btnCloseExperienceHistory)
                .setOnClickListener(ignored -> dialog.dismiss());
        dialog.setDismissWithAnimation(true);
        dialog.setOnShowListener(ignored -> {
            FrameLayout sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (sheet == null) return;
            sheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<FrameLayout> behavior =
                    BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });
        dialog.show();
    }

    private static void bindPattern(
            Context context,
            View root,
            FertilizerExperiencePatternAdvisor.Result result
    ) {
        TextView value = root.findViewById(R.id.txtExperiencePattern);
        TextView explanation = root.findViewById(
                R.id.txtExperiencePatternExplanation
        );
        StringBuilder contextText = new StringBuilder();
        int color = R.color.primary;

        if (!result.isAvailable()) {
            value.setText(R.string.fertilizer_experience_pattern_insufficient);
            appendLine(contextText, context.getString(
                    R.string.fertilizer_experience_pattern_principle
            ));
        } else {
            FertilizerExperiencePatternAdvisor.Pattern pattern =
                    result.getBestPattern();
            value.setText(context.getString(
                    R.string.fertilizer_experience_pattern_summary,
                    formatNumber(pattern.getDose()),
                    pattern.getUnit(),
                    pattern.getMethod(),
                    pattern.getObservations(),
                    pattern.getSuccessScore()
            ));

            if (pattern.getSuccessScore() < 40) {
                appendLine(contextText, context.getString(
                        R.string.fertilizer_experience_pattern_caution
                ));
                color = R.color.error;
            } else if (result.isComparative()) {
                appendLine(contextText, context.getString(
                        R.string.fertilizer_experience_pattern_comparative
                ));
                color = R.color.success;
            } else if (pattern.getSuccessScore() >= 70) {
                appendLine(contextText, context.getString(
                        R.string.fertilizer_experience_pattern_positive
                ));
                color = R.color.success;
            } else {
                appendLine(contextText, context.getString(
                        R.string.fertilizer_experience_pattern_balanced
                ));
                color = R.color.warning;
            }
        }

        if (result.getExcludedMixedCount() > 0) {
            appendLine(contextText, context.getString(
                    R.string.fertilizer_experience_pattern_mixed_excluded,
                    result.getExcludedMixedCount()
            ));
        }
        appendLine(contextText, context.getString(
                R.string.fertilizer_experience_pattern_safety
        ));

        value.setTextColor(ContextCompat.getColor(context, color));
        explanation.setText(contextText);
    }

    private static void bindItem(
            Context context,
            View item,
            FertilizerApplication application
    ) {
        TextView status = item.findViewById(R.id.txtExperienceOutcomeStatus);
        int color;
        switch (safe(application.getOutcome_status()).toUpperCase(Locale.ROOT)) {
            case "IMPROVED":
                status.setText(R.string.fertilizer_experience_outcome_improved);
                color = R.color.success;
                break;
            case "ISSUE":
                status.setText(R.string.fertilizer_experience_outcome_issue);
                color = R.color.error;
                break;
            default:
                status.setText(R.string.fertilizer_experience_outcome_unchanged);
                color = R.color.warning;
                break;
        }
        status.setTextColor(ContextCompat.getColor(context, color));

        TextView dates = item.findViewById(R.id.txtExperienceOutcomeDates);
        dates.setText(context.getString(
                R.string.fertilizer_experience_dates,
                formatDate(application.getApplied_at_epoch()),
                formatDate(application.getOutcome_observed_at_epoch())
        ));

        StringBuilder details = new StringBuilder();
        int vigor = application.getOutcome_vigor_score();
        if (vigor >= 1 && vigor <= 5) {
            appendLine(details, context.getString(
                    R.string.fertilizer_experience_vigor,
                    vigor
            ));
        }
        if (application.getApplied_dose() > 0.0) {
            appendLine(details, context.getString(
                    R.string.fertilizer_experience_dose,
                    formatNumber(application.getApplied_dose()),
                    safe(application.getDose_unit())
            ));
        }
        if (!safe(application.getApplication_method()).isBlank()) {
            appendLine(details, context.getString(
                    R.string.fertilizer_experience_method,
                    application.getApplication_method()
            ));
        }
        if (!safe(application.getMix_partner_product_name()).isBlank()) {
            appendLine(details, context.getString(
                    R.string.fertilizer_experience_mix,
                    application.getMix_partner_product_name()
            ));
        }
        TextView detailText = item.findViewById(
                R.id.txtExperienceOutcomeDetails
        );
        detailText.setText(details);
        detailText.setVisibility(details.length() == 0 ? View.GONE : View.VISIBLE);

        TextView notes = item.findViewById(R.id.txtExperienceOutcomeNotes);
        String note = safe(application.getOutcome_notes());
        notes.setText(context.getString(
                R.string.fertilizer_experience_note,
                note
        ));
        notes.setVisibility(note.isBlank() ? View.GONE : View.VISIBLE);
    }

    private static void appendLine(StringBuilder builder, String value) {
        if (builder.length() > 0) builder.append('\n');
        builder.append(value);
    }

    private static String formatDate(long epochSeconds) {
        if (epochSeconds <= 0L) return "—";
        return new SimpleDateFormat(
                "dd-MM-yyyy HH:mm",
                Locale.getDefault()
        ).format(new Date(epochSeconds * 1000L));
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
