package com.ali.smartgarden.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.models.FertilizerApplication;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FertilizerHistoryAdapter extends RecyclerView.Adapter<
        FertilizerHistoryAdapter.Holder> {

    public interface OnApplicationClickListener {
        void onApplicationClick(FertilizerApplication application);
    }
    private OnApplicationClickListener onApplicationClickListener;

    public void setOnApplicationClickListener(
            OnApplicationClickListener listener
    ) {
        onApplicationClickListener = listener;
    }

    private final List<FertilizerApplication> values =
            new ArrayList<>();

    public void submitList(List<FertilizerApplication> items) {
        values.clear();
        if (items != null) {
            values.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new Holder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_fertilizer_application,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {
        FertilizerApplication value = values.get(position);
        holder.product.setText(value.getProduct_name());
        holder.product.setTextColor(
                holder.itemView.getContext().getColor(
                        applicationTypeColor(value.getApplication_type())
                )
        );
        holder.zone.setText(
                value.getZone_name() + " · "
                        + applicationTypeLabel(
                                holder.itemView,
                                value.getApplication_type()
                        )
        );
        holder.dose.setText(
                holder.itemView.getContext().getString(
                        R.string.fertilizer_history_dose,
                        formatDose(value.getApplied_dose()),
                        value.getDose_unit()
                )
        );
        if (value.getArea_m2() > 0.0) {
            holder.calculationBasis.setText(
                    holder.itemView.getContext().getString(
                            R.string.fertilizer_history_area,
                            formatDose(value.getArea_m2())
                    )
            );
            holder.calculationBasis.setVisibility(View.VISIBLE);
        } else if (value.getTank_liters() > 0.0) {
            holder.calculationBasis.setText(
                    holder.itemView.getContext().getString(
                            R.string.fertilizer_history_tank,
                            formatDose(value.getTank_liters())
                    )
            );
            holder.calculationBasis.setVisibility(View.VISIBLE);
        } else {
            holder.calculationBasis.setVisibility(View.GONE);
        }
        String method = value.getApplication_method();
        if (method == null || method.isBlank()) {
            holder.method.setVisibility(View.GONE);
        } else {
            int methodLabel = "FOLIAR".equals(method)
                    ? R.string.fertilization_method_foliar
                    : "SOIL".equals(method)
                    ? R.string.fertilization_method_soil
                    : R.string.fertilization_method_drip;
            holder.method.setText(
                    holder.itemView.getContext().getString(
                            R.string.fertilizer_history_method,
                            holder.itemView.getContext().getString(
                                    methodLabel
                            )
                    )
            );
            holder.method.setVisibility(View.VISIBLE);
        }
        String notes = value.getNotes();
        if (notes == null || notes.isBlank()) {
            holder.note.setVisibility(View.GONE);
        } else {
            holder.note.setText(
                    holder.itemView.getContext().getString(
                            R.string.fertilizer_history_note,
                            notes
                    )
            );
            holder.note.setVisibility(View.VISIBLE);
        }
        String outcome = outcomeLabel(value);
        if (outcome.isEmpty()) {
            int evaluated = FertilizerOutcomeFollowUpPolicy.evaluatedCount(
                    values, value.getZone_id(), value.getProduct_id()
            );
            int target = FertilizerOutcomeFollowUpPolicy.RELIABLE_OBSERVATION_COUNT;
            holder.outcome.setText(evaluated >= target
                    ? holder.itemView.getContext().getString(
                    R.string.fertilizer_outcome_learning_ready, evaluated)
                    : holder.itemView.getContext().getString(
                    R.string.fertilizer_outcome_learning_progress, evaluated, target));
        } else {
            holder.outcome.setText(outcome);
        }
        holder.outcome.setVisibility(View.VISIBLE);
        holder.date.setText(
                Instant.ofEpochSecond(value.getApplied_at_epoch())
                        .atZone(ZoneId.systemDefault())
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd-MM-yyyy · HH:mm",
                                        new Locale("tr", "TR")
                                )
                        )
        );
        holder.itemView.setOnClickListener(view -> {
            if (onApplicationClickListener != null) {
                onApplicationClickListener.onApplicationClick(value);
            }
        });
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    private static String formatDose(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.2f", value);
    }

    private static String applicationTypeLabel(
            View view,
            String type
    ) {
        if ("ORGANIC".equals(type)) {
            return view.getContext().getString(
                    R.string.fertilizer_type_organic
            );
        }
        if ("CONDITIONER".equals(type)) {
            return view.getContext().getString(
                    R.string.fertilizer_type_conditioner
            );
        }
        if ("BIOSTIMULANT".equals(type)) {
            return view.getContext().getString(
                    R.string.fertilizer_type_biostimulant
            );
        }
        return view.getContext().getString(
                R.string.fertilizer_type_nutrition
        );
    }

    private static int applicationTypeColor(String type) {
        if ("ORGANIC".equals(type)) return R.color.organic;
        if ("CONDITIONER".equals(type)) return R.color.conditioner;
        if ("BIOSTIMULANT".equals(type)) return R.color.microbial;
        return R.color.info;
    }

    private static String outcomeLabel(FertilizerApplication value) {
        String status = value.getOutcome_status();
        if (status == null || status.isBlank()) {
            return "";
        }
        String label = "IMPROVED".equals(status)
                ? "Sonuç: İyileşme gözlendi"
                : "UNCHANGED".equals(status)
                ? "Sonuç: Belirgin değişiklik yok"
                : "Sonuç: Sorun gözlendi";
        if (value.getOutcome_vigor_score() > 0) {
            label += " · Canlılık "
                    + value.getOutcome_vigor_score() + "/5";
        }
        return label;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView product;
        final TextView zone;
        final TextView dose;
        final TextView calculationBasis;
        final TextView method;
        final TextView note;
        final TextView outcome;
        final TextView date;

        Holder(@NonNull View itemView) {
            super(itemView);
            product = itemView.findViewById(R.id.txtHistoryProduct);
            zone = itemView.findViewById(R.id.txtHistoryZone);
            dose = itemView.findViewById(R.id.txtHistoryDose);
            calculationBasis = itemView.findViewById(
                    R.id.txtHistoryCalculationBasis
            );
            method = itemView.findViewById(R.id.txtHistoryMethod);
            note = itemView.findViewById(R.id.txtHistoryNote);
            outcome = itemView.findViewById(R.id.txtHistoryOutcome);
            date = itemView.findViewById(R.id.txtHistoryDate);
        }
    }
}
