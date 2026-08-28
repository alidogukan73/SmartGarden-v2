package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces explainable warnings from recorded fertilizer applications.
 * <p>The rules are deliberately conservative. They identify records that
 * deserve review; they never prescribe a fertilizer or start an application.</p>
 */
public final class FertilizerHistoryRiskAdvisor {

    private static final long DAY_SECONDS = 86_400L;
    private static final long FREQUENCY_WINDOW_DAYS = 7L;
    private static final long RECENT_WINDOW_DAYS = 30L;
    private static final long POTASSIUM_REVIEW_DAYS = 21L;

    private FertilizerHistoryRiskAdvisor() { }

    public static List<String> assess(
            GardenZone zone,
            List<FertilizerProduct> products,
            List<FertilizerApplication> history,
            long now
    ) {
        List<String> risks = new ArrayList<>();
        if (zone == null || history == null || history.isEmpty()) {
            addStageGapRisk(risks, zone, products, history, now);
            return risks;
        }

        Map<String, FertilizerProduct> productsById = productMap(products);
        List<FertilizerApplication> zoneHistory = zoneHistory(
                zone.getZone_id(), history, now
        );

        FertilizerApplication issue = newestIssue(zoneHistory);
        if (issue != null) {
            risks.add("Son " + safe(issue.getProduct_name(), "gübre")
                    + " uygulamasında sorun bildirildi. Aynı ürünü tekrarlamadan "
                    + "önce sonucu ve uygulama koşullarını gözden geçirin.");
        }

        FertilizerApplication excessiveDose = newestExcessiveDose(zoneHistory);
        if (excessiveDose != null) {
            risks.add(safe(excessiveDose.getProduct_name(), "Bir ürün")
                    + " üretici üst dozunun üzerinde kaydedildi. Yeni uygulamadan "
                    + "önce etiket, ölçü birimi ve analiz sonucunu doğrulayın.");
        }

        int recentNutritionEvents = uniqueNutritionEvents(
                zoneHistory, FREQUENCY_WINDOW_DAYS, now
        );
        if (recentNutritionEvents >= 3) {
            risks.add("Son 7 günde " + recentNutritionEvents
                    + " ayrı besleme uygulaması kaydedildi. Tuz birikimi ve aşırı "
                    + "besleme riskine karşı yeni uygulamayı analizle doğrulayın.");
        }

        int nitrogenEvents = uniqueNitrogenEvents(
                zoneHistory, productsById, FREQUENCY_WINDOW_DAYS, now
        );
        if (nitrogenEvents >= 3) {
            risks.add("Son 7 günde " + nitrogenEvents
                    + " azot ağırlıklı uygulama kaydedildi. Aşırı azot; yumuşak "
                    + "gelişim ve meyve kalitesi riski oluşturabilir.");
        }

        addStageGapRisk(risks, zone, products, zoneHistory, now);
        return risks;
    }

    private static void addStageGapRisk(
            List<String> risks,
            GardenZone zone,
            List<FertilizerProduct> products,
            List<FertilizerApplication> history,
            long now
    ) {
        if (zone == null || zone.getFertilization() == null
                || !"FRUITING".equals(zone.getFertilization().getGrowth_stage())) {
            return;
        }
        List<FertilizerApplication> values = history == null
                ? new ArrayList<>() : history;
        Map<String, FertilizerProduct> productsById = productMap(products);
        boolean potassiumSeen = false;
        for (FertilizerApplication application : values) {
            if (!sameZone(zone.getZone_id(), application.getZone_id())
                    || !withinDays(application, POTASSIUM_REVIEW_DAYS, now)) {
                continue;
            }
            FertilizerProduct product = productsById.get(application.getProduct_id());
            if (product != null
                    && FertilizerNutrientProfile.from(product).isPotassiumForward()) {
                potassiumSeen = true;
                break;
            }
        }
        if (!potassiumSeen) {
            risks.add("Meyve döneminde son 21 gün içinde potasyum ağırlıklı "
                    + "uygulama kaydı bulunamadı. Gereksinimi ürün etiketi ve "
                    + "toprak/yaprak analiziyle değerlendirin.");
        }
    }

    private static List<FertilizerApplication> zoneHistory(
            String zoneId,
            List<FertilizerApplication> history,
            long now
    ) {
        List<FertilizerApplication> result = new ArrayList<>();
        for (FertilizerApplication application : history) {
            if (application != null
                    && sameZone(zoneId, application.getZone_id())
                    && withinDays(application, RECENT_WINDOW_DAYS, now)) {
                result.add(application);
            }
        }
        result.sort((left, right) -> Long.compare(
                right.getApplied_at_epoch(), left.getApplied_at_epoch()
        ));
        return result;
    }

    private static FertilizerApplication newestIssue(
            List<FertilizerApplication> history
    ) {
        for (FertilizerApplication application : history) {
            if ("ISSUE".equalsIgnoreCase(safe(application.getOutcome_status(), ""))
                    || (application.getOutcome_vigor_score() > 0
                    && application.getOutcome_vigor_score() <= 2)) {
                return application;
            }
        }
        return null;
    }

    private static FertilizerApplication newestExcessiveDose(
            List<FertilizerApplication> history
    ) {
        for (FertilizerApplication application : history) {
            double maximum = application.getRecommended_dose_max();
            if (maximum > 0.0
                    && application.getApplied_dose() > maximum * 1.10) {
                return application;
            }
        }
        return null;
    }

    private static int uniqueNutritionEvents(
            List<FertilizerApplication> history,
            long days,
            long now
    ) {
        Set<String> events = new HashSet<>();
        for (FertilizerApplication application : history) {
            if (withinDays(application, days, now)
                    && "NUTRITION".equalsIgnoreCase(
                    safe(application.getApplication_type(), "NUTRITION"))) {
                events.add(eventKey(application));
            }
        }
        return events.size();
    }

    private static int uniqueNitrogenEvents(
            List<FertilizerApplication> history,
            Map<String, FertilizerProduct> products,
            long days,
            long now
    ) {
        Set<String> events = new HashSet<>();
        for (FertilizerApplication application : history) {
            if (!withinDays(application, days, now)) continue;
            FertilizerProduct product = products.get(application.getProduct_id());
            if (product != null
                    && FertilizerNutrientProfile.from(product).isNitrogenForward()) {
                events.add(eventKey(application));
            }
        }
        return events.size();
    }

    private static String eventKey(FertilizerApplication application) {
        if (!safe(application.getMix_group_id(), "").isBlank()) {
            return "mix:" + application.getMix_group_id();
        }
        // Records created by one bulk/mixture confirmation share the same
        // second. Product id is intentionally excluded so one tank event is
        // not counted twice.
        return "time:" + application.getApplied_at_epoch();
    }

    private static boolean withinDays(
            FertilizerApplication application,
            long days,
            long now
    ) {
        if (application == null || application.getApplied_at_epoch() <= 0L
                || application.getApplied_at_epoch() > now) {
            return false;
        }
        return now - application.getApplied_at_epoch() <= days * DAY_SECONDS;
    }

    private static boolean sameZone(String expected, String actual) {
        return expected != null && !expected.isBlank() && expected.equals(actual);
    }

    private static Map<String, FertilizerProduct> productMap(
            List<FertilizerProduct> products
    ) {
        Map<String, FertilizerProduct> result = new HashMap<>();
        if (products == null) return result;
        for (FertilizerProduct product : products) {
            if (product != null && product.getProduct_id() != null) {
                result.put(product.getProduct_id(), product);
            }
        }
        return result;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
