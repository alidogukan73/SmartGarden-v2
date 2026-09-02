package com.alidogukan.avora.photos;

import com.alidogukan.avora.models.GardenPhoto;

import java.util.ArrayList;
import java.util.List;

/** Selects only the photos that belong to the journal record being viewed. */
public final class JournalPhotoRecordFilter {
    private static final String JOURNAL_GROUP_PREFIX = "journal_record_";

    private JournalPhotoRecordFilter() { }

    public static List<GardenPhoto> select(List<GardenPhoto> photos,
                                           String zoneId,
                                           String groupId,
                                           String selectedPath) {
        List<GardenPhoto> result = new ArrayList<>();
        if (photos == null) return result;
        String targetZone = safe(zoneId);
        String targetGroup = safe(groupId);
        String targetPath = safe(selectedPath);
        boolean groupedJournalRecord = targetGroup.startsWith(JOURNAL_GROUP_PREFIX);

        for (GardenPhoto photo : photos) {
            if (photo == null || !targetZone.equals(safe(photo.getZone_id()))) continue;
            if (groupedJournalRecord) {
                if (targetGroup.equals(safe(photo.getRelated_application_id()))) {
                    result.add(photo);
                }
            } else if (!targetPath.isBlank()
                    && targetPath.equals(safe(photo.getLocal_path()))) {
                result.add(photo);
                break;
            }
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
