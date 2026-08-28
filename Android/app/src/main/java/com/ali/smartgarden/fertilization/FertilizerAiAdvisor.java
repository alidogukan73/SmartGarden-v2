package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerProduct;

import java.util.Arrays;
import java.util.Locale;

/** Rule-based, traceable first layer of the SmartGarden fertilizer assistant. */
public final class FertilizerAiAdvisor {

    private FertilizerAiAdvisor() { }

    public static FertilizerAiProfile profileFor(FertilizerProduct product) {
        String name = safe(product.getName());
        String npk = safe(product.getNpk());
        String searchable = (name + " " + npk).toLowerCase(Locale.ROOT);

        if (hasTraceElements(product)) {
            String organicNote = product.isOrganic_farming_eligible()
                    ? " \u00dcr\u00fcn etiketi organik tar\u0131mda girdi olarak kullan\u0131labilece\u011fini belirtiyor."
                    : "";
            return profile(
                    "\u00c7oklu iz element deste\u011fi",
                    "Destek ama\u00e7l\u0131",
                    "Demir, \u00e7inko, bor, bak\u0131r ve mangan gibi iz elementleri destekler; ana NPK beslemesinin yerine ge\u00e7mez."
                            + organicNote,
                    "Meyve d\u00f6neminde yaln\u0131z ihtiya\u00e7 ve etiket do\u011frultusunda destek olarak de\u011ferlendirilir."
            );
        }

        if (searchable.contains("og toros") && searchable.contains("organik")) {
            return profile(
                    "Toprak hazırlığı ürünü",
                    "Yalnız toprak hazırlığında",
                    "Ürün etiketi sebze ve tarla bitkilerinde dikim öncesi toprak hazırlığını belirtir.",
                    "Meyve oluşumu ve hasat döneminde önerilmez; gelecek sezonun toprak hazırlığı planında değerlendirilir."
            );
        }
        if (searchable.contains("10.5.40") || searchable.contains("10-5-40")) {
            return profile("Ana potasyum beslemesi", "Uygun", "Potasyum ağırlığı meyve gelişimi ve kaliteyi destekler.", "Meyve döneminde ana besleme için uygundur.");
        }
        if (searchable.contains("20.20.20") || searchable.contains("20-20-20")) {
            return profile("Dengeli ana besleme", "Uygun", "Eşit NPK oranı vejetatif gelişim için dengeli destek sağlar.", "Meyve döneminde tek başına ana ürün olarak tercih edilmez; potasyum ağırlıklı ürünle dönüşümlü düşünülür.");
        }
        if (searchable.contains("fertisol") || searchable.contains("15.0.5") || searchable.contains("15-0-5")) {
            return profile("Mikro element desteği", "Destek amaçlı", "Azot yüksek, potasyum düşük ve fosfor yoktur; tek başına tam besleme sağlamaz.", "Meyve döneminde potasyum ağırlıklı ana besleme ile dönüşümlü kullanılır.");
        }
        if (searchable.contains("calsimagsi") || searchable.contains("kalsiyum") || searchable.contains("magsul")) {
            return profile("Kalsiyum / magnezyum desteği", "Destek amaçlı", "Kalsiyum ve magnezyum desteği sağlar; ana NPK programının yerine geçmez.", "Meyve kalitesi desteği için uygundur; fosfatlı veya sülfatlı ürünlerle aynı tankta karıştırmayın.");
        }
        if (searchable.contains("humik") || searchable.contains("fulvik") || searchable.contains("leonardit")) {
            return profile("Toprak düzenleyici", "Destek amaçlı", "Kök bölgesi ve besin alımını destekler; doğrudan ana NPK beslemesi değildir.", "Ana besleme uygulamasından ayrı kaydedin; programı tek başına değiştirmez.");
        }
        return profile("Besleme desteği", "Etikete göre değerlendir", "Ürünün NPK / içerik ve etiketi yeterli profil için doğrulanmalı.", "Toprak ve yaprak analizine göre karar verin.");
    }

    private static boolean hasTraceElements(FertilizerProduct product) {
        if (product.getFunctional_tags() == null) return false;
        for (String tag : product.getFunctional_tags()) {
            if ("TRACE_ELEMENTS".equalsIgnoreCase(safe(tag))) return true;
        }
        return false;
    }
    private static FertilizerAiProfile profile(String role, String suitability, String reason, String fruitAdvice) {
        return new FertilizerAiProfile(role, Arrays.asList("Kök", "Vejetatif", "Erken çiçeklenme"), suitability, reason, fruitAdvice, "Kesin doz için ürün etiketi ile toprak/yaprak analizini esas alın.");
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
