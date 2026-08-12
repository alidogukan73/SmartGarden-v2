package com.ali.smartgarden.plantassistant;

import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;

import java.util.List;

/**
 * Produces the short, advisory-only recommendation shown on the home screen.
 * It deliberately never diagnoses a disease or controls irrigation; it only
 * turns the latest garden signals into a clear observation prompt.
 */
public final class PlantAssistantHomeRecommendation {
    private static final long RECENT_ANALYSIS_SECONDS = 3L * 24L * 60L * 60L;

    private PlantAssistantHomeRecommendation() { }

    public static String create(
            List<GardenZone> zones,
            WeatherForecast weather,
            PlantAssistantHealthSignal recentAnalysis,
            long nowEpoch
    ) {
        if (zones == null || zones.isEmpty()) {
            return "Bahçe bölgesi bekleniyor. Bitki önerisi için bir bölge ekleyin.";
        }

        GardenZone analyzedZone = findZone(zones, recentAnalysis == null ? "" : recentAnalysis.getZoneId());
        if (hasActionableRecentAnalysis(recentAnalysis, nowEpoch)) {
            String zoneName = zoneName(analyzedZone);
            String title = clean(recentAnalysis.getTitle());
            return zoneName + " için son analiz: "
                    + (title.isEmpty() ? "yaprakları tekrar kontrol edin" : title);
        }

        GardenZone missingSensor = firstMissingSensor(zones);
        if (missingSensor != null) {
            return zoneName(missingSensor)
                    + " için güncel sensör verisi yok. Yapraklarda solma veya kuruma kontrolü öneriliyor.";
        }

        GardenZone criticalDry = firstDryZone(zones, 15);
        if (criticalDry != null) {
            return zoneName(criticalDry) + " için su stresi riski var: nem %"
                    + criticalDry.getMoisture() + ", sınır %" + criticalDry.getMoisture_limit()
                    + ". Yapraklarda solma ve kuruma kontrolü öneriliyor.";
        }

        GardenZone dryInHeat = firstDryZone(zones, 5);
        Double heat = hottestUpcomingTemperature(weather);
        if (dryInHeat != null && heat != null && heat >= 32D) {
            return zoneName(dryInHeat) + " için nem %" + dryInHeat.getMoisture()
                    + "; sıcaklık " + Math.round(heat)
                    + "°C bekleniyor. Yapraklarda sıcaklık stresi kontrolü öneriliyor.";
        }

        if (heat != null && heat >= 38D) {
            GardenZone zone = firstActiveZone(zones);
            return Math.round(heat) + "°C sıcaklık bekleniyor. " + zoneName(zone)
                    + " için öğle saatlerinde solma ve yaprak yanığı gözlemi öneriliyor.";
        }

        return "Şu an kritik bir bitki uyarısı yok. Bu hafta gelişim fotoğrafı ekleyerek görsel takibi sürdürün.";
    }

    private static boolean hasActionableRecentAnalysis(PlantAssistantHealthSignal signal, long nowEpoch) {
        if (signal == null || signal.getTitle().isBlank() || !signal.isRecent(nowEpoch)) return false;
        long age = nowEpoch - signal.getCreatedAtEpoch();
        if (age > RECENT_ANALYSIS_SECONDS) return false;
        String urgency = signal.getUrgency().trim().toLowerCase();
        return urgency.equals("orta") || urgency.equals("yüksek") || urgency.equals("acil");
    }

    private static GardenZone firstMissingSensor(List<GardenZone> zones) {
        for (GardenZone zone : zones) {
            if (isActive(zone) && zone.isSensor_enabled() && !zone.hasSensorData()) return zone;
        }
        return null;
    }

    private static GardenZone firstDryZone(List<GardenZone> zones, int deficit) {
        GardenZone result = null;
        int biggestDeficit = 0;
        for (GardenZone zone : zones) {
            if (!isActive(zone) || !zone.isSensor_enabled() || !zone.hasSensorData()) continue;
            int currentDeficit = zone.getMoisture_limit() - zone.getMoisture();
            if (currentDeficit >= deficit && currentDeficit > biggestDeficit) {
                result = zone;
                biggestDeficit = currentDeficit;
            }
        }
        return result;
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
