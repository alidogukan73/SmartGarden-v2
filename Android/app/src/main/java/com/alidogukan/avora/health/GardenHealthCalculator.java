package com.alidogukan.avora.health;

import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.ZoneIrrigationStatus;
import com.alidogukan.avora.plantassistant.PlantAssistantHealthSignal;

import java.util.List;

/** Conservative and explainable; this score never controls hardware. */
public final class GardenHealthCalculator {
    private GardenHealthCalculator() { }

    public static GardenHealthSummary calculate(List<GardenZone> zones, long now) {
        return calculate(zones, now, null);
    }

    public static GardenHealthSummary calculate(
            List<GardenZone> zones,
            long now,
            PlantAssistantHealthSignal assistantSignal
    ) {
        if (zones == null || zones.isEmpty()) {
            return new GardenHealthSummary(0, "Bahçe verisi bekleniyor",
                    "Bölgeler bağlandığında sağlık özeti hazırlanır.");
        }
        int total = 0;
        int count = 0;
        String priority = "";
        for (GardenZone zone : zones) {
            if (zone == null || !zone.isEnabled()) continue;
            GardenHealthZoneResult result = evaluateZone(zone, now, assistantSignal);
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
                : average >= 65 ? "Bahçede uyarı var"
                : "Bahçe kontrolü öneriliyor";
        String detail = priority.isEmpty()
                ? count + " aktif bölgenin nem, sensör ve gübreleme planı uygun görünüyor"
                : priority;
        return new GardenHealthSummary(average, title, detail);
    }

    public static GardenHealthZoneResult evaluateZone(GardenZone zone, long now) {
        return evaluateZone(zone, now, null);
    }

    public static GardenHealthZoneResult evaluateZone(
            GardenZone zone,
            long now,
            PlantAssistantHealthSignal assistantSignal
    ) {
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
        if (assistantSignal != null && assistantSignal.isRecent(now)
                && zone.getZone_id() != null
                && zone.getZone_id().equals(assistantSignal.getZoneId())) {
            String urgency = assistantSignal.getUrgency();
            if ("Yüksek".equalsIgnoreCase(urgency)) {
                score -= 25;
                add(reason, "Bitki Asistanı: yüksek aciliyet");
            } else if ("Orta".equalsIgnoreCase(urgency)) {
                score -= 12;
                add(reason, "Bitki Asistanı: orta aciliyet");
            } else if (!urgency.isEmpty()) {
                score -= 3;
                add(reason, "Bitki Asistanı gözlem önerisi var");
            }
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
