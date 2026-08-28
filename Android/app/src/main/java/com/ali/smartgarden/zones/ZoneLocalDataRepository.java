package com.ali.smartgarden.zones;

import android.content.Context;

import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.journal.LocalSeasonOutcomeStore;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenNotification;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.notifications.LocalGardenNotificationStore;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.ali.smartgarden.season.SeasonRecordPolicy;

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
