package com.ali.smartgarden.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.DecisionStep;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DecisionStepAdapter
        extends RecyclerView.Adapter<DecisionStepAdapter.DecisionStepViewHolder> {

    private final List<DecisionStep> decisionSteps = new ArrayList<>();

    @NonNull
    @Override
    public DecisionStepViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_decision_step, parent, false);

        return new DecisionStepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DecisionStepViewHolder holder,
            int position
    ) {
        DecisionStep step = decisionSteps.get(position);
        holder.bind(step);
    }

    @Override
    public int getItemCount() {
        return decisionSteps.size();
    }

    /**
     * RecyclerView içeriğini yalnız gerçekten değişen satırlarda yeniler.
     *
     * Önceki uygulama her Firebase güncellemesinde bütün satırları kaldırıp
     * yeniden ekliyordu. Bu davranış RecyclerView geçiş animasyonlarını
     * tetiklediği için AI Karar Akışı kartı yanıp sönüyormuş gibi görünüyordu.
     */
    public void submitList(List<DecisionStep> newSteps) {
        List<DecisionStep> safeSteps =
                newSteps == null ? new ArrayList<>() : new ArrayList<>(newSteps);

        if (decisionSteps.size() != safeSteps.size()) {
            decisionSteps.clear();
            decisionSteps.addAll(safeSteps);
            notifyDataSetChanged();
            return;
        }

        for (int index = 0; index < safeSteps.size(); index++) {
            DecisionStep current = decisionSteps.get(index);
            DecisionStep updated = safeSteps.get(index);

            if (!hasSameContent(current, updated)) {
                decisionSteps.set(index, updated);
                notifyItemChanged(index);
            }
        }
    }

    private boolean hasSameContent(DecisionStep first, DecisionStep second) {
        return first.getStepNumber() == second.getStepNumber()
                && first.getIconResource() == second.getIconResource()
                && Objects.equals(first.getTitle(), second.getTitle())
                && Objects.equals(first.getDescription(), second.getDescription())
                && Objects.equals(first.getBadgeText(), second.getBadgeText())
                && first.getStatus() == second.getStatus()
                && first.isShowBottomLine() == second.isShowBottomLine();
    }

    static class DecisionStepViewHolder extends RecyclerView.ViewHolder {

        private final android.widget.TextView txtStepNumber;
        private final View layoutStepCheck;
        private final AppCompatImageView imgStepCheck;
        private final View viewStepLine;

        private final AppCompatImageView imgStepIcon;
        private final android.widget.TextView txtStepTitle;
        private final android.widget.TextView txtStepDescription;
        private final android.widget.TextView txtStepBadge;

        private final MaterialCardView stepCard;

        DecisionStepViewHolder(@NonNull View itemView) {
            super(itemView);

            txtStepNumber =
                    itemView.findViewById(R.id.txtDecisionStepNumber);

            layoutStepCheck =
                    itemView.findViewById(R.id.layoutDecisionStepCheck);

            imgStepCheck =
                    itemView.findViewById(R.id.imgDecisionStepCheck);

            viewStepLine =
                    itemView.findViewById(R.id.viewDecisionStepLine);

            imgStepIcon =
                    itemView.findViewById(R.id.imgDecisionStepIcon);

            txtStepTitle =
                    itemView.findViewById(R.id.txtDecisionStepTitle);

            txtStepDescription =
                    itemView.findViewById(R.id.txtDecisionStepDescription);

            txtStepBadge =
                    itemView.findViewById(R.id.txtDecisionStepBadge);

            stepCard =
                    itemView.findViewById(R.id.cardDecisionStep);
        }

        void bind(DecisionStep step) {
            txtStepNumber.setText(String.valueOf(step.getStepNumber()));
            imgStepIcon.setImageResource(step.getIconResource());

            txtStepTitle.setText(step.getTitle());
            txtStepDescription.setText(step.getDescription());
            txtStepBadge.setText(step.getBadgeText());

            viewStepLine.setVisibility(
                    step.isShowBottomLine() ? View.VISIBLE : View.INVISIBLE
            );

            applyStatusStyle(step.getStatus());
        }

        private void applyStatusStyle(DecisionStep.Status status) {
            int primary =
                    ContextCompat.getColor(itemView.getContext(), R.color.primary);

            int primaryDark =
                    ContextCompat.getColor(itemView.getContext(), R.color.primaryDark);

            switch (status) {

                case COMPLETED:
                    layoutStepCheck.setVisibility(View.VISIBLE);
                    imgStepCheck.setImageResource(R.drawable.ic_ai_check_18);

                    imgStepCheck.setImageTintList(
                            ColorStateList.valueOf(primary)
                    );

                    txtStepBadge.setBackgroundResource(
                            R.drawable.bg_ai_badge_completed
                    );

                    txtStepBadge.setTextColor(primaryDark);
                    break;

                case ANALYZING:
                    layoutStepCheck.setVisibility(View.VISIBLE);
                    imgStepCheck.setImageResource(R.drawable.ic_ai_moisture_24);

                    imgStepCheck.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                    );

                    txtStepBadge.setBackgroundResource(
                            R.drawable.bg_ai_badge_analyzing
                    );

                    txtStepBadge.setTextColor(
                            Color.parseColor("#B45309")
                    );
                    break;

                case LEARNING:
                    layoutStepCheck.setVisibility(View.VISIBLE);
                    imgStepCheck.setImageResource(R.drawable.ic_ai_brain_24);

                    imgStepCheck.setImageTintList(
                            ColorStateList.valueOf(Color.parseColor("#7C3AED"))
                    );

                    txtStepBadge.setBackgroundResource(
                            R.drawable.bg_ai_badge_learning
                    );

                    txtStepBadge.setTextColor(
                            Color.parseColor("#6D28D9")
                    );
                    break;

                case RESULT:
                    layoutStepCheck.setVisibility(View.VISIBLE);
                    imgStepCheck.setImageResource(R.drawable.ic_ai_check_18);

                    imgStepCheck.setImageTintList(
                            ColorStateList.valueOf(primary)
                    );

                    txtStepBadge.setBackgroundResource(
                            R.drawable.bg_ai_badge_completed
                    );

                    txtStepBadge.setTextColor(primaryDark);
                    break;

                case WAITING:
                default:

                    layoutStepCheck.setVisibility(View.VISIBLE);

                    imgStepCheck.setImageResource(
                            R.drawable.ic_ai_waiting_18
                    );

                    imgStepCheck.setImageTintList(
                            null
                    );

                    txtStepBadge.setBackgroundResource(
                            R.drawable.bg_ai_badge_waiting
                    );

                    txtStepBadge.setTextColor(
                            Color.parseColor("#64748B")
                    );

                    break;
            }
        }
    }
}
