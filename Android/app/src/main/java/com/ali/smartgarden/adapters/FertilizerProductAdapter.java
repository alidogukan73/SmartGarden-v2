package com.ali.smartgarden.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.fertilization.FertilizerStagePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;

public class FertilizerProductAdapter extends RecyclerView.Adapter<
        FertilizerProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(FertilizerProduct product);
    }

    private final List<FertilizerProduct> products =
            new ArrayList<>();
    private OnProductClickListener listener;

    public void submitList(List<FertilizerProduct> value) {
        List<FertilizerProduct> sortedProducts = value == null
                ? new ArrayList<>()
                : new ArrayList<>(value);
        sortedProducts.sort(Comparator.comparing(
                product -> categoryCode(product) + product.getName(),
                String.CASE_INSENSITIVE_ORDER
        ));
        int previousCount = products.size();
        products.clear();
        if (previousCount > 0) {
            notifyItemRangeRemoved(0, previousCount);
        }
        products.addAll(sortedProducts);
        if (!products.isEmpty()) {
            notifyItemRangeInserted(0, products.size());
        }
    }

    public void setOnProductClickListener(
            OnProductClickListener value
    ) {
        listener = value;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new ProductViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_fertilizer_product,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position
    ) {
        FertilizerProduct product = products.get(position);
        String category = categoryCode(product);
        holder.category.setVisibility(View.VISIBLE);
        holder.category.setText(categoryLabel(
                holder.itemView, category
        ));
        int color = categoryColor(category);
        int background = categoryBackgroundColor(category);
        holder.category.setTextColor(
                holder.itemView.getContext().getColor(color)
        );
        holder.category.setBackgroundColor(
                holder.itemView.getContext().getColor(background)
        );
        holder.name.setText(product.getName());
        holder.inactive.setVisibility(
                product.isEnabled() ? View.GONE : View.VISIBLE
        );
        holder.info.setText(
                holder.itemView.getContext().getString(
                        R.string.fertilizer_product_info,
                        formLabel(holder.itemView.getContext(), product.getForm()),
                        emptyFallback(holder.itemView.getContext(), product.getNpk())
                )
        );
        String functionLabel = functionalTagLabel(holder.itemView, product);
        String organicLabel = product.isOrganic_farming_eligible()
                ? holder.itemView.getContext().getString(
                        R.string.fertilizer_organic_eligible_badge)
                : "";
        if (functionLabel.isBlank() && organicLabel.isBlank()) {
            holder.traits.setVisibility(View.GONE);
        } else {
            holder.traits.setVisibility(View.VISIBLE);
            holder.traits.setText(functionLabel.isBlank()
                    ? organicLabel
                    : organicLabel.isBlank()
                    ? functionLabel
                    : holder.itemView.getContext().getString(
                            R.string.fertilizer_traits_format,
                            functionLabel, organicLabel));
        }
        boolean hasRange = product.getLabel_dosage_min() > 0
                && product.getLabel_dosage_max()
                > product.getLabel_dosage_min();
        holder.dose.setText(
                hasRange
                        ? holder.itemView.getContext().getString(
                                R.string.fertilizer_product_dose_range,
                                formatDose(product.getLabel_dosage_min()),
                                formatDose(product.getLabel_dosage_max()),
                                product.getDosage_unit(),
                                product.getMinimum_interval_days()
                        )
                        : holder.itemView.getContext().getString(
                                R.string.fertilizer_product_dose,
                                formatDose(product.getLabel_dosage()),
                                product.getDosage_unit(),
                                product.getMinimum_interval_days()
                )
        );
        holder.dose.setTextColor(
                holder.itemView.getContext().getColor(
                        categoryColor(category)
                )
        );
        List<String> stages = FertilizerStagePolicy.effectiveStages(product);
        holder.stages.setText(stageSummary(holder.itemView, stages));
        holder.stages.setTextColor(holder.itemView.getContext().getColor(
                stages.isEmpty() ? R.color.warning : categoryColor(category)
        ));
        boolean stockKnown = product.getStock_unit() != null
                && !product.getStock_unit().isBlank();
        boolean lowStock = stockKnown
                && product.getLow_stock_threshold() > 0.0
                && product.getStock_amount()
                <= product.getLow_stock_threshold();
        holder.stock.setText(
                !stockKnown
                        ? holder.itemView.getContext().getString(
                                R.string.fertilizer_stock_unknown
                        )
                        : holder.itemView.getContext().getString(
                                lowStock
                                        ? R.string.fertilizer_stock_low
                                        : R.string
                                        .fertilizer_stock_available,
                                formatDose(product.getStock_amount()),
                                product.getStock_unit()
                        )
        );
        holder.stock.setTextColor(
                holder.itemView.getContext().getColor(
                        lowStock ? R.color.warning : categoryColor(category)
                )
        );
        holder.itemView.setOnClickListener(
                view -> {
                    if (listener != null) {
                        listener.onProductClick(product);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private static String functionalTagLabel(View view, FertilizerProduct product) {
        List<String> tags = product.getFunctional_tags();
        if (tags == null || tags.isEmpty() || tags.get(0) == null) return "";
        int resource;
        switch (tags.get(0)) {
            case "TRACE_ELEMENTS":
                resource = R.string.fertilizer_function_trace_elements;
                break;
            case "ORGANIC_MATTER":
                resource = R.string.fertilizer_function_organic_matter;
                break;
            case "HUMIC_FULVIC":
                resource = R.string.fertilizer_function_humic_fulvic;
                break;
            case "SEAWEED":
                resource = R.string.fertilizer_function_seaweed;
                break;
            case "CALCIUM_MAGNESIUM":
                resource = R.string.fertilizer_function_calcium_magnesium;
                break;
            case "AMINO_ACIDS":
                resource = R.string.fertilizer_function_amino_acids;
                break;
            default:
                resource = R.string.fertilizer_function_general;
        }
        return view.getContext().getString(resource);
    }

    private static String stageSummary(View view, List<String> stages) {
        if (stages == null || stages.isEmpty()) {
            return view.getContext().getString(R.string.fertilizer_stage_missing);
        }
        List<String> labels = new ArrayList<>();
        for (String stage : stages) {
            int label = FertilizerStagePolicy.SOIL_PREPARATION.equals(stage)
                    ? R.string.fertilizer_stage_soil_preparation
                    : FertilizerStagePolicy.ROOTING.equals(stage)
                    ? R.string.growth_stage_rooting
                    : FertilizerStagePolicy.VEGETATIVE.equals(stage)
                    ? R.string.growth_stage_vegetative
                    : FertilizerStagePolicy.FLOWERING.equals(stage)
                    ? R.string.growth_stage_flowering
                    : FertilizerStagePolicy.FRUITING.equals(stage)
                    ? R.string.growth_stage_fruiting
                    : R.string.growth_stage_harvest;
            labels.add(view.getContext().getString(label));
        }
        return view.getContext().getString(
                R.string.fertilizer_stages_summary,
                String.join(", ", labels)
        );
    }
    private static String formLabel(android.content.Context context, String form) {
        if ("GRANULAR".equals(form)) {
            return context.getString(R.string.runtime_form_granular);
        }
        return context.getString("POWDER".equals(form) ? R.string.runtime_form_powder : R.string.runtime_form_liquid);
    }

    private static String emptyFallback(android.content.Context context, String value) {
        return value == null || value.isBlank()
                ? context.getString(R.string.runtime_npk_missing)
                : "NPK " + value;
    }

    private static String formatDose(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.3f", value)
                .replaceAll("0+$", "")
                .replaceAll("[,.]$", "");
    }

    private static String categoryCode(FertilizerProduct product) {
        String type = product.getApplication_type();
        if (type != null && !type.isBlank()) {
            return type;
        }
        String text = (product.getName() + " " + product.getNpk())
                .toLowerCase(Locale.ROOT);
        if (text.contains("humik") || text.contains("hümik")
                || text.contains("fulvik")
                || text.contains("leonardit")) return "CONDITIONER";
        if (text.contains("mikrobiyal") || text.contains("deniz yosunu"))
            return "BIOSTIMULANT";
        if (text.contains("organik") || text.contains("kompost"))
            return "ORGANIC";
        return "NUTRITION";
    }

    private static String categoryLabel(View view, String type) {
        int id = "ORGANIC".equals(type) ? R.string.fertilizer_type_organic
                : "CONDITIONER".equals(type)
                ? R.string.fertilizer_type_conditioner
                : "BIOSTIMULANT".equals(type)
                ? R.string.fertilizer_type_biostimulant
                : R.string.fertilizer_type_nutrition;
        String icon = "ORGANIC".equals(type) ? "🍂 "
                : "CONDITIONER".equals(type) ? "🪨 "
                : "BIOSTIMULANT".equals(type) ? "🦠 " : "🌱 ";
        return icon + view.getContext().getString(id);
    }

    private static int categoryColor(String type) {
        if ("ORGANIC".equals(type)) return R.color.organic;
        if ("CONDITIONER".equals(type)) return R.color.conditioner;
        if ("BIOSTIMULANT".equals(type)) return R.color.microbial;
        return R.color.info;
    }

    private static int categoryBackgroundColor(String type) {
        if ("ORGANIC".equals(type)) return R.color.organicBackground;
        if ("CONDITIONER".equals(type))
            return R.color.conditionerBackground;
        if ("BIOSTIMULANT".equals(type))
            return R.color.microbialBackground;
        return R.color.infoBackground;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView category;
        final TextView info;
        final TextView traits;
        final TextView inactive;
        final TextView dose;
        final TextView stages;
        final TextView stock;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtProductName);
            category = itemView.findViewById(R.id.txtProductCategory);
            info = itemView.findViewById(R.id.txtProductInfo);
            traits = itemView.findViewById(R.id.txtProductTraits);
            inactive = itemView.findViewById(R.id.txtProductInactive);
            dose = itemView.findViewById(R.id.txtProductDose);
            stages = itemView.findViewById(R.id.txtProductStages);
            stock = itemView.findViewById(R.id.txtProductStock);
        }
    }
}
