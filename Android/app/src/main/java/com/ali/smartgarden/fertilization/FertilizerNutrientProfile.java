package com.ali.smartgarden.fertilization;

import com.ali.smartgarden.models.FertilizerProduct;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a conventional N-P2O5-K2O label without relying on the product name. */
public final class FertilizerNutrientProfile {

    private static final Pattern NPK_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*[-/]\\s*"
                    + "(\\d+(?:[.,]\\d+)?)\\s*[-/]\\s*"
                    + "(\\d+(?:[.,]\\d+)?)"
    );

    private final Double nitrogen;
    private final Double phosphorus;
    private final Double potassium;

    private FertilizerNutrientProfile(Double nitrogen, Double phosphorus,
                                      Double potassium) {
        this.nitrogen = nitrogen;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
    }

    public static FertilizerNutrientProfile from(FertilizerProduct product) {
        String source = ((product.getNpk() == null ? "" : product.getNpk())
                + " " + (product.getName() == null ? "" : product.getName()))
                .toLowerCase(Locale.ROOT);
        Matcher matcher = NPK_PATTERN.matcher(source);
        if (!matcher.find()) return new FertilizerNutrientProfile(null, null, null);
        return new FertilizerNutrientProfile(parse(matcher.group(1)),
                parse(matcher.group(2)), parse(matcher.group(3)));
    }

    public boolean hasNpk() { return nitrogen != null && phosphorus != null && potassium != null; }
    public double getNitrogen() { return nitrogen == null ? 0.0 : nitrogen; }
    public double getPhosphorus() { return phosphorus == null ? 0.0 : phosphorus; }
    public double getPotassium() { return potassium == null ? 0.0 : potassium; }
    public boolean isPotassiumForward() { return hasNpk() && getPotassium() > getNitrogen() && getPotassium() >= 20.0; }
    public boolean isNitrogenForward() {
        return hasNpk() && getNitrogen() >= 15.0
                && getNitrogen() > getPotassium()
                && getNitrogen() > getPhosphorus();
    }
    public boolean isBalanced() { return hasNpk() && Math.abs(getNitrogen() - getPotassium()) <= 2.0 && Math.abs(getNitrogen() - getPhosphorus()) <= 2.0; }

    private static Double parse(String value) {
        try { return Double.parseDouble(value.replace(',', '.')); }
        catch (NumberFormatException ignored) { return null; }
    }
}
