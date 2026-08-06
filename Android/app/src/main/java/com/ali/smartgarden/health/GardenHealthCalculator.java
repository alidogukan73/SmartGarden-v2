package com.ali.smartgarden.health;

import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;

import java.util.List;

/** Conservative and explainable; this score never controls hardware. */
public final class GardenHealthCalculator {
    private GardenHealthCalculator() { }

    public static GardenHealthSummary calculate(List<GardenZone> zones, long now) {
        if (zones == null || zones.isEmpty()) {
            return new GardenHealthSummary(0, "Bahçe verisi bekleniyor",
                    "Bölgeler bağlandığında sağlık özeti hazırlanır.");
        }
        int total = 0;
        int count = 0;
        String priority = "";
        for (GardenZone zone : zones) {
            if (zone == null || !zone.isEnabled()) continue;
            GardenHealthZoneResult result = evaluateZone(zone, now);
            total += result.getScore();
            count++;
            if (priority.isEmpty() && result.getScore() < 100) {
                priority = safeName(zone) + " · " + result.getReason();
            }
        }
        if (count == 0) {
            return new GardenHealthSummary(0, "Aktif bölge yok",
                    "Sağlık özeti için en az bir aktif bölge gerekir.");
        }
        int average = Math.round((float) total / count);
        String title = average >= 85 ? "Bahçe genel olarak iyi durumda"
                : average >= 65 ? "Bahçede dikkat gerektiren noktalar var"
                : "Bahçe kontrolü öneriliyor";
        String detail = priority.isEmpty()
                ? count + " aktif bölgenin nem, sensör ve gübreleme planı uygun görünüyor"
                : priority;
        return new GardenHealthSummary(average, title, detail);
    }

    public static GardenHealthZoneResult evaluateZone(GardenZone zone, long now) {
        if (zone == null) return new GardenHealthZoneResult(0, "Bölge verisi yok");
        if (!zone.isSensor_enabled()) return new GardenHealthZoneResult(55, "Sensör devre dışı");
        if (!zone.hasSensorData()) return new GardenHealthZoneResult(55, "Sensör verisi bekleniyor");

        int score = 100;
        StringBuilder reason = new StringBuilder();
        long age = Math.max(0L, now - zone.getUpdated_at_epoch());
        if (age > 15 * 60L) {
            score -= 30;
            add(reason, "Sensör verisi güncel değil");
        }
        if (zone.getMoisture() < zone.getMoisture_limit()) {
            score -= Math.min(35, 10 + zone.getMoisture_limit() - zone.getMoisture());
            add(reason, "Nem düşük: %" + zone.getMoisture()
                    + " / sınır %" + zone.getMoisture_limit());
        }
        ZoneIrrigationStatus irrigation = zone.getIrrigation_status();
        if (irrigation != null && !irrigation.isSensor_stable()) {
            score -= 20;
            add(reason, "Sensör ölçümü kararsız");
        }
        FertilizationProfile profile = zone.getFertilization();
        if (profile != null && profile.isEnabled()
                && profile.getNext_application_at_epoch() > 0
                && profile.getNext_application_at_epoch() <= now) {
            score -= 10;
            add(reason, "Gübreleme kaydı bekleniyor");
        }
        if (reason.length() == 0) reason.append("Nem, sensör ve gübreleme planı uygun görünüyor");
        return new GardenHealthZoneResult(score, reason.toString());
    }

    private static void add(StringBuilder target, String text) {
        if (target.length() > 0) target.append(" · ");
        target.append(text);
    }

    private static String safeName(GardenZone zone) {
        return zone.getName() == null || zone.getName().isBlank() ? "Bölge" : zone.getName();
    }
}
