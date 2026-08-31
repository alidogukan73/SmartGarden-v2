package com.alidogukan.avora.plantassistant;

import com.alidogukan.avora.models.FertilizationProfile;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.WeatherForecast;

import java.util.List;

/**
 * Explainable field screening. This class deliberately produces a likelihood,
 * never a definitive disease diagnosis and never controls irrigation or dosing.
 */
public final class PlantAssistantAdvisor {
    private PlantAssistantAdvisor() { }

    public static PlantAssistantResult assess(GardenZone zone, List<String> symptoms,
                                           String note, WeatherForecast weather,
                                           boolean hasPhoto, boolean growthStatusRequested) {
        int moisture = zone.getMoisture();
        int limit = zone.getMoisture_limit();
        boolean veryDry = moisture < Math.max(0, limit - 10);
        boolean sensorReady = zone.hasSensorData();
        boolean fertilizerDue = isFertilizerDue(zone.getFertilization());
        double temperature = value(weather == null ? null : weather.getCurrentTemperature());
        double humidity = value(weather == null ? null : weather.getCurrentHumidity());
        double rain = value(weather == null ? null : weather.getTodayRainProbability());
        double wind = value(weather == null ? null : weather.getCurrentWind());

        String context = "Toprak nemi %" + moisture + " (sınır %" + limit + ") · "
                + (sensorReady ? "sensör verisi güncel" : "sensör verisi bekleniyor")
                + weatherContext(weather)
                + " · " + (fertilizerDue ? "gübreleme planı gecikmiş" : "gübreleme planı güncel")
                + " · " + (hasPhoto ? "fotoğraf eklendi" : "fotoğraf eklenmedi");

        if (growthStatusRequested) {
            return result("Bitki gelişimi yapay zekâ ile değerlendiriliyor", "%35", "Düşük", context,
                    "Fotoğraf; bitkinin canlılığı, gelişim evresi, yaprak-gövde dengesi ve görünür stres işaretleri "
                            + "için bahçe verileriyle birlikte inceleniyor.");
        }

        if (symptoms.contains("Yaprakta leke / yanıklık")
                && (symptoms.contains("Solma") || symptoms.contains("Yaprak kuruması"))) {
            return result("Yayılım gösteren yaprak sorunu ihtimali", "%75", "Yüksek", context,
                    "Lekeli ve solan yapraklar birlikte görüldüğü için aynı bitkinin yakın plan fotoğrafını 24 saat içinde tekrar alın. "
                            + "Hızlı yayılma, küf, çürüme veya gövdede kararma varsa yerel ziraat uzmanına başvurun.");
        }

        if (symptoms.contains("Yaprakta leke / yanıklık")) {
            int score = humidity >= 70 || rain >= 50 ? 70 : 52;
            String urgency = score >= 70 ? "Orta" : "Düşük";
            return result("Yaprak hastalığı veya yanık ihtimali", percent(score), urgency, context,
                    "Lekelerin alt ve üst yapraklardaki yayılımını 3 gün izleyin. "
                            + "Yaprakları ıslatmadan sulayın; hızlı yayılma, küf veya çürüme varsa "
                            + "yakın plan fotoğrafla ziraat uzmanına danışın.");
        }

        if (symptoms.contains("Meyve çatlaması")) {
            int score = rain >= 40 || moisture > limit + 20 ? 72 : 55;
            return result("Düzensiz su alımı kaynaklı çatlama ihtimali", percent(score), "Orta", context,
                    "Sulamayı ani ve büyük değişimler yerine kısa, dengeli çevrimlerle sürdürün. "
                            + "Yağış sonrası ekstra sulama veya gübre uygulaması yapmadan önce kök bölgesini kontrol edin.");
        }

        if (symptoms.contains("Çiçek dökümü")) {
            int score = temperature >= 32 || wind >= 25 ? 68 : 48;
            return result("Sıcaklık veya çevre stresi ihtimali", percent(score), score >= 65 ? "Orta" : "Düşük", context,
                    "Öğle sıcağında işlem yapmayın. Sabah erken gözlem yapın; toprak nemini dengeli tutun. "
                            + "Çiçek kaybı artarsa fotoğrafla ve son besleme kaydıyla birlikte değerlendirin.");
        }

        if (symptoms.contains("Alt yapraklarda sararma")) {
            if (veryDry) {
                return result("Su stresi ihtimali", "%78", "Orta", context,
                        "Önce normal sulama çevriminin tamamlanmasını bekleyin. Sulama sonrası 24–48 saat gözlem yapın; "
                                + "hemen ek gübre uygulamayın.");
            }
            int score = fertilizerDue ? 68 : 52;
            return result("Besin eksikliği veya doğal yaşlanma ihtimali", percent(score), "Düşük", context,
                    "Alt yapraklardaki damar rengini ve sararmanın yeni yapraklara yayılıp yayılmadığını kaydedin. "
                            + "Gübre önerisini yalnızca ürün etiketi ve toprak/yaprak analiziyle kesinleştirin.");
        }

        if (symptoms.contains("Yaprak kuruması") || symptoms.contains("Solma")) {
            int score = veryDry || temperature >= 31 ? 72 : 48;
            return result("Su, kök veya sıcaklık stresi ihtimali", percent(score), score >= 70 ? "Orta" : "Düşük", context,
                    "Kök bölgesinde kuruluk ya da su birikmesi olmadığını kontrol edin. "
                            + "Sıcak saatlerde sulama yerine sistemin planlı çevrimini takip edin ve 24 saat sonra yeniden gözlem yapın.");
        }

        String detail = note == null || note.trim().isEmpty()
                ? "Belirti ayrıntısı girilmedi."
                : "Not: " + note.trim();
        return result("Gözlem kaydı oluşturuldu", hasPhoto ? "%35" : "%20", "Düşük", context,
                detail + " Aynı bölgeyi 3 gün sonra aynı açıdan tekrar fotoğraflayın. "
                        + "Bu sonuç destek amaçlıdır; kesin teşhis değildir.");
    }

    private static boolean isFertilizerDue(FertilizationProfile profile) {
        long now = System.currentTimeMillis() / 1000L;
        return profile != null && profile.isEnabled()
                && profile.getNext_application_at_epoch() > 0L
                && profile.getNext_application_at_epoch() <= now;
    }

    private static String weatherContext(WeatherForecast weather) {
        if (weather == null || weather.getCurrentTemperature() == null) return " · hava verisi bekleniyor";
        return " · hava " + Math.round(weather.getCurrentTemperature()) + "°C"
                + (weather.getCurrentHumidity() == null ? "" : " / nem %" + Math.round(weather.getCurrentHumidity()))
                + (weather.getTodayRainProbability() == null ? "" : " / yağış %" + Math.round(weather.getTodayRainProbability()));
    }

    private static double value(Double value) { return value == null ? -1d : value; }
    private static String percent(int value) { return "%" + value; }

    private static PlantAssistantResult result(String title, String probability, String urgency,
                                            String context, String advice) {
        return new PlantAssistantResult(title, probability, urgency, context, advice);
    }
}
