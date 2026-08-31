package com.alidogukan.avora.fertilization;

import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.FertilizerProduct;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.WeatherForecast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Explains risks without changing a fertilizer plan or starting an application. */
public final class FertilizerRiskAdvisor {

    private FertilizerRiskAdvisor() { }

    public static List<String> assess(GardenZone zone,
                                      List<FertilizerProduct> products,
                                      WeatherForecast weather, long now) {
        List<String> risks = new ArrayList<>();
        FertilizationProfile profile = zone.getFertilization();
        if (profile == null || !profile.isEnabled()) {
            risks.add("Gubreleme profili tamamlanmadi; uygulama onerisi icin "
                    + "dikim tarihi ve gelisim donemini girin.");
            return risks;
        }

        risks.addAll(FertilizerDataFreshnessPolicy.warnings(zone, weather, now));
        boolean sensorFresh = FertilizerDataFreshnessPolicy.isSensorFresh(zone, now);
        boolean weatherFresh = FertilizerDataFreshnessPolicy.isWeatherFresh(weather, now);
        boolean analysisFresh = FertilizerDataFreshnessPolicy
                .isWaterAnalysisFresh(profile, now);

        if (sensorFresh && zone.getMoisture() < zone.getMoisture_limit()) {
            risks.add("Toprak nemi dusuk. Damlama ile besleme dusunuluyorsa "
                    + "once kok bolgesini guvenli nem duzeyine getirin.");
        }
        if (weatherFresh && weather.getTomorrowTemperatureMax() != null
                && weather.getTomorrowTemperatureMax() >= 35.0) {
            risks.add("Yarin sicaklik yuksek gorunuyor. Uygulamayi serin saatlere "
                    + "alin ve etiketteki sulandirma onerisine uyun.");
        }
        if (analysisFresh && profile.getWater_ph() > 0.0
                && (profile.getWater_ph() < 5.5 || profile.getWater_ph() > 7.5)) {
            risks.add("Girilen su pH değeri " + String.format(Locale.ROOT, "%.1f", profile.getWater_ph())
                    + ". Besin alımını etkileyebilir; ürün etiketi ve su/toprak analiziyle doğrulayın.");
        }
        if (analysisFresh && profile.getWater_ec_ms() >= 2.5) {
            risks.add("Girilen EC " + String.format(Locale.ROOT, "%.2f", profile.getWater_ec_ms())
                    + " mS/cm. Yeni gübre eklemeden önce su ve kök bölgesi analizini kontrol edin.");
        }

        FertilizerProduct active = productById(products, profile.getActive_product_id());
        if (active != null && profile.getLast_application_at_epoch() > 0
                && active.getMinimum_interval_days() > 0) {
            long days = calendarDaysSince(profile.getLast_application_at_epoch());
            long remaining = active.getMinimum_interval_days() - days;
            if (remaining > 0) {
                risks.add("Son uygulamadan sonra " + remaining + " gun daha bekleyin. "
                        + "Secili urunun minimum tekrar araligi korunuyor.");
            }
        }

        if ("FRUITING".equals(profile.getGrowth_stage())
                && !hasPotassiumSupport(products)) {
            risks.add("Meyve doneminde depoda yuksek potasyum destegi bulunamadi. "
                    + "Yeni urun alma kararini etiket ve analizle verin.");
        }
        if (allStockUnavailable(products)) {
            risks.add("Etkin urunlerin stok bilgisi eksik veya tukendi. "
                    + "Oneriyi uygulamadan once stoklari guncelleyin.");
        }
        return risks;
    }

    private static FertilizerProduct productById(List<FertilizerProduct> products,
                                                 String id) {
        if (id == null || id.isBlank()) return null;
        for (FertilizerProduct product : products) {
            if (product != null && id.equals(product.getProduct_id())) return product;
        }
        return null;
    }

    private static boolean hasPotassiumSupport(List<FertilizerProduct> products) {
        for (FertilizerProduct product : products) {
            if (product == null || !product.isEnabled()) continue;
            FertilizerNutrientProfile nutrients = FertilizerNutrientProfile.from(product);
            String text = (product.getName() == null ? "" : product.getName())
                    .toLowerCase(Locale.ROOT);
            if (nutrients.isPotassiumForward() || text.contains("potasyum")) return true;
        }
        return false;
    }

    private static boolean allStockUnavailable(List<FertilizerProduct> products) {
        boolean hasTrackedProduct = false;
        for (FertilizerProduct product : products) {
            if (product == null || !product.isEnabled()) continue;
            if (product.getStock_unit() == null || product.getStock_unit().isBlank()) continue;
            hasTrackedProduct = true;
            if (product.getStock_amount() > 0) return false;
        }
        // No tracked product means stock information is missing; tracked but
        // no positive balance means every tracked item is exhausted.
        return true;
    }

    private static long calendarDaysSince(long epoch) {
        if (epoch <= 0) return 0L;
        return Math.max(0L, ChronoUnit.DAYS.between(
                Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()));
    }
}
