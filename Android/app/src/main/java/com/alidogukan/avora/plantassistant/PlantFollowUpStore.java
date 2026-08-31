package com.alidogukan.avora.plantassistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Keeps one lightweight follow-up task per plant-assistant analysis on this phone. */
public final class PlantFollowUpStore {
    private static final String PREFS = "plant_assistant_followups";
    private static final String KEY = "items";
    private final Context context;

    public PlantFollowUpStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public Result registerAnalysis(String zoneId, String photoId, String title) {
        if (zoneId == null || zoneId.isBlank() || photoId == null || photoId.isBlank()) {
            return Result.none();
        }
        JSONArray items = read();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (photoId.equals(item.optString("analysis_photo_id"))) {
                    return item.optBoolean("completed", false)
                            ? Result.none() : Result.existing(item.optLong("due_at_epoch"));
                }
            }
            long now = System.currentTimeMillis() / 1000L;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (zoneId.equals(item.optString("zone_id"))
                        && !item.optBoolean("completed", false)) {
                    item.put("completed", true);
                    item.put("completed_photo_id", photoId);
                    item.put("completed_at_epoch", now);
                    save(items);
                    return Result.completed(item.optString("title"));
                }
            }
            JSONObject task = new JSONObject();
            task.put("zone_id", zoneId);
            task.put("title", title == null ? "" : title);
            task.put("analysis_photo_id", photoId);
            task.put("created_at_epoch", now);
            task.put("due_at_epoch", now + 3 * 24 * 60 * 60L);
            task.put("completed", false);
            task.put("notified", false);
            items.put(task);
            save(items);
            return Result.scheduled(task.optLong("due_at_epoch"));
        } catch (Exception ignored) {
            return Result.none();
        }
    }

    /** Returns only tasks whose three-day waiting period has ended. */
    public List<DueTask> dueUnnotified(long nowEpochSeconds) {
        ArrayList<DueTask> due = new ArrayList<>();
        JSONArray items = read();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                long dueAt = item.optLong("due_at_epoch", 0L);
                if (!item.optBoolean("completed", false)
                        && !item.optBoolean("notified", false)
                        && dueAt > 0L && dueAt <= nowEpochSeconds) {
                    due.add(new DueTask(item.optString("analysis_photo_id"),
                            item.optString("zone_id"), item.optString("title"), dueAt));
                }
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return due;
    }

    public void markNotified(String analysisPhotoId) {
        if (analysisPhotoId == null || analysisPhotoId.isBlank()) return;
        JSONArray items = read();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (analysisPhotoId.equals(item.optString("analysis_photo_id"))) {
                    item.put("notified", true);
                    item.put("notified_at_epoch", System.currentTimeMillis() / 1000L);
                    save(items);
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    private JSONArray read() {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private void save(JSONArray items) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, items.toString()).apply();
    }

    public static final class DueTask {
        public final String photoId;
        public final String zoneId;
        public final String title;
        public final long dueAtEpoch;

        DueTask(String photoId, String zoneId, String title, long dueAtEpoch) {
            this.photoId = photoId == null ? "" : photoId;
            this.zoneId = zoneId == null ? "" : zoneId;
            this.title = title == null ? "" : title;
            this.dueAtEpoch = dueAtEpoch;
        }
    }

    public static final class Result {
        public final String type;
        public final long dueAtEpoch;
        public final String previousTitle;

        private Result(String type, long dueAtEpoch, String previousTitle) {
            this.type = type;
            this.dueAtEpoch = dueAtEpoch;
            this.previousTitle = previousTitle;
        }

        static Result scheduled(long dueAtEpoch) {
            return new Result("SCHEDULED", dueAtEpoch, "");
        }

        static Result existing(long dueAtEpoch) {
            return new Result("SCHEDULED_EXISTING", dueAtEpoch, "");
        }

        static Result completed(String previousTitle) {
            return new Result("COMPLETED", 0L, previousTitle == null ? "" : previousTitle);
        }

        static Result none() {
            return new Result("NONE", 0L, "");
        }
    }
}