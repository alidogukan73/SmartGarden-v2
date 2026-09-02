package com.alidogukan.avora.photos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.alidogukan.avora.models.GardenPhoto;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public final class JournalPhotoRecordFilterTest {
    @Test
    public void plantAssistantLabelDoesNotMergeIndependentAnalyses() {
        GardenPhoto first = photo("one", "zone-1", "plant_assistant", "/one.jpg");
        GardenPhoto selected = photo("two", "zone-1", "plant_assistant", "/two.jpg");
        GardenPhoto third = photo("three", "zone-1", "plant_assistant", "/three.jpg");

        List<GardenPhoto> result = JournalPhotoRecordFilter.select(
                Arrays.asList(first, selected, third),
                "zone-1",
                "plant_assistant",
                "/two.jpg"
        );

        assertEquals(1, result.size());
        assertSame(selected, result.get(0));
    }

    @Test
    public void journalRecordGroupKeepsAllAndOnlyItsOwnPhotos() {
        GardenPhoto first = photo("one", "zone-1", "journal_record_a", "/one.jpg");
        GardenPhoto second = photo("two", "zone-1", "journal_record_a", "/two.jpg");
        GardenPhoto otherGroup = photo("three", "zone-1", "journal_record_b", "/three.jpg");
        GardenPhoto otherZone = photo("four", "zone-2", "journal_record_a", "/four.jpg");

        List<GardenPhoto> result = JournalPhotoRecordFilter.select(
                Arrays.asList(first, second, otherGroup, otherZone),
                "zone-1",
                "journal_record_a",
                "/one.jpg"
        );

        assertEquals(Arrays.asList(first, second), result);
    }

    private static GardenPhoto photo(String id, String zone, String group, String path) {
        GardenPhoto result = new GardenPhoto();
        result.setId(id);
        result.setZone_id(zone);
        result.setRelated_application_id(group);
        result.setLocal_path(path);
        return result;
    }
}
