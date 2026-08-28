package com.ali.smartgarden.crop;

import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.season.SeasonStartConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Built-in catalog and merge rules for device-specific user products. */
public final class CropCatalog {
    public static final String CUSTOM_ID = "__new_crop__";

    private CropCatalog() { }

    public static List<CropCatalogItem> builtIns() {
        return Arrays.asList(
                system("tomato", "Domates", "🍅", "tomato", 40, 60),
                system("pepper", "Biber", "🌶️", "pepper", 40, 60),
                system("cucumber", "Salatalık", "🥒", "cucumber", 45, 65),
                system("eggplant", "Patlıcan", "🍆", "eggplant", 40, 60),
                system("bean", "Fasulye", "🫘", "bean", 45, 65),
                system("carrot", "Havuç", "🥕", "carrot", 35, 55),
                system("okra", "Bamya", "🌱", "okra", 35, 55),
                system("zucchini", "Kabak", "🥒", "zucchini", 40, 60),
                system("lettuce", "Marul", "🥬", "lettuce", 50, 70),
                system("onion", "Soğan", "🧅", "onion", 35, 55),
                system("potato", "Patates", "🥔", "potato", 40, 60),
                system("corn", "Mısır", "🌽", "corn", 40, 60),
                system("pea", "Bezelye", "🌱", "pea", 45, 65),
                system("strawberry", "Çilek", "🍓", "strawberry", 45, 65),
                system("watermelon", "Karpuz", "🍉", "watermelon", 35, 55),
                system("melon", "Kavun", "🍈", "melon", 35, 55)
        );
    }

    public static CropCatalogItem newUserItem(String name, String emoji, int min, int max) {
        String plantType = SeasonStartConfiguration.customPlantType(name);
        String id = "user-" + plantType + "-" + System.currentTimeMillis();
        CropCatalogItem item = new CropCatalogItem(id, name.trim(), emoji.trim(), plantType,
                min, max, CropCatalogItem.SOURCE_USER, true);
        long now = System.currentTimeMillis() / 1000L;
        item.setCreated_at_epoch(now);
        item.setUpdated_at_epoch(now);
        return item;
    }

    public static List<CropCatalogItem> merge(List<CropCatalogItem> userItems) {
        Map<String, CropCatalogItem> unique = new LinkedHashMap<>();
        for (CropCatalogItem item : builtIns()) unique.put(item.getCrop_id(), item);
        if (userItems != null) {
            for (CropCatalogItem item : userItems) {
                if (item == null || !item.isEnabled() || blank(item.getName())) continue;
                unique.put(item.getCrop_id(), item);
            }
        }
        List<CropCatalogItem> merged = new ArrayList<>(unique.values());
        merged.sort(Comparator.comparing(CropCatalogItem::getName,
                java.text.Collator.getInstance(Locale.forLanguageTag("tr-TR"))));
        return merged;
    }

    private static CropCatalogItem system(String id, String name, String emoji,
                                          String type, int min, int max) {
        return new CropCatalogItem(id, name, emoji, type, min, max,
                CropCatalogItem.SOURCE_SYSTEM, true);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
