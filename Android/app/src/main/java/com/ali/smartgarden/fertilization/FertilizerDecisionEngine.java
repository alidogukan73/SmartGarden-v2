package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.FertilizerApplicationSchedule;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Safe, explainable advisor. It ranks only products already entered by the user. */
public final class FertilizerDecisionEngine {
    private FertilizerDecisionEngine() { }

    public static FertilizerAdvice advise(GardenZone zone, List<FertilizerProduct> products,
                                           WeatherForecast weather, long now) {
        FertilizationProfile profile = zone.getFertilization();
        String title = safe(zone.getEmoji(), "\uD83C\uDF31") + " " + safe(zone.getName(), "Bölge");
        if (profile == null || !profile.isEnabled() || safe(profile.getGrowth_stage(), "NOT_SET").equals("NOT_SET")) {
            return new FertilizerAdvice(title, "PLAN HAZIR DEĞİL",
                    "Dikim tarihi ve gelişim dönemi girildiğinde kişisel öneri hazırlanır.",
                    "", new ArrayList<>(),
                    FertilizerRiskAdvisor.assess(zone, products, weather, now));
        }
        String context = buildContext(zone, profile, weather, now);
        List<Candidate> candidates = new ArrayList<>();
        for (FertilizerProduct product : products) {
            if (product != null && product.isEnabled()) {
                candidates.add(score(product, profile, weather,
                        remainingDays(profile, product, now)));
            }
        }
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
        List<Candidate> available = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.isAvailable()) available.add(candidate);
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, available.size()); i++) {
            result.add(available.get(i).display());
        }
        if (available.isEmpty()) {
            long nearest = nearestWait(candidates);
            String reason = nearest == Long.MAX_VALUE
                    ? "Bu dönem için etkin ve uygun ürün bulunamadı."
                    : "Ürünlerin kendi bekleme aralıkları sürüyor. En yakın tekrar uygulama "
                    + nearest + " gün sonra değerlendirilebilir.";
            return new FertilizerAdvice(title, "HENÜZ ERKEN", reason, context,
                    new ArrayList<>(),
                    FertilizerRiskAdvisor.assess(zone, products, weather, now));
        }
        String reason = "Bugün uygulama planı değerlendirilebilir.";
        if (zone.hasSensorData() && zone.getMoisture() < zone.getMoisture_limit()) {
            reason += " Toprak nemi düşük; önce sulama ve kök bölgesini kontrol edin.";
        }
        return new FertilizerAdvice(title, "BUGÜNKÜ ÖNERİ", reason, context, result,
                FertilizerRiskAdvisor.assess(zone, products, weather, now));
    }

    private static Candidate score(FertilizerProduct product, FertilizationProfile profile,
                                   WeatherForecast weather, long waitDays) {
        String text = (safe(product.getName(), "") + " " + safe(product.getNpk(), "")).toLowerCase(Locale.ROOT);
        FertilizerNutrientProfile nutrients = FertilizerNutrientProfile.from(product);
        String stage = safe(profile.getGrowth_stage(), "");
        int score = 45; String role = "Destek ürünü";
        if ("FRUITING".equals(stage)) {
            if (nutrients.isPotassiumForward()) { score = 95; role = "Ana potasyum beslemesi"; }
            else if (matches(text, "calsimagsi", "kalsiyum", "magsul")) { score = 84; role = "Kalsiyum / magnezyum desteği"; }
            else if (matches(text, "deniz yosunu", "searius")) { score = 72; role = "Biyostimülant desteği"; }
            else if (matches(text, "fertisol", "15-0-5", "15.0.5")) { score = 58; role = "Mikro element desteği"; }
            else if (matches(text, "20-20-20", "20.20.20")) { score = 48; role = "Dengeli destek; tek başına ana tercih değil"; }
        } else if ("VEGETATIVE".equals(stage)) {
            if (nutrients.isBalanced()) { score = 92; role = "Dengeli ana besleme"; }
            else if (matches(text, "fertisol", "15-0-5", "15.0.5")) { score = 78; role = "Mikro element desteği"; }
            else if (matches(text, "humik", "fulvik", "leonardit", "super root")) { score = 74; role = "Kök ve toprak desteği"; }
        } else if ("ROOTING".equals(stage)) {
            if (matches(text, "humik", "fulvik", "leonardit", "super root")) { score = 92; role = "Kök / toprak desteği"; }
            else if (matches(text, "20-20-20", "20.20.20")) { score = 72; role = "Dengeli başlangıç desteği"; }
        } else if ("FLOWERING".equals(stage)) {
            if (matches(text, "calsimagsi", "kalsiyum")) { score = 86; role = "Kalsiyum desteği"; }
            else if (matches(text, "10-5-40", "10.5.40")) { score = 82; role = "Potasyum ağırlıklı destek"; }
            else if (matches(text, "deniz yosunu", "searius")) { score = 76; role = "Biyostimülant desteği"; }
        }
        String stock = stockSummary(product, profile);
        if (stock.contains("eksik") || stock.startsWith("Stok yok")) score -= 55;
        else if (stock.startsWith("Düşük stok")) score -= 25;
        else if (stock.equals("Bilgi girilmedi")) score -= 5;
        return new Candidate(product.getName(), Math.max(0, score), role, stock,
                doseSummary(product), zoneDoseSummary(product, profile), waitDays);
    }

    private static String buildContext(GardenZone zone, FertilizationProfile profile, WeatherForecast weather, long now) {
        List<String> parts = new ArrayList<>();
        long age = plantAge(profile.getPlanting_date());
        if (age >= 0) parts.add(age + " günlük");
        parts.add(stageLabel(profile.getGrowth_stage()));
        if (zone.hasSensorData()) parts.add("nem %" + zone.getMoisture());
        if (profile.getLast_application_at_epoch() > 0) {
            long days = Math.max(0, (now - profile.getLast_application_at_epoch()) / 86400L);
            parts.add("son uygulama " + days + " gün önce");
        }
        if (weather != null && weather.getTomorrowTemperatureMax() != null) parts.add("yarın " + Math.round(weather.getTomorrowTemperatureMax()) + "°C");
        return String.join(" · ", parts);
    }

    private static long plantAge(String value) {
        try { return ChronoUnit.DAYS.between(LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")), LocalDate.now()) + 1; }
        catch (Exception ignored) { return -1; }
    }
    private static long remainingDays(FertilizationProfile profile,
                                      FertilizerProduct product, long now) {
        String type = applicationType(product);
        FertilizerApplicationSchedule schedule = profile.getApplication_schedules() == null
                ? null : profile.getApplication_schedules().get(type);
        String productId = safe(product.getProduct_id(), "");
        if (schedule != null && (productId.equals(safe(schedule.getProduct_id(), ""))
                || (safe(schedule.getProduct_id(), "").isBlank()
                && safe(product.getName(), "").equalsIgnoreCase(
                safe(schedule.getProduct_name(), ""))))) {
            return daysUntil(schedule.getNext_application_at_epoch());
        }
        // Records created before product_id was added can be migrated safely.
        if ("NUTRITION".equals(type)
                && productId.equals(safe(profile.getActive_product_id(), ""))) {
            return daysUntil(profile.getNext_application_at_epoch());
        }
        return 0L;
    }
    private static long daysUntil(long epoch) {
        if (epoch <= 0) return 0L;
        return Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(),
                java.time.Instant.ofEpochSecond(epoch)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
    }
    private static long nearestWait(List<Candidate> candidates) {
        long nearest = Long.MAX_VALUE;
        for (Candidate candidate : candidates) {
            if (candidate.waitDays > 0) nearest = Math.min(nearest, candidate.waitDays);
        }
        return nearest;
    }
    private static String applicationType(FertilizerProduct product) {
        String type = safe(product.getApplication_type(), "NUTRITION").trim();
        return type.isEmpty() ? "NUTRITION" : type.toUpperCase(Locale.ROOT);
    }
    private static String doseSummary(FertilizerProduct product) {
        String unit = safe(product.getDosage_unit(), "etikete göre");
        double min = product.getLabel_dosage_min() > 0
                ? product.getLabel_dosage_min() : product.getLabel_dosage();
        double max = product.getLabel_dosage_max();
        if (min <= 0) return "Etiket dozu: kontrol edin";
        String amount = max > min ? trim(min) + "–" + trim(max) : trim(min);
        return "Etiket dozu: " + amount + " " + unit;
    }
    private static String stockSummary(FertilizerProduct product,
                                       FertilizationProfile profile) {
        String stockUnit = safe(product.getStock_unit(), "");
        DoseForZone need = doseForZone(product, profile);
        if (stockUnit.isBlank()) return "Bilgi girilmedi";
        if (product.getStock_amount() <= 0) {
            return need == null ? "Stok yok" : "0 " + stockUnit + " · "
                    + trim(need.max) + " " + need.unit + " eksik";
        }
        if (need == null || !stockUnit.equalsIgnoreCase(need.unit)) {
            return product.getLow_stock_threshold() > 0
                    && product.getStock_amount() <= product.getLow_stock_threshold()
                    ? "Düşük stok: " + trim(product.getStock_amount()) + " " + stockUnit
                    : trim(product.getStock_amount()) + " " + stockUnit;
        }
        if (product.getStock_amount() < need.max) {
            return trim(product.getStock_amount()) + " " + stockUnit + " · "
                    + trim(need.max - product.getStock_amount()) + " " + need.unit + " eksik";
        }
        int applications = (int) Math.floor(product.getStock_amount() / need.max);
        return trim(product.getStock_amount()) + " " + stockUnit
                + " · yaklaşık " + Math.max(1, applications) + " uygulama yeter";
    }
    private static String zoneDoseSummary(FertilizerProduct product,
                                          FertilizationProfile profile) {
        DoseForZone dose = doseForZone(product, profile);
        if (dose == null) return "";
        String amount = dose.max > dose.min ? trim(dose.min) + "–" + trim(dose.max)
                : trim(dose.min);
        String sourceUnit = safe(product.getDosage_unit(), "");
        String method = applicationNote(sourceUnit);
        String scope = dose.isTank ? trim(profile.getTank_liters()) + " L tank için"
                : trim(profile.getArea_m2()) + " m² için";
        String areaOrTank = (dose.isTank ? "Tank dozu: " : "Alan dozu: ")
                + amount + " " + dose.unit + " · " + scope;
        String tankMix = tankMixSummary(product, profile, sourceUnit);
        if (!tankMix.isBlank()) return areaOrTank + "\n" + tankMix;
        return areaOrTank + (method.isBlank() ? "" : " · " + method);
    }
    private static String tankMixSummary(FertilizerProduct product,
                                         FertilizationProfile profile,
                                         String sourceUnit) {
        if (profile.getTank_liters() <= 0 || !sourceUnit.toLowerCase(Locale.ROOT)
                .replace(" ", "").contains("tonsu")) return "";
        double min = product.getLabel_dosage_min() > 0
                ? product.getLabel_dosage_min() : product.getLabel_dosage();
        double max = product.getLabel_dosage_max() > 0
                ? product.getLabel_dosage_max() : min;
        if (min <= 0) return "";
        String normalized = sourceUnit.toLowerCase(Locale.ROOT).replace(" ", "");
        double factor;
        String unit;
        if (normalized.contains("kg/dekar")) {
            // kg/dekar with one ton water equals grams per litre.
            factor = 1.0;
            unit = "g";
        } else if (normalized.contains("l/dekar")) {
            // L/dekar with one ton water equals millilitres per litre.
            factor = 1.0;
            unit = "ml";
        } else {
            return "";
        }
        double tankMin = min * profile.getTank_liters() * factor;
        double tankMax = max * profile.getTank_liters() * factor;
        String amount = tankMax > tankMin ? trim(tankMin) + "–" + trim(tankMax)
                : trim(tankMin);
        String applicationVolume = profile.getArea_m2() > 0
                ? " · bu bölge için yaklaşık " + trim(profile.getArea_m2()) + " L çözelti uygulayın"
                : "";
        return "Tank karışımı: " + amount + " " + unit + " · "
                + trim(profile.getTank_liters()) + " L tank için" + applicationVolume;
    }
    private static String applicationNote(String unit) {
        int separator = unit.indexOf('·');
        return separator < 0 ? "" : unit.substring(separator + 1).trim();
    }
    private static DoseForZone doseForZone(FertilizerProduct product,
                                           FertilizationProfile profile) {
        double min = product.getLabel_dosage_min() > 0 ? product.getLabel_dosage_min() : product.getLabel_dosage();
        double max = product.getLabel_dosage_max() > 0 ? product.getLabel_dosage_max() : min;
        if (min <= 0) return null;
        String unit = safe(product.getDosage_unit(), "").toLowerCase(Locale.ROOT).replace(" ", "");
        if (unit.contains("kg/dekar") && profile.getArea_m2() > 0) return new DoseForZone(min * profile.getArea_m2(), max * profile.getArea_m2(), "g", false);
        if (unit.contains("l/dekar") && profile.getArea_m2() > 0) return new DoseForZone(min * profile.getArea_m2(), max * profile.getArea_m2(), "ml", false);
        if (unit.contains("ml/100l") && profile.getTank_liters() > 0) return new DoseForZone(min * profile.getTank_liters() / 100.0, max * profile.getTank_liters() / 100.0, "ml", true);
        return null;
    }
    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
    private static boolean matches(String value, String... keys) { for (String key : keys) if (value.contains(key)) return true; return false; }
    private static String stageLabel(String stage) { if ("FRUITING".equals(stage)) return "Meyve dönemi"; if ("FLOWERING".equals(stage)) return "Çiçeklenme"; if ("VEGETATIVE".equals(stage)) return "Vejetatif dönem"; if ("ROOTING".equals(stage)) return "Kök gelişimi"; return "Gelişim dönemi"; }
    private static String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static class DoseForZone { final double min, max; final String unit; final boolean isTank; DoseForZone(double min, double max, String unit, boolean isTank) { this.min = min; this.max = max; this.unit = unit; this.isTank = isTank; } }
    private static class Candidate { final String name, role, stock, dose, zoneDose; final int score; final long waitDays; Candidate(String name, int score, String role, String stock, String dose, String zoneDose, long waitDays) { this.name=name; this.score=score; this.role=role; this.stock=stock; this.dose=dose; this.zoneDose=zoneDose; this.waitDays=waitDays; } int score() { return score; } boolean isAvailable() { return waitDays <= 0; } String display() { return stars(score) + " " + name + "\n" + role + "\n" + dose + (zoneDose.isBlank() ? "" : "\n" + zoneDose) + "\nStok: " + stock; } private static String stars(int value) { int count=Math.max(1, Math.min(5, Math.round(value / 20f))); return "★★★★★".substring(0, count) + "☆☆☆☆☆".substring(0, 5-count); } }
}
