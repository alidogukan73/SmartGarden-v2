package com.alidogukan.avora.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.alidogukan.avora.models.GardenPhoto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stores the private photo archive on this phone; it has no cloud cost. */
public class LocalGardenPhotoStore {
    private static final String PREFS = "garden_photo_archive";
    private static final String KEY_INDEX = "index";
    private final Context context;

    public LocalGardenPhotoStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public GardenPhoto save(Uri source, String zoneId, String note) throws Exception {
        return save(source, zoneId, note, "");
    }

    public GardenPhoto save(Uri source, String zoneId, String note,
                            String relatedApplicationId) throws Exception {
        String id = UUID.randomUUID().toString();
        File folder = new File(context.getFilesDir(), "garden_photos");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Photo folder could not be created");
        }
        File target = new File(folder, id + ".jpg");
        writeOptimizedImage(source, target);
        GardenPhoto photo = new GardenPhoto();
        photo.setId(id);
        photo.setZone_id(zoneId);
        photo.setLocal_path(target.getAbsolutePath());
        photo.setNote(note == null ? "" : note.trim());
        photo.setRelated_application_id(relatedApplicationId == null
                ? "" : relatedApplicationId.trim());
        photo.setCaptured_at_epoch(System.currentTimeMillis() / 1000L);
        JSONArray index = readIndex();
        JSONObject item = new JSONObject();
        item.put("id", photo.getId());
        item.put("zone_id", photo.getZone_id());
        item.put("season_id", safe(photo.getSeason_id()));
        item.put("local_path", photo.getLocal_path());
        item.put("note", photo.getNote());
        item.put("related_application_id", photo.getRelated_application_id());
        item.put("captured_at_epoch", photo.getCaptured_at_epoch());
        index.put(item);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_INDEX, index.toString()).apply();
        return photo;
    }

    /** Saves a photo captured by the in-app camera to the same private archive. */
    public GardenPhoto save(Bitmap bitmap, String zoneId, String note,
                            String relatedApplicationId) throws Exception {
        if (bitmap == null) throw new IllegalArgumentException("Photo is required");
        String id = UUID.randomUUID().toString();
        File folder = new File(context.getFilesDir(), "garden_photos");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Photo folder could not be created");
        }
        File target = new File(folder, id + ".jpg");
        try (FileOutputStream output = new FileOutputStream(target)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                throw new IllegalStateException("Photo could not be written");
            }
        }
        GardenPhoto photo = new GardenPhoto();
        photo.setId(id);
        photo.setZone_id(zoneId);
        photo.setLocal_path(target.getAbsolutePath());
        photo.setNote(note == null ? "" : note.trim());
        photo.setRelated_application_id(relatedApplicationId == null ? "" : relatedApplicationId.trim());
        photo.setCaptured_at_epoch(System.currentTimeMillis() / 1000L);
        JSONArray index = readIndex();
        JSONObject item = new JSONObject();
        item.put("id", photo.getId());
        item.put("zone_id", photo.getZone_id());
        item.put("season_id", safe(photo.getSeason_id()));
        item.put("local_path", photo.getLocal_path());
        item.put("note", photo.getNote());
        item.put("related_application_id", photo.getRelated_application_id());
        item.put("captured_at_epoch", photo.getCaptured_at_epoch());
        index.put(item);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_INDEX, index.toString()).apply();
        return photo;
    }

    public List<GardenPhoto> load() {
        List<GardenPhoto> photos = new ArrayList<>();
        JSONArray index = readIndex();
        JSONArray validIndex = new JSONArray();
        boolean cleaned = false;
        for (int i = index.length() - 1; i >= 0; i--) {
            try {
                JSONObject item = index.getJSONObject(i);
                File file = new File(item.optString("local_path"));
                if (!file.exists()) {
                    cleaned = true;
                    continue;
                }
                GardenPhoto photo = new GardenPhoto();
                photo.setId(item.optString("id"));
                photo.setZone_id(item.optString("zone_id"));
                photo.setSeason_id(item.optString("season_id"));
                photo.setLocal_path(file.getAbsolutePath());
                photo.setNote(item.optString("note"));
                photo.setRelated_application_id(
                        item.optString("related_application_id"));
                photo.setAnalysis_title(item.optString("analysis_title"));
                photo.setAnalysis_meta(item.optString("analysis_meta"));
                photo.setAnalysis_context(item.optString("analysis_context"));
                photo.setAnalysis_advice(item.optString("analysis_advice"));
                photo.setCaptured_at_epoch(item.optLong("captured_at_epoch"));
                photos.add(photo);
                validIndex.put(item);
            } catch (Exception ignored) { }
        }
        if (cleaned) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_INDEX, validIndex.toString()).apply();
        }
        return photos;
    }

    /** Attaches the final AI assessment to its already archived photo. */
    public GardenPhoto updateAnalysis(String photoId, String title, String meta,
                               String contextText, String advice) {
        if (photoId == null || photoId.isBlank()) return null;
        JSONArray index = readIndex();
        for (int i = 0; i < index.length(); i++) {
            try {
                JSONObject item = index.getJSONObject(i);
                if (!photoId.equals(item.optString("id"))) continue;
                item.put("analysis_title", safe(title));
                item.put("analysis_meta", safe(meta));
                item.put("analysis_context", safe(contextText));
                item.put("analysis_advice", safe(advice));
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_INDEX, index.toString()).apply();
                for (GardenPhoto photo : load()) {
                    if (photoId.equals(photo.getId())) return photo;
                }
                return null;
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Groups existing photos so a journal entry can hold several images. */
    public boolean updateRelatedApplicationId(String photoId, String relatedApplicationId) {
        if (photoId == null || photoId.isBlank()) return false;
        JSONArray index = readIndex();
        for (int i = 0; i < index.length(); i++) {
            try {
                JSONObject item = index.getJSONObject(i);
                if (!photoId.equals(item.optString("id"))) continue;
                item.put("related_application_id", safe(relatedApplicationId));
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_INDEX, index.toString()).apply();
                return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    /** Persists the cloud-resolved season id in the phone-only photo index. */
    public boolean updateSeasonId(String photoId, String seasonId) {
        if (photoId == null || photoId.isBlank()) return false;
        JSONArray index = readIndex();
        for (int i = 0; i < index.length(); i++) {
            try {
                JSONObject item = index.getJSONObject(i);
                if (!photoId.equals(item.optString("id"))) continue;
                item.put("season_id", safe(seasonId));
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_INDEX, index.toString()).apply();
                return true;
            } catch (Exception ignored) { }
        }
        return false;
    }
    /** Removes both the private archive record and its private phone copy. */
    public boolean delete(GardenPhoto photo) {
        if (photo == null || photo.getId() == null || photo.getId().isBlank()) {
            return false;
        }
        JSONArray current = readIndex();
        JSONArray remaining = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < current.length(); i++) {
            try {
                JSONObject item = current.getJSONObject(i);
                if (photo.getId().equals(item.optString("id"))) {
                    removed = true;
                } else {
                    remaining.put(item);
                }
            } catch (Exception ignored) { }
        }
        if (!removed) return false;
        File imageFile = new File(photo.getLocal_path());
        if (imageFile.exists() && !imageFile.delete()) return false;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_INDEX, remaining.toString()).apply();
        return true;
    }

    private JSONArray readIndex() {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_INDEX, "[]");
        try { return new JSONArray(raw); } catch (Exception ignored) { return new JSONArray(); }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** Keeps a useful plant photo while preventing full camera originals filling storage. */
    private void writeOptimizedImage(Uri source, File target) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IllegalStateException("Photo could not be read");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalArgumentException("Selected file is not a readable image");
        }
        int sample = 1;
        int largestEdge = Math.max(bounds.outWidth, bounds.outHeight);
        while (largestEdge / sample > 1600) sample *= 2;
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IllegalStateException("Photo could not be read");
            bitmap = BitmapFactory.decodeStream(input, null, decode);
        }
        if (bitmap == null) throw new IllegalStateException("Photo could not be decoded");
        try (FileOutputStream output = new FileOutputStream(target)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                throw new IllegalStateException("Photo could not be written");
            }
        } finally {
            bitmap.recycle();
        }
    }
}
