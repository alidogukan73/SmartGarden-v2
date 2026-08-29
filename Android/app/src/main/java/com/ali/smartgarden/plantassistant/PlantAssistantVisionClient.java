package com.ali.smartgarden.plantassistant;

import android.graphics.Bitmap;
import android.util.Base64;

import com.ali.smartgarden.security.AppCheckRequestAuthenticator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** LAN client. The Gemini key remains only on the Raspberry Pi. */
public final class PlantAssistantVisionClient {
    private PlantAssistantVisionClient() { }
    // Use the Raspberry Pi's LAN address: Android devices do not reliably resolve .local names.
    /** Tailscale özel ağı üzerinden Raspberry Pi'deki görsel analiz servisi. */
    public static final String BASE_URL = "http://100.97.32.111:8787";
    public static final String ENDPOINT = BASE_URL + "/v1/plant-assistant/analyze";

    public static JSONObject analyze(Bitmap bitmap, JSONObject context) throws Exception {
        bitmap = scaledForUpload(bitmap);
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, image)) throw new IllegalStateException("PHOTO_ENCODE_FAILED");
        JSONObject request = new JSONObject();
        request.put("mime_type", "image/jpeg");
        request.put("image_base64", Base64.encodeToString(image.toByteArray(), Base64.NO_WRAP));
        request.put("context", context);
        byte[] payload = request.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        AppCheckRequestAuthenticator.authorize(connection);
        connection.getOutputStream().write(payload);
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        byte[] response = readAll(stream);
        JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
        if (status < 200 || status >= 300) throw new IllegalStateException(json.optString("error", "VISION_UNAVAILABLE"));
        return json;
    }

    public static String list(JSONArray values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; values != null && i < values.length(); i++) {
            if (builder.length() > 0) builder.append("\n");
            builder.append("• ").append(values.optString(i));
        }
        return builder.toString();
    }

    /** Reads responses on every supported Android version (API 26+). */
    private static byte[] readAll(InputStream stream) throws Exception {
        if (stream == null) return new byte[0];
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
    private static Bitmap scaledForUpload(Bitmap bitmap) {
        int longestSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longestSide <= 1600) return bitmap;
        float scale = 1600f / longestSide;
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
    }
}
