package com.ali.smartgarden.activities;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.health.GardenHealthCalculator;
import com.ali.smartgarden.health.GardenHealthSummary;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.journal.LocalSeasonOutcomeStore;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenSeason;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.models.SeasonStatus;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.models.ZoneSeasonState;
import com.ali.smartgarden.season.SeasonRepository;
import com.ali.smartgarden.season.SeasonScope;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shows one immutable season at a time; it never mixes records from other seasons. */
public class SeasonReportActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final SeasonRepository seasonRepository = new SeasonRepository();
    private TextView reportView;
    private TextView outcomeSummary;
    private TextView scopeView;
    private TextView comparisonView;
    private List<GardenZone> zones = new ArrayList<>();
    private List<GardenSeason> seasons = new ArrayList<>();
    private List<FertilizerApplication> fertilizerHistory = new ArrayList<>();
    private List<WateringHistory> wateringHistory = new ArrayList<>();
    private WeatherForecast weather;
    private String reportText = "Rapor hazırlanıyor...";
    private LocalSeasonOutcomeStore seasonOutcomeStore;
    private LocalGardenEventStore gardenEventStore;
    private List<SeasonOutcome> seasonOutcomes = new ArrayList<>();
    private String selectedZoneId = "";
    private String selectedSeasonId = "";

    private final ActivityResultLauncher<String> pdfCreator = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), this::writePdf);

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_season_report);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        reportView = findViewById(R.id.txtSeasonReport);
        outcomeSummary = findViewById(R.id.txtSeasonOutcomeSummary);
        scopeView = findViewById(R.id.txtSeasonScope);
        comparisonView = findViewById(R.id.txtSeasonComparison);
        selectedZoneId = safe(getIntent().getStringExtra("zone_id"));
        selectedSeasonId = safe(getIntent().getStringExtra("season_id"));
        seasonOutcomeStore = new LocalSeasonOutcomeStore(this);
        gardenEventStore = new LocalGardenEventStore(this);
        syncSeasonBackup();

        scopeView.setOnClickListener(view -> showSeasonSelector());
        findViewById(R.id.btnAddSeasonOutcome).setOnClickListener(view -> {
            Intent intent = new Intent(this, SeasonManagementActivity.class);
            if (!selectedZoneId.isBlank()) intent.putExtra("zone_id", selectedZoneId);
            startActivity(intent);
        });
        findViewById(R.id.btnExportSeasonPdf).setOnClickListener(view -> {
            GardenSeason season = selectedSeason();
            String suffix = season == null ? "Sezon-Raporu" : safeFileName(season.getZone_name() + "-" + season.getLabel());
            pdfCreator.launch("AVORA-" + suffix + "-" + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()) + ".pdf");
        });

        repository.observeGardenZones().observe(this, value -> {
            zones = value == null ? new ArrayList<>() : value;
            ensureSelectedSeason();
            render();
        });
        repository.observeFertilizerHistory().observe(this, value -> {
            fertilizerHistory = value == null ? new ArrayList<>() : value;
            render();
        });
        repository.observeWateringHistory().observe(this, value -> {
            wateringHistory = value == null ? new ArrayList<>() : value;
            render();
        });
        repository.observeWeatherForecast().observe(this, value -> {
            weather = value;
            render();
        });
        seasonRepository.observeAllSeasons().observe(this, value -> {
            seasons = value == null ? new ArrayList<>() : value;
            ensureSelectedSeason();
            render();
        });
    }

    private void render() {
        GardenSeason season = selectedSeason();
        if (season == null) {
            scopeView.setText("Raporlanacak sezon seçilmedi");
            outcomeSummary.setText("Önce Sezon Yönetimi ekranından bir sezon başlatın.");
            comparisonView.setText("Geçmiş sezonlar oluştuğunda burada karşılaştırılabilir.");
            reportText = "AVORA Sezon Özeti\n\nHenüz sezon kaydı bulunmuyor.";
            reportView.setText(reportText);
            return;
        }

        ZoneSeasonState scope = seasonState(season);
        seasonOutcomes = selectedOutcomes(scope);
        renderOutcomeSummary(season, scope);
        boolean closed = SeasonStatus.isClosed(season.getStatus());
        int wateringCount = closed ? season.getWatering_count() : wateringCount(scope);
        long wateringSeconds = closed ? season.getWatering_seconds() : wateringSeconds(scope);
        int fertilizerCount = closed ? season.getFertilizer_application_count() : fertilizerCount(scope);

        String healthText;
        String weatherText;
        if (closed) {
            healthText = "Kapanmış sezon · canlı sağlık puanı arşiv raporuna karıştırılmaz";
            weatherText = "Kapanmış sezon · güncel hava tahmini arşiv raporuna karıştırılmaz";
        } else {
            List<GardenZone> selectedZones = selectedZoneList(season.getZone_id());
            GardenHealthSummary health = GardenHealthCalculator.calculate(selectedZones, System.currentTimeMillis() / 1000L);
            healthText = health.getScore() + "/100 · " + health.getTitle() + "\n" + health.getDetail();
            weatherText = weather == null || weather.getTomorrowTemperatureMax() == null
                    ? "Henüz hava tahmini yok"
                    : "Yarın " + Math.round(weather.getTomorrowTemperatureMax()) + "°C"
                    + (weather.getTomorrowRainProbability() == null ? ""
                    : " · Yağış olasılığı %" + Math.round(weather.getTomorrowRainProbability()));
        }

        reportText = "AVORA Sezon Özeti\n\n"
                + displaySeasonName(season) + "\n"
                + "Durum: " + statusLabel(season) + "\n"
                + "Rapor tarihi: " + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()) + "\n\n"
                + "Sulama\nToplam sulama: " + wateringCount + "\n"
                + "Toplam çalışma süresi: " + formatDuration(wateringSeconds) + "\n\n"
                + "Gübreleme\nKayıtlı uygulama: " + fertilizerCount + "\n\n"
                + "Günlük ve AI\nGünlük olayı: " + season.getJournal_event_count()
                + " · Fotoğraf: " + season.getPhoto_count()
                + " · Bitki Asistanı analizi: " + season.getPlant_assistant_analysis_count() + "\n\n"
                + fertilizerLearningSummary(scope) + "\n\n"
                + "Bahçe sağlığı\n" + healthText + "\n\n"
                + "Hava görünümü\n" + weatherText;
        reportView.setText(reportText);
    }

    private void renderOutcomeSummary(GardenSeason season, ZoneSeasonState scope) {
        scopeView.setText(displaySeasonName(season) + " · Değiştirmek için dokunun");
        SeasonOutcome outcome = seasonOutcomes.isEmpty() ? null : seasonOutcomes.get(0);
        if (outcome == null && !SeasonStatus.isClosed(season.getStatus())) {
            outcomeSummary.setText("Sezon devam ediyor. Hasat sonucu sezon kapanışında bu kayda eklenecek.");
            comparisonView.setText(seasonComparison(season));
            return;
        }

        String result = outcome == null ? season.getResult() : outcome.getResult();
        String amount = outcome == null ? season.getHarvest_amount() : outcome.getHarvest_amount();
        String nextNote = outcome == null ? season.getNext_season_note() : outcome.getNext_season_note();
        if (result.isBlank()) result = "Sezon kapatıldı";
        if (amount.isBlank()) amount = "Miktar girilmedi";
        outcomeSummary.setText("Sezon sonucu: " + result + "\nHasat: " + amount
                + (nextNote.isBlank() ? "" : "\nGelecek sezon: " + nextNote));
        comparisonView.setText(seasonClosureDetails(season, outcome, scope) + "\n\n" + seasonComparison(season));
    }

    private List<SeasonOutcome> selectedOutcomes(ZoneSeasonState scope) {
        List<SeasonOutcome> values = new ArrayList<>();
        for (SeasonOutcome outcome : seasonOutcomeStore.load()) {
            if (!selectedZoneId.equals(outcome.getZone_id())) continue;
            if (SeasonScope.belongsTo(outcome.getSeason_id(), outcome.getRecorded_at_epoch(), scope)) values.add(outcome);
        }
        return values;
    }

    private String seasonClosureDetails(GardenSeason season, @Nullable SeasonOutcome outcome, ZoneSeasonState scope) {
        String water = outcome == null ? "" : outcome.getWater_summary();
        String fertilizer = outcome == null ? "" : outcome.getFertilizer_summary();
        if (water.isBlank()) water = season.getWatering_count() + " sulama · " + formatDuration(season.getWatering_seconds());
        if (fertilizer.isBlank()) fertilizer = season.getFertilizer_application_count() + " gübre uygulaması";
        String yield = outcome == null ? season.getYield_note() : outcome.getYield_note();
        String issues = outcome == null ? season.getIssues_note() : outcome.getIssues_note();
        String practices = outcome == null ? season.getSuccessful_practices() : outcome.getSuccessful_practices();
        String text = "Su: " + water + "\nGübre: " + fertilizer;
        if (!yield.isBlank()) text += "\nVerim notu: " + yield;
        if (!issues.isBlank()) text += "\nSorunlar: " + issues;
        if (!practices.isBlank()) text += "\nBaşarılı uygulamalar: " + practices;
        return text;
    }

    private String seasonComparison(GardenSeason current) {
        GardenSeason previous = previousSeason(current);
        if (previous == null) return "Bu bölge için karşılaştırılabilecek önceki sezon bulunmuyor.";
        String currentWater = current.getWatering_count() + " sulama / " + formatDuration(current.getWatering_seconds());
        String previousWater = previous.getWatering_count() + " sulama / " + formatDuration(previous.getWatering_seconds());
        String note = previous.getNext_season_note().isBlank()
                ? "Önceki sezon notu girilmedi." : previous.getNext_season_note();
        return current.getLabel() + " ile " + previous.getLabel() + " karşılaştırması"
                + "\nSulama: " + currentWater + " · Önceki: " + previousWater
                + "\nGübre: " + current.getFertilizer_application_count()
                + " · Önceki: " + previous.getFertilizer_application_count()
                + "\nÖnceki not: " + note;
    }

    private void ensureSelectedSeason() {
        if (seasons.isEmpty()) return;
        for (GardenSeason season : seasons) {
            if (selectedSeasonId.equals(season.getSeason_id())) {
                selectedZoneId = season.getZone_id();
                return;
            }
        }
        GardenSeason fallback = null;
        if (!selectedZoneId.isBlank()) {
            for (GardenSeason season : seasons) {
                if (!selectedZoneId.equals(season.getZone_id())) continue;
                if (fallback == null) fallback = season;
                if (SeasonStatus.isActive(season.getStatus())) {
                    fallback = season;
                    break;
                }
            }
        }
        if (fallback == null) fallback = seasons.get(0);
        selectedSeasonId = fallback.getSeason_id();
        selectedZoneId = fallback.getZone_id();
    }

    private void showSeasonSelector() {
        if (seasons.isEmpty()) {
            Toast.makeText(this, "Henüz sezon kaydı bulunmuyor.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[seasons.size()];
        int checked = 0;
        for (int i = 0; i < seasons.size(); i++) {
            GardenSeason season = seasons.get(i);
            labels[i] = displaySeasonName(season) + " · " + statusLabel(season);
            if (selectedSeasonId.equals(season.getSeason_id())) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Raporlanacak sezon")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    GardenSeason season = seasons.get(which);
                    selectedSeasonId = season.getSeason_id();
                    selectedZoneId = season.getZone_id();
                    dialog.dismiss();
                    render();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Nullable
    private GardenSeason selectedSeason() {
        for (GardenSeason season : seasons) if (selectedSeasonId.equals(season.getSeason_id())) return season;
        return null;
    }

    @Nullable
    private GardenSeason previousSeason(GardenSeason current) {
        boolean foundCurrent = false;
        for (GardenSeason season : seasons) {
            if (!current.getZone_id().equals(season.getZone_id())) continue;
            if (current.getSeason_id().equals(season.getSeason_id())) {
                foundCurrent = true;
                continue;
            }
            if (foundCurrent || season.getStarted_at_epoch() < current.getStarted_at_epoch()) return season;
        }
        return null;
    }

    private ZoneSeasonState seasonState(GardenSeason season) {
        ZoneSeasonState state = new ZoneSeasonState();
        state.setActive_season_id(season.getSeason_id());
        state.setStatus(season.getStatus());
        state.setLabel(season.getLabel());
        state.setStarted_at_epoch(season.getStarted_at_epoch());
        state.setEnded_at_epoch(season.getEnded_at_epoch());
        state.setInclude_legacy_records(season.isIncludes_legacy_records());
        return state;
    }

    private int wateringCount(ZoneSeasonState scope) {
        int count = 0;
        for (WateringHistory item : wateringHistory) {
            if (selectedZoneId.equals(item.getZoneId()) && item.isCompleted()
                    && SeasonScope.belongsTo(item.getSeasonId(), wateringEpoch(item), scope)) count++;
        }
        return count;
    }

    private long wateringSeconds(ZoneSeasonState scope) {
        long seconds = 0L;
        for (WateringHistory item : wateringHistory) {
            if (selectedZoneId.equals(item.getZoneId()) && item.isCompleted()
                    && SeasonScope.belongsTo(item.getSeasonId(), wateringEpoch(item), scope)) seconds += item.getDuration();
        }
        return seconds;
    }

    private int fertilizerCount(ZoneSeasonState scope) {
        int count = 0;
        for (FertilizerApplication item : fertilizerHistory) {
            if (selectedZoneId.equals(item.getZone_id())
                    && SeasonScope.belongsTo(item.getSeason_id(), item.getApplied_at_epoch(), scope)) count++;
        }
        return count;
    }

    private String fertilizerLearningSummary(ZoneSeasonState scope) {
        int observed = 0;
        int improved = 0;
        int unchanged = 0;
        int issue = 0;
        double vigorTotal = 0.0;
        int vigorCount = 0;
        Map<String, int[]> byProduct = new HashMap<>();
        for (FertilizerApplication value : fertilizerHistory) {
            if (!selectedZoneId.equals(value.getZone_id())) continue;
            if (!SeasonScope.belongsTo(value.getSeason_id(), value.getApplied_at_epoch(), scope)) continue;
            String status = value.getOutcome_status();
            if (status == null || status.isBlank()) continue;
            observed++;
            String product = value.getProduct_name() == null || value.getProduct_name().isBlank()
                    ? "Adsız ürün" : value.getProduct_name();
            int[] totals = byProduct.containsKey(product) ? byProduct.get(product) : new int[3];
            totals[0]++;
            if ("IMPROVED".equals(status)) {
                improved++;
                totals[1]++;
            } else if ("ISSUE".equals(status)) {
                issue++;
                totals[2]++;
            } else {
                unchanged++;
            }
            byProduct.put(product, totals);
            if (value.getOutcome_vigor_score() > 0) {
                vigorTotal += value.getOutcome_vigor_score();
                vigorCount++;
            }
        }
        if (observed == 0) {
            return "AI öğrenme özeti\nBu sezon için henüz uygulama sonucu gözlemi yok.";
        }
        String summary = "AI öğrenme özeti\nSonuç kaydı: " + observed + " · İyileşme: " + improved
                + " · Değişiklik yok: " + unchanged + " · Sorun: " + issue;
        if (vigorCount > 0) summary += "\nOrtalama canlılık: "
                + String.format(Locale.getDefault(), "%.1f", vigorTotal / vigorCount) + "/5";
        if (observed < 3) return summary + "\nGüvenilir sezon eğilimi için en az 3 sonuç gözlemi gerekir.";
        String best = "";
        int bestRate = -1;
        for (Map.Entry<String, int[]> item : byProduct.entrySet()) {
            int[] totals = item.getValue();
            if (totals[0] < 2) continue;
            int rate = Math.round(totals[1] * 100f / totals[0]);
            if (rate > bestRate) {
                bestRate = rate;
                best = item.getKey();
            }
        }
        if (!best.isBlank()) summary += "\nOlumlu eğilim gösteren ürün: " + best + " (%" + bestRate + " iyileşme bildirimi).";
        if (issue > improved) summary += "\nDikkat: sorun bildirimi iyileşmeden fazla. Doz ve uygulama koşulları gözden geçirilmeli.";
        return summary;
    }

    private List<GardenZone> selectedZoneList(String zoneId) {
        List<GardenZone> selected = new ArrayList<>();
        for (GardenZone zone : zones) if (zoneId.equals(zone.getZone_id())) selected.add(zone);
        return selected;
    }

    private long wateringEpoch(WateringHistory item) {
        String value = item.getStartedAt();
        if (value == null || value.isBlank()) value = item.getFinishedAt();
        if (value == null || value.isBlank()) return 0L;
        try { return Instant.parse(value).getEpochSecond(); } catch (Exception ignored) { }
        try { return OffsetDateTime.parse(value).toEpochSecond(); } catch (Exception ignored) { }
        try { return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toEpochSecond(); } catch (Exception ignored) { }
        return 0L;
    }

    private String displaySeasonName(GardenSeason season) {
        String zone = season.getZone_name().isBlank() ? zoneName(season.getZone_id()) : season.getZone_name();
        return zone + " · " + (season.getLabel().isBlank() ? "Sezon" : season.getLabel());
    }

    private String statusLabel(GardenSeason season) {
        return SeasonStatus.isClosed(season.getStatus()) ? "Tamamlandı" : "Devam ediyor";
    }

    private String zoneName(String zoneId) {
        for (GardenZone zone : zones) if (zoneId.equals(zone.getZone_id())) return zone.getName();
        return "Bölge";
    }

    private void syncSeasonBackup() {
        for (SeasonOutcome outcome : seasonOutcomeStore.load()) repository.saveSeasonOutcome(outcome);
        for (com.ali.smartgarden.models.GardenEvent event : gardenEventStore.load()) repository.saveGardenEvent(event);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours == 0L && minutes == 0L && seconds > 0L) return seconds + " sn";
        return hours > 0 ? hours + " sa " + minutes + " dk" : minutes + " dk";
    }

    private void writePdf(Uri uri) {
        if (uri == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            PdfDocument document = new PdfDocument();
            PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(14);
            paint.setColor(0xFF1F241F);
            int y = 48;
            for (String line : reportText.split("\\n")) {
                canvas.drawText(line, 42, y, paint);
                y += 24;
            }
            document.finishPage(page);
            document.writeTo(output);
            document.close();
            Toast.makeText(this, "PDF kaydedildi.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "PDF kaydedilemedi.", Toast.LENGTH_LONG).show();
        }
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static String safeFileName(String value) { return safe(value).replaceAll("[^A-Za-z0-9çğıöşüÇĞİÖŞÜ_-]+", "-"); }
}
