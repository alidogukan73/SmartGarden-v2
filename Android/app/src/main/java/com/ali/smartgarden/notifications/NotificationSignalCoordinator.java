package com.ali.smartgarden.notifications;

import android.content.Context;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.WateringHistory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Converts meaningful weather, device and watering changes into deduplicated AVORA alerts. */
public final class NotificationSignalCoordinator {
    private NotificationSignalCoordinator() { }

    public static void evaluateWeather(Context context, Double temperature, Double rain,
                                       Double wind, String forecastDate) {
        double temp = value(temperature);
        double rainfall = value(rain);
        double windSpeed = value(wind);
        String date = forecastDate == null || forecastDate.isBlank()
                ? LocalDate.now().toString() : forecastDate;
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        if (rainfall >= 60D) {
            notifications.publishOnce("WEATHER", "HIGH", "", "Yağış riski yüksek",
                    "Tahminde %" + Math.round(rainfall)
                            + " yağış olasılığı var. Sulama öncesi toprak nemini yeniden kontrol edin.",
                    "weather:rain:" + date);
        } else if (temp >= 35D) {
            notifications.publishOnce("WEATHER", "NORMAL", "", "Yüksek sıcaklık uyarısı",
                    "Tahminde " + Math.round(temp)
                            + "°C görülüyor. Toprak nemini ve yapraklarda solmayı takip edin.",
                    "weather:heat:" + date);
        } else if (windSpeed >= 30D) {
            notifications.publishOnce("WEATHER", "NORMAL", "", "Kuvvetli rüzgâr uyarısı",
                    "Rüzgâr yaklaşık " + Math.round(windSpeed)
                            + " km/sa. Toprak nemi daha hızlı düşebilir.",
                    "weather:wind:" + date);
        }
    }

    public static void evaluateDevice(Context context, Status status, Health health) {
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        String date = LocalDate.now().toString();
        if (status != null && !status.isOnline()) {
            notifications.publishOnce("DEVICE", "HIGH", "", "Raspberry Pi bağlantısı yok",
                    "Bahçe cihazı çevrimdışı görünüyor. Enerji ve ağ bağlantısını kontrol edin.",
                    "device:offline:" + date);
            return;
        }
        if (status != null && status.getLastError() != null
                && !status.getLastError().isBlank()) {
            notifications.publishOnce("DEVICE", "HIGH", "", "Bahçe cihazı uyarısı",
                    status.getLastError(),
                    "device:error:" + status.getLastError().hashCode() + ":" + date);
        }
        if (health != null && (health.isUnderVoltageNow() || health.isThrottledNow()
                || health.isFrequencyCappedNow() || health.getCpuTemperature() >= 80D
                || health.getDiskUsage() >= 90D)) {
            notifications.publishOnce("DEVICE", "HIGH", "", "Raspberry Pi kaynak uyarısı",
                    "Cihazda güç, sıcaklık veya kaynak sınırı algılandı. "
                            + "Cihaz Sağlığı ekranından ayrıntıları inceleyin.",
                    "device:health:" + date);
        }
    }

    /** Sends only newly completed cycles; opening a journal never replays old watering alerts. */
    public static void evaluateWatering(Context context, List<WateringHistory> records) {
        if (records == null
                || !new NotificationSettingsStore(context).isReminderEnabled("irrigation")) {
            return;
        }
        long now = System.currentTimeMillis();
        GardenNotificationManager notifications = new GardenNotificationManager(context);
        for (WateringHistory record : records) {
            if (record == null || !record.isCompleted()) continue;
            long completedAt = parseTime(record.getFinishedAt());
            if (completedAt <= 0L || now < completedAt
                    || now - completedAt > 20L * 60L * 1000L) continue;
            String id = record.getRecordId() == null || record.getRecordId().isBlank()
                    ? record.getFinishedAt() : record.getRecordId();
            String zoneId = record.getZoneId() == null ? "" : record.getZoneId();
            notifications.publishOnce("IRRIGATION", "NORMAL", zoneId, "Sulama tamamlandı",
                    "Sulama süresi: " + record.getDuration() + " sn.",
                    "watering:" + zoneId + ":" + id);
        }
    }

    private static long parseTime(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        try {
            return LocalDateTime.parse(raw).atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static double value(Double number) {
        return number == null ? 0D : number;
    }
}