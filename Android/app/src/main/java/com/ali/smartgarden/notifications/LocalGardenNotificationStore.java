package com.ali.smartgarden.notifications;

import android.content.Context;
import com.ali.smartgarden.models.GardenNotification;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Offline-first notification storage; Firebase is used as a backup by the caller. */
public final class LocalGardenNotificationStore {
    private static final String CLOUD_KNOWN_IDS = "cloud_known_ids";
    private static final String DISMISSED_KEYS = "dismissed_source_keys";
    private static final String PREFS = "avora_notifications";
    private static final String KEY = "items";
    private final Context context;
    public LocalGardenNotificationStore(Context context) { this.context = context.getApplicationContext(); }

    public GardenNotification add(String type, String priority, String zoneId, String title, String description, String sourceKey) {
        GardenNotification value = new GardenNotification(); value.setId(UUID.randomUUID().toString()); value.setType(type); value.setPriority(priority); value.setZone_id(zoneId); value.setTitle(title); value.setDescription(description); value.setSource_key(sourceKey); value.setCreated_at_epoch(System.currentTimeMillis() / 1000L);
        JSONArray items = read(); items.put(toJson(value)); save(items); return value;
    }

    public GardenNotification addOnce(
            String type,
            String priority,
            String zoneId,
            String title,
            String description,
            String sourceKey
    ) {
        if (sourceKey != null && !sourceKey.isBlank()) {

            // Kullanıcı bu olayı daha önce sildiyse yeniden üretme.
            if (isDismissed(sourceKey)) {
                return null;
            }

            for (GardenNotification value : load()) {
                if (sourceKey.equals(value.getSource_key())) {
                    return null;
                }
            }
        }

        return add(
                type,
                priority,
                zoneId,
                title,
                description,
                sourceKey
        );
    }

    public void rememberDismissed(List<GardenNotification> values) {
        if (values == null || values.isEmpty()) return;

        Set<String> dismissed = new HashSet<>(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getStringSet(DISMISSED_KEYS, new HashSet<>())
        );

        boolean changed = false;

        for (GardenNotification value : values) {
            if (value == null) continue;

            String sourceKey = value.getSource_key();

            if (sourceKey != null && !sourceKey.isBlank()) {
                if (dismissed.add(sourceKey)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(DISMISSED_KEYS, dismissed)
                    .apply();
        }
    }

    private boolean isDismissed(String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return false;
        }

        Set<String> dismissed =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getStringSet(DISMISSED_KEYS, null);

        return dismissed != null && dismissed.contains(sourceKey);
    }
    public boolean updateState(String id, boolean read, boolean saved) {
        JSONArray items = read();
        for (int i = 0; i < items.length(); i++) try { JSONObject item = items.getJSONObject(i); if (id.equals(item.optString("id"))) { item.put("read", read); item.put("saved", saved); save(items); return true; } } catch (Exception ignored) { }
        return false;
    }

    public int removeAll(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        JSONArray current = read();
        JSONArray remaining = new JSONArray();
        int removed = 0;

        for (int i = 0; i < current.length(); i++) {
            try {
                JSONObject item = current.getJSONObject(i);
                String id = item.optString("id");

                if (ids.contains(id)) {
                    removed++;
                } else {
                    remaining.put(item);
                }
            } catch (Exception ignored) {
            }
        }

        if (removed > 0) {
            save(remaining);
        }

        return removed;
    }

    public List<GardenNotification> load() {
        List<GardenNotification> values = new ArrayList<>(); JSONArray items = read();
        for (int i = 0; i < items.length(); i++) try { values.add(fromJson(items.getJSONObject(i))); } catch (Exception ignored) { }
        Collections.sort(values, Comparator.comparingLong(GardenNotification::getCreated_at_epoch).reversed()); return values;
    }

    /** Merges Firebase backup records without discarding newer local-only alerts. */
    public int mergeFromCloud(List<GardenNotification> remoteValues) {
        if (remoteValues == null || remoteValues.isEmpty()) return 0;
        Map<String, GardenNotification> merged = new HashMap<>();
        for (GardenNotification local : load()) {
            if (local != null && !local.getId().isBlank()) merged.put(local.getId(), local);
        }
        int imported = 0;
        for (GardenNotification remote : remoteValues) {
            if (remote == null
                    || remote.getId() == null
                    || remote.getId().isBlank()) {
                continue;
            }

            String sourceKey = remote.getSource_key();

            // Kullanıcının daha önce sildiği olay Firebase'den de geri gelmesin.
            if (sourceKey != null
                    && !sourceKey.isBlank()
                    && isDismissed(sourceKey)) {
                continue;
            }

            if (!merged.containsKey(remote.getId())) {
                imported++;
            }

            merged.put(remote.getId(), remote);
        }

        JSONArray items = new JSONArray();
        List<GardenNotification> values = new ArrayList<>(merged.values());
        values.sort(Comparator.comparingLong(GardenNotification::getCreated_at_epoch).reversed());
        for (GardenNotification value : values) items.put(toJson(value));
        save(items);
        Set<String> knownCloudIds =
                new HashSet<>(
                        context.getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        ).getStringSet(
                                CLOUD_KNOWN_IDS,
                                Collections.emptySet()
                        )
                );

        for (GardenNotification remote : remoteValues) {
            if (remote != null
                    && remote.getId() != null
                    && !remote.getId().isBlank()) {

                knownCloudIds.add(remote.getId());
            }
        }

        context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                ).edit()
                .putStringSet(
                        CLOUD_KNOWN_IDS,
                        new HashSet<>(knownCloudIds)
                )
                .apply();
        return imported;
    }

    public int reconcileCloudSnapshot(
            List<GardenNotification> remoteValues
    ) {
        List<GardenNotification> localValues = load();

        Map<String, GardenNotification> merged = new HashMap<>();

        for (GardenNotification local : localValues) {
            if (local != null
                    && local.getId() != null
                    && !local.getId().isBlank()) {

                merged.put(local.getId(), local);
            }
        }

        Set<String> knownCloudIds =
                new HashSet<>(
                        context.getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        ).getStringSet(
                                CLOUD_KNOWN_IDS,
                                Collections.emptySet()
                        )
                );

        Set<String> currentCloudIds =
                new HashSet<>();

        if (remoteValues != null) {
            for (GardenNotification remote : remoteValues) {

                if (remote == null
                        || remote.getId() == null
                        || remote.getId().isBlank()) {
                    continue;
                }

                String sourceKey = remote.getSource_key();

                if (sourceKey != null
                        && !sourceKey.isBlank()
                        && isDismissed(sourceKey)) {
                    continue;
                }

                currentCloudIds.add(remote.getId());
                knownCloudIds.add(remote.getId());

                // Firebase read/saved durumu otoritedir.
                merged.put(remote.getId(), remote);
            }
        }

        int removed = 0;

        for (String id : new HashSet<>(knownCloudIds)) {

            if (!currentCloudIds.contains(id)) {

                if (merged.remove(id) != null) {
                    removed++;
                }

                knownCloudIds.remove(id);
            }
        }

        List<GardenNotification> values =
                new ArrayList<>(merged.values());

        values.sort(
                Comparator.comparingLong(
                        GardenNotification::getCreated_at_epoch
                ).reversed()
        );

        JSONArray items = new JSONArray();

        for (GardenNotification value : values) {
            items.put(toJson(value));
        }

        save(items);

        context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                ).edit()
                .putStringSet(
                        CLOUD_KNOWN_IDS,
                        new HashSet<>(knownCloudIds)
                )
                .apply();

        return removed;
    }

    private JSONObject toJson(GardenNotification v) { try { JSONObject o = new JSONObject(); o.put("id", v.getId()); o.put("type", v.getType()); o.put("priority", v.getPriority()); o.put("zone_id", v.getZone_id()); o.put("title", v.getTitle()); o.put("description", v.getDescription()); o.put("source_key", v.getSource_key()); o.put("created_at_epoch", v.getCreated_at_epoch()); o.put("read", v.isRead()); o.put("saved", v.isSaved()); return o; } catch (Exception e) { throw new IllegalStateException(e); } }
    private GardenNotification fromJson(JSONObject o) { GardenNotification v = new GardenNotification(); v.setId(o.optString("id")); v.setType(o.optString("type", "SYSTEM")); v.setPriority(o.optString("priority", "NORMAL")); v.setZone_id(o.optString("zone_id")); v.setTitle(o.optString("title")); v.setDescription(o.optString("description")); v.setSource_key(o.optString("source_key")); v.setCreated_at_epoch(o.optLong("created_at_epoch")); v.setRead(o.optBoolean("read")); v.setSaved(o.optBoolean("saved")); return v; }
    private JSONArray read() { String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"); try { return new JSONArray(raw == null ? "[]" : raw); } catch (Exception ignored) { return new JSONArray(); } }
    private void save(JSONArray items) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, items.toString()).apply(); }
}
