package com.alidogukan.avora.fertilization;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.FertilizerApplication;
import com.alidogukan.avora.models.GardenZone;

import java.util.List;

/** Renders the learned outcome history for the current zone and top product. */
public final class FertilizerExperiencePresenter {
    private FertilizerExperiencePresenter() { }

    public static void bind(
            Context context,
            View container,
            TextView text,
            FertilizerAdvice.Experience experience
    ) {
        bind(context, container, text, experience, null, null);
    }

    public static void bind(
            Context context,
            View container,
            TextView text,
            FertilizerAdvice.Experience experience,
            GardenZone zone,
            List<FertilizerApplication> history
    ) {
        if (container == null || text == null || experience == null
                || !experience.isAvailable()) {
            if (container != null) container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        text.setText(summary(context, experience));

        boolean hasDetails = zone != null;
        container.setClickable(hasDetails);
        container.setFocusable(hasDetails);
        container.setOnClickListener(hasDetails
                ? ignored -> FertilizerExperienceHistorySheet.show(
                        context,
                        zone,
                        experience,
                        history
                )
                : null
        );
    }

    static String summary(
            Context context,
            FertilizerAdvice.Experience experience
    ) {
        int observations = experience.getObservations();
        if (observations <= 0) {
            return context.getString(
                    R.string.fertilizer_past_zone_experience_none,
                    experience.getProductName()
            );
        }
        if (!experience.isReliable()) {
            int remaining = Math.max(
                    0,
                    experience.getRequiredObservations() - observations
            );
            return context.getString(
                    R.string.fertilizer_past_zone_experience_learning,
                    experience.getProductName(),
                    observations,
                    experience.getRequiredObservations(),
                    remaining
            );
        }
        int score = experience.getSuccessScore();
        int label = score >= 70
                ? R.string.fertilizer_past_zone_experience_positive
                : score < 40
                ? R.string.fertilizer_past_zone_experience_caution
                : R.string.fertilizer_past_zone_experience_balanced;
        return context.getString(
                R.string.fertilizer_past_zone_experience_reliable,
                experience.getProductName(),
                observations,
                score,
                context.getString(label)
        );
    }
}
