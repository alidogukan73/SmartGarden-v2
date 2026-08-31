package com.alidogukan.avora.fertilization;

import com.alidogukan.avora.models.FertilizerProduct;

import java.util.List;
import java.util.Locale;

/**
 * Intentionally conservative tank mix rules. Product labels and a jar test
 * always take precedence because commercial formulations and water differ.
 */
public final class FertilizerMixAdvisor {

    private FertilizerMixAdvisor() { }

    public static FertilizerMixResult assess(
            FertilizerProduct first, FertilizerProduct second) {
        if (first == null || second == null) {
            return blocked(
                    "Ürün bilgisi eksik",
                    "Karışım değerlendirmesi için iki geçerli ürün seçin."
            );
        }
        String firstText = textFor(first);
        String secondText = textFor(second);
        FertilizerNutrientProfile firstNutrients =
                FertilizerNutrientProfile.from(first);
        FertilizerNutrientProfile secondNutrients =
                FertilizerNutrientProfile.from(second);
        boolean firstCalcium = hasTag(first, "CALCIUM")
                || hasTag(first, "CALCIUM_MAGNESIUM")
                || containsCalcium(firstText);
        boolean secondCalcium = hasTag(second, "CALCIUM")
                || hasTag(second, "CALCIUM_MAGNESIUM")
                || containsCalcium(secondText);
        boolean firstPhosphate = hasTag(first, "PHOSPHATE")
                || containsPhosphate(firstText)
                || (firstNutrients.hasNpk()
                && firstNutrients.getPhosphorus() > 0);
        boolean secondPhosphate = hasTag(second, "PHOSPHATE")
                || containsPhosphate(secondText)
                || (secondNutrients.hasNpk()
                && secondNutrients.getPhosphorus() > 0);
        boolean firstSulphate = hasTag(first, "SULFATE")
                || containsSulphate(firstText);
        boolean secondSulphate = hasTag(second, "SULFATE")
                || containsSulphate(secondText);
        boolean firstMicrobial = hasTag(first, "MICROBIAL")
                || containsMicrobial(firstText);
        boolean secondMicrobial = hasTag(second, "MICROBIAL")
                || containsMicrobial(secondText);
        boolean firstHumic = hasTag(first, "HUMIC_FULVIC")
                || containsHumic(firstText);
        boolean secondHumic = hasTag(second, "HUMIC_FULVIC")
                || containsHumic(secondText);

        if (sameProduct(first, second)) {
            return blocked(
                    "Aynı ürün iki kez seçildi",
                    "Dozu ikiye katlamayın. Tek ürünün etiket dozunu esas alın."
            );
        }

        if ((firstCalcium && (secondPhosphate || secondSulphate))
                || (secondCalcium && (firstPhosphate || firstSulphate))) {
            return blocked(
                    "Yüksek çökelti riski",
                    "Kalsiyumlu ürün ile fosfatlı veya sülfatlı ürün seçildi. "
                            + "Aynı tankta kullanmayın; ayrı uygulayın. Ürün "
                            + "etiketi farklı bir yöntem belirtmedikçe bu "
                            + "karışım AVORA tarafından kaydedilmez."
            );
        }

        if (firstNutrients.isBalanced() && secondNutrients.isBalanced()) {
            return blocked(
                    "Aynı amaçlı iki ana gübre",
                    "İki dengeli NPK ürünü birlikte seçildi. Besin dozunu "
                            + "istemeden artırmamak için yalnız birini seçin "
                            + "ve etiket dozunu esas alın."
            );
        }

        if ((firstMicrobial && isConcentratedMineral(secondNutrients,
                secondPhosphate, secondSulphate, secondCalcium))
                || (secondMicrobial && isConcentratedMineral(firstNutrients,
                firstPhosphate, firstSulphate, firstCalcium))) {
            return caution(
                    "Mikrobiyal ürün koruması",
                    "Canlı mikroorganizma içeren ürün ile mineral gübre "
                            + "seçildi. Etiket iki ürünü açıkça uyumlu "
                            + "göstermiyorsa aynı tankta bekletmeyin; ayrı "
                            + "uygulayın."
            );
        }

        if ((firstHumic && isConcentratedMineral(secondNutrients,
                secondPhosphate, secondSulphate, secondCalcium))
                || (secondHumic && isConcentratedMineral(firstNutrients,
                firstPhosphate, firstSulphate, firstCalcium))) {
            return caution(
                    "Organik düzenleyici karışımı",
                    "Hümik/fülvik ürün ile mineral gübrenin uyumu ürün "
                            + "formülasyonuna ve suya bağlıdır. Etikette izin "
                            + "yoksa ayrı uygulayın; kullanmadan önce aynı "
                            + "suyla kavanoz testi yapın."
            );
        }

        if (firstCalcium || secondCalcium) {
            return caution(
                    "Dikkatli karışım",
                    "Kalsiyum desteği seçildi. Etiketteki karışım uyarılarını "
                            + "kontrol edin ve aynı sulama suyuyla küçük bir "
                            + "kapta kavanoz testi yapın."
            );
        }

        return new FertilizerMixResult(
                "Uyumluluk doğrulanmadı",
                "Bu iki ürün için etiket ve formülasyon bilgisi yeterli değil. "
                        + "Aynı sulama suyu ve uygulama yoğunluğuyla kavanoz "
                        + "testi yapın; bulanma, ısınma veya çökelti varsa "
                        + "birlikte kullanmayın.",
                FertilizerMixResult.RiskLevel.UNVERIFIED
        );
    }

    private static FertilizerMixResult caution(String title, String message) {
        return new FertilizerMixResult(
                title, message, FertilizerMixResult.RiskLevel.CAUTION
        );
    }

    private static FertilizerMixResult blocked(String title, String message) {
        return new FertilizerMixResult(
                title, message, FertilizerMixResult.RiskLevel.BLOCKED
        );
    }

    private static boolean sameProduct(
            FertilizerProduct first,
            FertilizerProduct second
    ) {
        String firstId = first.getProduct_id();
        return firstId != null && firstId.equals(second.getProduct_id());
    }

    private static boolean containsCalcium(String text) {
        return text.contains("kalsiyum") || text.contains("calcium")
                || text.contains("calsimagsi") || text.contains("calmag");
    }

    private static boolean containsPhosphate(String text) {
        return text.contains("fosfat") || text.contains("phosphate")
                || text.contains("map") || text.contains("mkp")
                || text.contains("npk")
                || text.matches(".*\\d+[-.]\\d+[-.]\\d+.*");
    }

    private static boolean containsSulphate(String text) {
        return text.contains("sulfat") || text.contains("sülfat")
                || text.contains("sulphate") || text.contains("magsul")
                || text.contains("magnezyum sulfat")
                || text.contains("magnezyum sülfat");
    }

    private static boolean containsMicrobial(String text) {
        return text.contains("mikrobiyal") || text.contains("microbial")
                || text.contains("bakteri") || text.contains("bacteria")
                || text.contains("kob/ml") || text.contains("cfu/ml");
    }

    private static boolean containsHumic(String text) {
        return text.contains("humik") || text.contains("hümik")
                || text.contains("humic") || text.contains("fulvik")
                || text.contains("fülvik") || text.contains("fulvic");
    }

    private static boolean isConcentratedMineral(
            FertilizerNutrientProfile nutrients,
            boolean phosphate,
            boolean sulphate,
            boolean calcium
    ) {
        return nutrients.hasNpk() || phosphate || sulphate || calcium;
    }

    private static boolean hasTag(
            FertilizerProduct product,
            String expected
    ) {
        List<String> tags = product.getFunctional_tags();
        if (tags == null) return false;
        for (String tag : tags) {
            if (expected.equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    private static String textFor(FertilizerProduct product) {
        return ((product.getName() == null ? "" : product.getName()) + " "
                + (product.getNpk() == null ? "" : product.getNpk()) + " "
                + (product.getNotes() == null ? "" : product.getNotes()))
                .toLowerCase(Locale.ROOT);
    }
}
