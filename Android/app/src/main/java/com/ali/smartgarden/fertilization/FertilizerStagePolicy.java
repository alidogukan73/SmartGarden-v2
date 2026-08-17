package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Normalizes and validates fertilizer application periods. */
public final class FertilizerStagePolicy {
    public static final String SOIL_PREPARATION = "SOIL_PREPARATION";
    public static final String ROOTING = "ROOTING";
    public static final String VEGETATIVE = "VEGETATIVE";
    public static final String FLOWERING = "FLOWERING";
    public static final String FRUITING = "FRUITING";
    public static final String HARVEST = "HARVEST";
    public static final String SEASON_END = "SEASON_END";

    private FertilizerStagePolicy() { }

    public static List<String> effectiveStages(FertilizerProduct product) {
        if (product == null) return Collections.emptyList();
        Set<String> normalized = new LinkedHashSet<>();
        List<String> configured = product.getRecommended_stages();
        if (configured != null) {
            for (String stage : configured) {
                String value = normalize(stage);
                if (!value.isEmpty() && !SEASON_END.equals(value)) {
                    normalized.add(value);
                }
            }
        }
        if (!normalized.isEmpty()) return new ArrayList<>(normalized);

        // Verified from the supplied product label: OG Toros is a pre-plant product.
        String name = safe(product.getName()).toLowerCase(new Locale("tr", "TR"));
        if (name.contains("og toros") && name.contains("organik")) {
            return Collections.singletonList(SOIL_PREPARATION);
        }
        return Collections.emptyList();
    }

    public static String normalize(String stage) {
        String value = safe(stage).toUpperCase(Locale.ROOT);
        if ("SOIL_PREP".equals(value) || "PREPARATION".equals(value)
                || "BASE".equals(value)) return SOIL_PREPARATION;
        if ("SEEDLING".equals(value)) return ROOTING;
        if ("FRUIT".equals(value)) return FRUITING;
        if ("ACTIVE_HARVEST".equals(value)) return HARVEST;
        if ("SEASON_CLOSED".equals(value) || "POST_HARVEST".equals(value)) {
            return SEASON_END;
        }
        if (SOIL_PREPARATION.equals(value) || ROOTING.equals(value)
                || VEGETATIVE.equals(value) || FLOWERING.equals(value)
                || FRUITING.equals(value) || HARVEST.equals(value)
                || SEASON_END.equals(value)) return value;
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
