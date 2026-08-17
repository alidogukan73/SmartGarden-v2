package com.ali.smartgarden.fertilization;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ali.smartgarden.BuildConfig;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.plantassistant.PlantAssistantVisionClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Requests data-minimized, advisory-only organic product guidance.
 * No sensor, location, weather, area, tank, inventory or stock data is sent.
 */
public final class OrganicFertilizerAiAdvisor {
    private static final String ENDPOINT = PlantAssistantVisionClient.BASE_URL
            + "/v1/fertilizer-assistant/organic-alternatives";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Result> CACHE = new ConcurrentHashMap<>();

    private OrganicFertilizerAiAdvisor() { }

    public interface Callback {
        void onResult(Result result);
        void onUnavailable();
    }

    public static boolean isRequired(FertilizerAdvice advice) {
        return advice != null
                && "ORGANİK ÜRÜN GEREKİYOR".equals(advice.getStatus());
    }

    public static void request(GardenZone zone, Callback callback) {
        if (zone == null || callback == null) return;
        FertilizationProfile profile = zone.getFertilization();
        String plantType = safe(zone.getPlant_type());
        String stage = profile == null ? "" : safe(profile.getGrowth_stage());
        String key = plantType + '|' + stage;
        Result cached = CACHE.get(key);
        if (cached != null) {
            MAIN.post(() -> callback.onResult(cached));
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                JSONObject context = new JSONObject();
                context.put("plant_type", plantType);
                context.put("growth_stage", stage);
                context.put("application_method", "DRIP_IRRIGATION");
                context.put("organic_only", true);
                context.put("deterministic_result", "NO_COMPATIBLE_ORGANIC_PRODUCT");
                Result result = Result.from(post(context));
                CACHE.put(key, result);
                MAIN.post(() -> callback.onResult(result));
            } catch (Exception ignored) {
                MAIN.post(callback::onUnavailable);
            }
        });
    }

    private static JSONObject post(JSONObject context) throws Exception {
        JSONObject request = new JSONObject();
        request.put("context", context);
        byte[] payload = request.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-SmartGarden-Token", BuildConfig.PLANT_VISION_TOKEN);
        connection.getOutputStream().write(payload);
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        byte[] response = readAll(stream);
        JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(json.optString("error", "AI_ADVICE_UNAVAILABLE"));
        }
        return json;
    }

    private static byte[] readAll(InputStream stream) throws Exception {
        if (stream == null) return new byte[0];
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        private final String headline;
        private final String rationale;
        private final JSONArray recommendations;
        private final JSONArray cautions;
        private final String disclaimer;

        private Result(String headline, String rationale, JSONArray recommendations,
                       JSONArray cautions, String disclaimer) {
            this.headline = headline;
            this.rationale = rationale;
            this.recommendations = recommendations;
            this.cautions = cautions;
            this.disclaimer = disclaimer;
        }

        private static Result from(JSONObject json) {
            JSONArray recommendations = json.optJSONArray("recommendations");
            JSONArray cautions = json.optJSONArray("cautions");
            return new Result(
                    safe(json.optString("headline")),
                    safe(json.optString("rationale")),
                    recommendations == null ? new JSONArray() : recommendations,
                    cautions == null ? new JSONArray() : cautions,
                    safe(json.optString("disclaimer"))
            );
        }

        public String compactText() {
            StringBuilder text = new StringBuilder();
            appendLine(text, headline);
            if (recommendations.length() > 0) {
                JSONObject first = recommendations.optJSONObject(0);
                if (first != null) {
                    appendLine(text, first.optString("product_type"));
                    appendLine(text, first.optString("purpose"));
                }
            } else {
                appendLine(text, rationale);
            }
            return text.toString();
        }

        public String fullText(Context context) {
            StringBuilder text = new StringBuilder();
            appendLine(text, headline);
            appendLine(text, rationale);
            if (recommendations.length() > 0) {
                appendLine(text, context.getString(
                        R.string.fertilizer_organic_ai_recommended_profiles));
            }
            for (int index = 0; index < recommendations.length(); index++) {
                JSONObject item = recommendations.optJSONObject(index);
                if (item == null) continue;
                String type = safe(item.optString("product_type"));
                String purpose = safe(item.optString("purpose"));
                String criteria = safe(item.optString("selection_criteria"));
                String method = safe(item.optString("application_method"));
                appendLine(text, "• " + type + (purpose.isEmpty() ? "" : " — " + purpose));
                if (!criteria.isEmpty()) {
                    appendLine(text, context.getString(
                            R.string.fertilizer_organic_ai_verify_label,
                            criteria));
                }
                if (!method.isEmpty()) {
                    appendLine(text, context.getString(
                            R.string.fertilizer_organic_ai_application,
                            method));
                }
            }
            if (cautions.length() > 0) {
                appendLine(text, context.getString(
                        R.string.fertilizer_organic_ai_cautions));
            }
            for (int index = 0; index < cautions.length(); index++) {
                appendLine(text, "• " + cautions.optString(index));
            }
            appendLine(text, disclaimer);
            return text.toString();
        }

        private static void appendLine(StringBuilder target, String value) {
            String clean = safe(value);
            if (clean.isEmpty()) return;
            if (target.length() > 0) target.append('\n');
            target.append(clean);
        }
    }
}