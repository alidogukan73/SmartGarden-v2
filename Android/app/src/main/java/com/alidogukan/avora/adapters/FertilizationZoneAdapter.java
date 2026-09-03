package com.alidogukan.avora.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.GardenZone;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FertilizationZoneAdapter extends RecyclerView.Adapter<
        FertilizationZoneAdapter.ZoneViewHolder> {

    private final List<GardenZone> zones = new ArrayList<>();
    private OnZoneClickListener onZoneClickListener;

    public interface OnZoneClickListener {
        void onZoneClick(GardenZone zone);
    }

    public void setOnZoneClickListener(
            OnZoneClickListener listener
    ) {
        onZoneClickListener = listener;
    }

    public void submitList(List<GardenZone> value) {
        int previousCount = zones.size();
        zones.clear();
        if (previousCount > 0) {
            notifyItemRangeRemoved(0, previousCount);
        }
        if (value != null) {
            zones.addAll(value);
        }
        if (!zones.isEmpty()) {
            notifyItemRangeInserted(0, zones.size());
        }
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_fertilization_zone,
                parent,
                false
        );
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ZoneViewHolder holder,
            int position
    ) {
        GardenZone zone = zones.get(position);
        holder.bind(zone);
        holder.itemView.setOnClickListener(
                view -> {
                    if (onZoneClickListener != null) {
                        onZoneClickListener.onZoneClick(zone);
                    }
                }
        );
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    static class ZoneViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtZoneName;
        private final TextView txtPlanStatus;
        private final TextView txtGrowthStage;
        private final TextView txtNextApplication;
        private final TextView txtSetupHint;
        private final MaterialCardView card;

        ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            txtZoneName = itemView.findViewById(R.id.txtZoneName);
            txtPlanStatus = itemView.findViewById(R.id.txtPlanStatus);
            txtGrowthStage = itemView.findViewById(R.id.txtGrowthStage);
            txtNextApplication = itemView.findViewById(
                    R.id.txtNextApplication
            );
            txtSetupHint = itemView.findViewById(R.id.txtSetupHint);
            card = (MaterialCardView) itemView;
        }

        void bind(GardenZone zone) {
            String emoji = zone.getEmoji();
            if (emoji == null || emoji.isBlank()) {
                emoji = "🌱";
            }
            txtZoneName.setText(itemView.getContext().getString(
                    R.string.runtime_icon_label,
                    emoji,
                    com.alidogukan.avora.zones.PhysicalZoneIdentity.name(zone)));

            FertilizationProfile profile = zone.getFertilization();
            boolean configured = profile != null && profile.isEnabled();

            txtPlanStatus.setText(
                    configured
                            ? R.string.fertilization_plan_active
                            : R.string.fertilization_plan_waiting
            );
            txtPlanStatus.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            configured ? R.color.primary : R.color.warning
                    )
            );
            card.setStrokeColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            R.color.border
                    )
            );
            txtNextApplication.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            R.color.textSecondary
                    )
            );

            String stage = configured
                    ? growthStageLabel(profile.getGrowth_stage())
                    : itemView.getContext().getString(
                            R.string.fertilization_not_set
                    );
            txtGrowthStage.setText(
                    itemView.getContext().getString(
                            R.string.fertilization_growth_stage,
                            stage
                    )
            );

            if (
                    configured
                            && profile.getNext_application_at_epoch() > 0L
            ) {
                String date = new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(
                        new Date(
                                profile.getNext_application_at_epoch()
                                        * 1000L
                        )
                );
                txtNextApplication.setText(
                        itemView.getContext().getString(
                                R.string.fertilization_next_application,
                                date
                        )
                );
                applyDueStatus(
                        profile.getNext_application_at_epoch()
                );
            } else {
                txtNextApplication.setText(
                        R.string.fertilization_next_not_planned
                );
            }

            txtSetupHint.setVisibility(
                    configured ? View.GONE : View.VISIBLE
            );
        }

        private void applyDueStatus(long epochSeconds) {
            LocalDate due = Instant.ofEpochSecond(epochSeconds)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long days = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    due
            );
            int color;
            if (days < 0L) {
                txtPlanStatus.setText(
                        R.string.fertilization_status_overdue
                );
                txtNextApplication.setText(
                        itemView.getContext().getString(
                                R.string.fertilization_due_overdue,
                                Math.abs(days)
                        )
                );
                color = R.color.offline;
            } else if (days == 0L) {
                txtPlanStatus.setText(
                        R.string.fertilization_status_today
                );
                txtNextApplication.setText(
                        R.string.fertilization_due_today
                );
                color = R.color.info;
            } else if (days <= 7L) {
                txtPlanStatus.setText(
                        R.string.fertilization_status_upcoming
                );
                txtNextApplication.setText(
                        itemView.getContext().getString(
                                R.string.fertilization_due_in_days,
                                days
                        )
                );
                color = R.color.warning;
            } else {
                return;
            }
            txtPlanStatus.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            color
                    )
            );
            txtNextApplication.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            color
                    )
            );
            card.setStrokeColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            color
                    )
            );
        }

        private String growthStageLabel(String stage) {
            if (stage == null) {
                return itemView.getContext().getString(
                        R.string.fertilization_not_set
                );
            }
            switch (stage) {
                case "ROOTING":
                    return itemView.getContext().getString(
                            R.string.growth_stage_rooting
                    );
                case "VEGETATIVE":
                    return itemView.getContext().getString(
                            R.string.growth_stage_vegetative
                    );
                case "FLOWERING":
                    return itemView.getContext().getString(
                            R.string.growth_stage_flowering
                    );
                case "FRUITING":
                    return itemView.getContext().getString(
                            R.string.growth_stage_fruiting
                    );

                case "HARVEST":
                    return itemView.getContext().getString(
                            R.string.growth_stage_harvest
                    );
                case "SEASON_END":
                    return itemView.getContext().getString(
                            R.string.growth_stage_season_end
                    );
                default:
                    return itemView.getContext().getString(
                            R.string.fertilization_not_set
                    );
            }
        }
    }
}
