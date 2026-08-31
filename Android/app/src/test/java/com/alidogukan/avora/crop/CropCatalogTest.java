package com.alidogukan.avora.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.CropCatalogItem;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class CropCatalogTest {
    @Test
    public void builtInsContainCarrotAndOkra() {
        List<CropCatalogItem> items = CropCatalog.builtIns();
        assertTrue(items.stream().anyMatch(item -> "carrot".equals(item.getPlant_type())));
        assertTrue(items.stream().anyMatch(item -> "okra".equals(item.getPlant_type())));
    }

    @Test
    public void userItemIsMergedWithoutChangingSystemItems() {
        CropCatalogItem user = CropCatalog.newUserItem("Roka", "🥬", 40, 65);
        List<CropCatalogItem> merged = CropCatalog.merge(Collections.singletonList(user));
        assertTrue(merged.stream().anyMatch(item -> "Roka".equals(item.getName())));
        assertTrue(merged.stream().anyMatch(CropCatalogItem::isSystemItem));
        assertEquals(CropCatalogItem.SOURCE_USER, user.getSource());
    }
}
