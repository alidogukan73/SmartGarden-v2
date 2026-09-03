package com.alidogukan.avora.zones;

import android.content.Context;

import com.alidogukan.avora.journal.LocalGardenEventStore;
import com.alidogukan.avora.journal.LocalSeasonOutcomeStore;
import com.alidogukan.avora.models.GardenEvent;
import com.alidogukan.avora.models.GardenNotification;
import com.alidogukan.avora.models.GardenPhoto;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.SeasonOutcome;
import com.alidogukan.avora.notifications.LocalGardenNotificationStore;
import com.alidogukan.avora.photos.LocalGardenPhotoStore;
import com.alidogukan.avora.season.SeasonRecordPolicy;

import java.util.ArrayList;
import java.util.List;

/** Owns local history checks and cleanup used by the zone removal transaction. */
public final class ZoneLocalDataRepository {
    private final Context context;

    public ZoneLocalDataRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasMeaningfulHistory(String zoneId) {
        return hasMeaningfulHistory(zoneId, 0L, false);
    }

    public boolean hasMeaningfulHistory(GardenZone zone) {
        if (zone == null) return false;
        return hasMeaningfulHistory(
                zone.getZone_id(),
                zone.getCreated_at_epoch(),
                !safe(zone.getArea_id()).isEmpty());
    }

    private boolean hasMeaningfulHistory(
            String zoneId,
            long createdAtEpoch,
            boolean modernArea
    ) {
        String expected = safe(zoneId);
        for (GardenEvent event : new LocalGardenEventStore(context).load()) {
            if (expected.equals(safe(event.getZone_id()))
                    && belongsToCurrentArea(event.getOccurred_at_epoch(), createdAtEpoch, modernArea)
                    && SeasonRecordPolicy.isFieldJournalEvent(
                    event.getType(), event.getSource(), event.getSource_key())) {
                return true;
            }
        }
        for (GardenPhoto photo : new LocalGardenPhotoStore(context).load()) {
            if (expected.equals(safe(photo.getZone_id()))
                    && belongsToCurrentArea(photo.getCaptured_at_epoch(), createdAtEpoch, modernArea)) return true;
        }
        for (SeasonOutcome outcome : new LocalSeasonOutcomeStore(context).load()) {
            if (expected.equals(safe(outcome.getZone_id()))
                    && belongsToCurrentArea(outcome.getRecorded_at_epoch(), createdAtEpoch, modernArea)
                    && SeasonRecordPolicy.hasMeaningfulOutcome(outcome)) {
                return true;
            }
        }
        return false;
    }

    private static boolean belongsToCurrentArea(long recordEpoch, long createdAtEpoch, boolean modernArea) {
        if (!modernArea) return true;
        return recordEpoch > 0L && createdAtEpoch > 0L && recordEpoch >= createdAtEpoch;
    }

    public void removeEmptyZoneData(String zoneId) {
        String expected = safe(zoneId);
        new LocalGardenEventStore(context).removeByZone(expected);
        new LocalSeasonOutcomeStore(context).removeByZone(expected);

        LocalGardenNotificationStore store = new LocalGardenNotificationStore(context);
        List<GardenNotification> matches = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (GardenNotification notification : store.load()) {
            if (notification == null
                    || !expected.equals(safe(notification.getZone_id()))
                    || safe(notification.getId()).isEmpty()) {
                continue;
            }
            matches.add(notification);
            ids.add(notification.getId());
        }
        store.rememberDeleted(matches);
        store.removeAll(ids);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
