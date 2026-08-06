package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerProduct;

import java.util.Locale;

/**
 * Intentionally conservative first-pass tank mix rules. The label and a jar
 * test always take precedence because commercial formulations may differ.
 */
public final class FertilizerMixAdvisor {

    private FertilizerMixAdvisor() { }

    public static FertilizerMixResult assess(
            FertilizerProduct first, FertilizerProduct second) {
        String firstText = textFor(first);
        String secondText = textFor(second);
        FertilizerNutrientProfile firstNutrients = FertilizerNutrientProfile.from(first);
        FertilizerNutrientProfile secondNutrients = FertilizerNutrientProfile.from(second);
        boolean firstCalcium = containsCalcium(firstText);
        boolean secondCalcium = containsCalcium(secondText);
        boolean firstPhosphate = containsPhosphate(firstText)
                || (firstNutrients.hasNpk() && firstNutrients.getPhosphorus() > 0);
        boolean secondPhosphate = containsPhosphate(secondText)
                || (secondNutrients.hasNpk() && secondNutrients.getPhosphorus() > 0);
        boolean firstSulphate = containsSulphate(firstText);
        boolean secondSulphate = containsSulphate(secondText);

        if (sameProduct(first, second)) {
            return caution("Ayni urun iki kez secildi",
                    "Dozu ikiye katlamayin. Tek urunun etiket dozunu "
                            + "esas alin.");
        }

        if ((firstCalcium && (secondPhosphate || secondSulphate))
                || (secondCalcium && (firstPhosphate || firstSulphate))) {
            return caution("Yuksek cokelti riski",
                    "Kalsiyumlu urun ile fosfatli veya sulfatli urun secildi. "
                            + "Ayni tankta kullanmayin; ayri tankta uygulayin. "
                            + "Suyunuzun pH/sertligi ve urun etiketi sonucu degistirebilir.");
        }

        if (firstCalcium || secondCalcium) {
            return caution("Dikkatli karisim",
                    "Kalsiyum destegi secildi. Etiketteki karisim "
                            + "uyarilarini kontrol edin ve uygulama once "
                            + "kucuk bir kapta kavanoz testi yapin.");
        }

        return new FertilizerMixResult("Uyumluluk dogrulanmadi",
                "Bu iki urun icin elde etiket/formulasyon bilgisi yeterli degil. "
                        + "Ayni sulama suyu ve uygulama yogunluguyla kavanoz testi yapin; "
                        + "bulanma veya cokelti varsa birlikte kullanmayin.", true);
    }

    private static FertilizerMixResult caution(String title, String message) {
        return new FertilizerMixResult(title, message, true);
    }

    private static boolean sameProduct(FertilizerProduct first,
                                       FertilizerProduct second) {
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
                || text.contains("npk") || text.matches(".*\\d+[-.]\\d+[-.]\\d+.*");
    }

    private static boolean containsSulphate(String text) {
        return text.contains("sulfat") || text.contains("sulphate")
                || text.contains("magsul") || text.contains("magnezyum sulfat");
    }

    private static String textFor(FertilizerProduct product) {
        return ((product.getName() == null ? "" : product.getName()) + " "
                + (product.getNpk() == null ? "" : product.getNpk()))
                .toLowerCase(Locale.ROOT);
    }
}
