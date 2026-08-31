package com.alidogukan.avora.zones;

import android.content.Context;

import com.alidogukan.avora.journal.LocalGardenEventStore;
import com.alidogukan.avora.journal.LocalSeasonOutcomeStore;
import com.alidogukan.avora.models.GardenEvent;
import com.alidogukan.avora.models.GardenNotification;
import com.alidogukan.avora.models.GardenPhoto;
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
        String expected = safe(zoneId);
        for (GardenEvent event : new LocalGardenEventStore(context).load()) {
            if (expected.equals(safe(event.getZone_id()))
                    && SeasonRecordPolicy.isFieldJournalEvent(
                    event.getType(), event.getSource(), event.getSource_key())) {
                return true;
            }
        }
        for (GardenPhoto photo : new LocalGardenPhotoStore(context).load()) {
            if (expected.equals(safe(photo.getZone_id()))) return true;
        }
        for (SeasonOutcome outcome : new LocalSeasonOutcomeStore(context).load()) {
            if (expected.equals(safe(outcome.getZone_id()))
                    && SeasonRecordPolicy.hasMeaningfulOutcome(outcome)) {
                return true;
            }
        }
        return false;
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
