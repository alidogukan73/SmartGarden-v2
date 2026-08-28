package com.ali.smartgarden.season;

import com.ali.smartgarden.models.GardenZone;

import java.text.Normalizer;
import java.util.Locale;

/** Crop identity selected explicitly when a new zone season starts. */
public final class SeasonStartConfiguration {
    private final String cropName;
    private final String plantType;
    private final String emoji;

    public SeasonStartConfiguration(
            String cropName,
            String plantType,
            String emoji
    ) {
        this.cropName = safe(cropName).trim();
        this.plantType = safe(plantType).trim();
        this.emoji = safe(emoji).trim().isEmpty() ? "🌱" : safe(emoji).trim();
    }

    public static SeasonStartConfiguration fromZone(GardenZone zone) {
        if (zone == null) return new SeasonStartConfiguration("", "", "🌱");
        return new SeasonStartConfiguration(
                zone.getName(),
                zone.getPlant_type(),
                zone.getEmoji()
        );
    }

    public static String customPlantType(String cropName) {
        String normalized = normalizedCropName(cropName)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isEmpty() ? "custom_crop" : normalized;
    }

    /** Provides a helpful default icon while keeping the icon editable in the season form. */
    public static String suggestedCropEmoji(String cropName) {
        String crop = normalizedCropName(cropName);
        if (crop.contains("domates")) return "🍅";
        if (crop.contains("biber")) return "🌶️";
        if (crop.contains("salatalik") || crop.contains("hiyar")) return "🥒";
        if (crop.contains("patlican")) return "🍆";
        if (crop.contains("havuc")) return "🥕";
        if (crop.contains("fasulye") || crop.contains("nohut") || crop.contains("mercimek")) return "🫘";
        if (crop.contains("bezelye")) return "🫛";
        if (crop.contains("marul") || crop.contains("lahana") || crop.contains("ispanak")
                || crop.contains("roka") || crop.contains("pazi")) return "🥬";
        if (crop.contains("misir")) return "🌽";
        if (crop.contains("patates")) return "🥔";
        if (crop.contains("sogan")) return "🧅";
        if (crop.contains("sarimsak")) return "🧄";
        if (crop.contains("brokoli") || crop.contains("karnabahar")) return "🥦";
        if (crop.contains("kabak")) return "🎃";
        if (crop.contains("karpuz")) return "🍉";
        if (crop.contains("kavun")) return "🍈";
        if (crop.contains("cilek")) return "🍓";
        if (crop.contains("uzum")) return "🍇";
        if (crop.contains("elma")) return "🍎";
        if (crop.contains("limon")) return "🍋";
        if (crop.contains("portakal")) return "🍊";
        return "🌱";
    }

    public String getCropName() { return cropName; }
    public String getPlantType() { return plantType; }
    public String getEmoji() { return emoji; }

    public boolean isValid() {
        return !cropName.isEmpty() && !plantType.isEmpty();
    }

    private static String normalizedCropName(String cropName) {
        return Normalizer.normalize(safe(cropName), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ı', 'i');
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
