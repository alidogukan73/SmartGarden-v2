package com.alidogukan.avora.plantassistant;

import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.WeatherForecast;

import java.util.List;
import java.util.Locale;

/**
 * Produces the short, advisory-only recommendation shown on the home screen.
 * It deliberately never diagnoses a disease or controls irrigation; it only
 * turns the latest garden signals into a clear observation prompt.
 */
public final class PlantAssistantHomeRecommendation {
    private static final long RECENT_ANALYSIS_SECONDS = 3L * 24L * 60L * 60L;
    private static final long CURRENT_SENSOR_SECONDS = 90L;

    public enum Level {
        NORMAL,
        FOLLOW_UP,
        WARNING
    }

    public static final class Recommendation {
        private final String message;
        private final Level level;

        private Recommendation(String message, Level level) {
            this.message = message;
            this.level = level;
        }

        public String getMessage() { return message; }
        public Level getLevel() { return level; }
    }

    private PlantAssistantHomeRecommendation() { }

    public static String create(
            List<GardenZone> zones,
            WeatherForecast weather,
            PlantAssistantHealthSignal recentAnalysis,
            long nowEpoch
    ) {
        return evaluate(zones, weather, recentAnalysis, nowEpoch).getMessage();
    }

    public static Recommendation evaluate(
            List<GardenZone> zones,
            WeatherForecast weather,
            PlantAssistantHealthSignal recentAnalysis,
            long nowEpoch
    ) {
        if (zones == null || zones.isEmpty()) {
            return recommendation(
                    "Bahçe bölgesi bekleniyor. Bitki önerisi için bir bölge ekleyin.",
                    Level.FOLLOW_UP
            );
        }

        GardenZone analyzedZone = findZone(
                zones,
                recentAnalysis == null ? "" : recentAnalysis.getZoneId()
        );
        if (hasActionableRecentAnalysis(recentAnalysis, nowEpoch)) {
            String zoneName = zoneName(analyzedZone);
            String title = clean(recentAnalysis.getTitle());
            return recommendation(
                    zoneName + " için son analiz: "
                            + (title.isEmpty() ? "yaprakları tekrar kontrol edin" : title)
                            + ". Takip gözlemini ihmal etmeyin.",
                    isHighUrgency(recentAnalysis) ? Level.WARNING : Level.FOLLOW_UP
            );
        }

        GardenZone criticalDry = firstDryZone(zones, 15, nowEpoch);
        if (criticalDry != null) {
            return recommendation(
                    zoneName(criticalDry) + " için su stresi riski var: nem %"
                            + criticalDry.getMoisture() + ", sınır %"
                            + criticalDry.getMoisture_limit()
                            + ". Yapraklarda solma ve kuruma kontrolü öneriliyor.",
                    Level.WARNING
            );
        }

        GardenZone dryInHeat = firstDryZone(zones, 5, nowEpoch);
        Double heat = hottestUpcomingTemperature(weather);
        if (dryInHeat != null && heat != null && heat >= 32D) {
            return recommendation(
                    zoneName(dryInHeat) + " için nem %" + dryInHeat.getMoisture()
                            + "; sıcaklık " + Math.round(heat)
                            + "°C bekleniyor. Yapraklarda sıcaklık stresi kontrolü öneriliyor.",
                    Level.WARNING
            );
        }

        GardenZone missingSensor = firstMissingSensor(zones, nowEpoch);
        if (missingSensor != null) {
            return recommendation(
                    zoneName(missingSensor)
                            + " için güncel sensör verisi yok. Sensörü ve yapraklarda solma veya kuruma olup olmadığını kontrol edin.",
                    Level.FOLLOW_UP
            );
        }

        if (heat != null && heat >= 38D) {
            GardenZone zone = firstActiveZone(zones);
            return recommendation(
                    Math.round(heat) + "°C sıcaklık bekleniyor. " + zoneName(zone)
                            + " için öğle saatlerinde solma ve yaprak yanığı gözlemi öneriliyor.",
                    Level.FOLLOW_UP
            );
        }

        return recommendation(
                "Şu an kritik bir bitki uyarısı yok. Bu hafta gelişim fotoğrafı ekleyerek görsel takibi sürdürün.",
                Level.NORMAL
        );
    }

    private static Recommendation recommendation(String message, Level level) {
        return new Recommendation(message, level);
    }

    private static boolean hasActionableRecentAnalysis(
            PlantAssistantHealthSignal signal,
            long nowEpoch
    ) {
        if (signal == null || signal.getTitle().isBlank() || !signal.isRecent(nowEpoch)) return false;
        long age = nowEpoch - signal.getCreatedAtEpoch();
        if (age > RECENT_ANALYSIS_SECONDS) return false;
        String urgency = signal.getUrgency().trim().toLowerCase(Locale.ROOT);
        return urgency.equals("orta") || urgency.equals("yüksek") || urgency.equals("acil");
    }

    private static boolean isHighUrgency(PlantAssistantHealthSignal signal) {
        if (signal == null) return false;
        String urgency = signal.getUrgency().trim().toLowerCase(Locale.ROOT);
        return urgency.equals("yüksek") || urgency.equals("acil");
    }

    private static GardenZone firstMissingSensor(List<GardenZone> zones, long nowEpoch) {
        for (GardenZone zone : zones) {
            if (isActive(zone)
                    && zone.isSensor_enabled()
                    && !hasCurrentSensorData(zone, nowEpoch)) {
                return zone;
            }
        }
        return null;
    }

    private static GardenZone firstDryZone(
            List<GardenZone> zones,
            int deficit,
            long nowEpoch
    ) {
        GardenZone result = null;
        int biggestDeficit = 0;
        for (GardenZone zone : zones) {
            if (!isActive(zone)
                    || !zone.isSensor_enabled()
                    || !hasCurrentSensorData(zone, nowEpoch)) {
                continue;
            }
            int currentDeficit = zone.getMoisture_limit() - zone.getMoisture();
            if (currentDeficit >= deficit && currentDeficit > biggestDeficit) {
                result = zone;
                biggestDeficit = currentDeficit;
            }
        }
        return result;
    }

    private static boolean hasCurrentSensorData(GardenZone zone, long nowEpoch) {
        if (zone == null || !zone.hasSensorData()) return false;
        long age = Math.max(0L, nowEpoch - zone.getUpdated_at_epoch());
        return age <= CURRENT_SENSOR_SECONDS;
    }

    private static GardenZone firstActiveZone(List<GardenZone> zones) {
        for (GardenZone zone : zones) if (isActive(zone)) return zone;
        return zones.get(0);
    }

    private static GardenZone findZone(List<GardenZone> zones, String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return null;
        for (GardenZone zone : zones) {
            if (zoneId.equals(zone.getZone_id())) return zone;
        }
        return null;
    }

    private static Double hottestUpcomingTemperature(WeatherForecast weather) {
        if (weather == null) return null;
        Double today = weather.getTodayTemperatureMax();
        Double tomorrow = weather.getTomorrowTemperatureMax();
        if (today == null) return tomorrow;
        if (tomorrow == null) return today;
        return Math.max(today, tomorrow);
    }

    private static boolean isActive(GardenZone zone) {
        return zone != null && zone.isEnabled();
    }

    private static String zoneName(GardenZone zone) {
        if (zone == null || clean(zone.getName()).isEmpty()) return "Bu bölge";
        String emoji = clean(zone.getEmoji());
        return (emoji.isEmpty() ? "" : emoji + " ") + clean(zone.getName());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}