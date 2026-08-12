package com.ali.smartgarden.activities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.Statistics;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.journal.LocalSeasonOutcomeStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class SeasonReportActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private TextView reportView;
    private TextView outcomeSummary;
    private TextView scopeView;
    private TextView comparisonView;
    private Statistics statistics = new Statistics();
    private List<GardenZone> zones = new ArrayList<>();
    private List<FertilizerApplication> fertilizerHistory = new ArrayList<>();
    private List<WateringHistory> wateringHistory = new ArrayList<>();
    private WeatherForecast weather;
    private String reportText = "Rapor hazırlanıyor...";
    private LocalSeasonOutcomeStore seasonOutcomeStore;
    private LocalGardenEventStore gardenEventStore;
    private List<SeasonOutcome> seasonOutcomes = new ArrayList<>();
    private String selectedZoneId = "";

    private final ActivityResultLauncher<String> pdfCreator = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), this::writePdf);

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_season_report);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        reportView = findViewById(R.id.txtSeasonReport);
        outcomeSummary = findViewById(R.id.txtSeasonOutcomeSummary);
        scopeView = findViewById(R.id.txtSeasonScope);
        comparisonView = findViewById(R.id.txtSeasonComparison);
        selectedZoneId = getIntent().getStringExtra("zone_id");
        if (selectedZoneId == null) selectedZoneId = "";
        seasonOutcomeStore = new LocalSeasonOutcomeStore(this);
        gardenEventStore = new LocalGardenEventStore(this);
        syncSeasonBackup();
        findViewById(R.id.btnAddSeasonOutcome).setOnClickListener(view -> showSeasonOutcomeDialog());
        findViewById(R.id.btnExportSeasonPdf).setOnClickListener(view ->
                pdfCreator.launch("AVORA-Sezon-Raporu-" + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()) + ".pdf"));
        repository.observeStatistics(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot data) {
                Statistics value = data.getValue(Statistics.class);
                if (value != null) statistics = value;
                render();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
        repository.observeGardenZones().observe(this, value -> { zones = value == null ? new ArrayList<>() : value; render(); });
        repository.observeFertilizerHistory().observe(this, value -> { fertilizerHistory = value == null ? new ArrayList<>() : value; render(); });
        repository.observeWateringHistory().observe(this, value -> { wateringHistory = value == null ? new ArrayList<>() : value; render(); });
        repository.observeWeatherForecast().observe(this, value -> { weather = value; render(); });
    }

    private void render() {
        seasonOutcomes = selectedOutcomes();
        renderOutcomeSummary();
        GardenHealthSummary health = GardenHealthCalculator.calculate(zones, System.currentTimeMillis() / 1000L);
        String weatherText = weather == null || weather.getTomorrowTemperatureMax() == null ? "Henüz hava tahmini yok"
                : "Yarın " + Math.round(weather.getTomorrowTemperatureMax()) + "°C"
                + (weather.getTomorrowRainProbability() == null ? "" : " · Yağış olasılığı %" + Math.round(weather.getTomorrowRainProbability()));
        reportText = "AVORA Sezon Özeti\n\n"
                + "Rapor tarihi: " + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()) + "\n\n"
                + "Sulama\nToplam sulama: " + statistics.getTotalWaterings() + "\n"
                + "Toplam çalışma süresi: " + formatDuration(statistics.getTotalWateringSeconds()) + "\n"
                + "Başarı oranı: %" + statistics.getSuccessRate() + "\n\n"
                + "Gübreleme\nKayıtlı uygulama: " + fertilizerHistory.size() + "\n\n"
                + fertilizerLearningSummary() + "\n\n"
                + "Bahçe sağlığı\n" + health.getScore() + "/100 · " + health.getTitle() + "\n" + health.getDetail() + "\n\n"
                + "Hava görünümü\n" + weatherText;
        reportView.setText(reportText);
    }

    private void renderOutcomeSummary() {
        scopeView.setText(selectedZoneId.isEmpty()
                ? "Tüm bahçe bölgeleri"
                : zoneName(selectedZoneId) + " bölgesinin sezon kayıtları");
        if (seasonOutcomes.isEmpty()) {
            outcomeSummary.setText("Henüz hasat sonucu yok. Sezon sonunda miktarı, sonucu ve gelecek yıl notunu ekleyin.");
            comparisonView.setText("Önceki sezon karşılaştırması için en az iki sezon sonucu kaydedin.");
            return;
        }
        SeasonOutcome latest = seasonOutcomes.get(0);
        comparisonView.setText(seasonClosureDetails(latest) + "\n\n" + seasonComparison());
        String result = latest.getResult().isBlank() ? "Sonuç kaydedildi" : latest.getResult();
        String amount = latest.getHarvest_amount().isBlank() ? "Miktar girilmedi" : latest.getHarvest_amount();
        outcomeSummary.setText("Son kayıt: " + zoneName(latest.getZone_id()) + " · " + result + "\nHasat: " + amount
                + (latest.getNext_season_note().isBlank() ? "" : "\nGelecek sezon: " + latest.getNext_season_note()));
    }

    private List<SeasonOutcome> selectedOutcomes() {
        List<SeasonOutcome> values = new ArrayList<>();
        for (SeasonOutcome outcome : seasonOutcomeStore.load()) {
            if (selectedZoneId.isEmpty() || selectedZoneId.equals(outcome.getZone_id())) values.add(outcome);
        }
        return values;
    }

    private String waterSummary(String zoneId) {
        int count = 0; long seconds = 0L;
        for (WateringHistory item : wateringHistory) {
            if (zoneId.equals(item.getZoneId()) && item.isCompleted()) { count++; seconds += item.getDuration(); }
        }
        return count + " sulama · " + formatDuration(seconds);
    }

    private String fertilizerSummary(String zoneId) {
        int count = 0;
        for (FertilizerApplication item : fertilizerHistory) if (zoneId.equals(item.getZone_id())) count++;
        return count + " gübre uygulaması";
    }

    private String seasonClosureDetails(SeasonOutcome outcome) {
        String text = "Su: " + (outcome.getWater_summary().isBlank() ? waterSummary(outcome.getZone_id()) : outcome.getWater_summary())
                + "\nGübre: " + (outcome.getFertilizer_summary().isBlank() ? fertilizerSummary(outcome.getZone_id()) : outcome.getFertilizer_summary());
        if (!outcome.getYield_note().isBlank()) text += "\nVerim notu: " + outcome.getYield_note();
        if (!outcome.getIssues_note().isBlank()) text += "\nSorunlar: " + outcome.getIssues_note();
        if (!outcome.getSuccessful_practices().isBlank()) text += "\nBaşarılı uygulamalar: " + outcome.getSuccessful_practices();
        return text;
    }

    private String seasonComparison() {
        if (seasonOutcomes.size() < 2) return "Önceki sezon karşılaştırması için en az iki sezon sonucu kaydedin.";
        SeasonOutcome current = seasonOutcomes.get(0);
        SeasonOutcome previous = seasonOutcomes.get(1);
        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date(current.getRecorded_at_epoch() * 1000L));
        String previousYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date(previous.getRecorded_at_epoch() * 1000L));
        String note = previous.getNext_season_note().isBlank() ? "Önceki sezon notu girilmedi." : previous.getNext_season_note();
        return currentYear + " sezonu, " + previousYear + " sezonuyla karşılaştırılmaya hazır. Önceki not: " + note;
    }

    private String zoneName(String zoneId) {
        for (GardenZone zone : zones) if (zoneId.equals(zone.getZone_id())) return zone.getName();
        return "Bölge";
    }

    private void showSeasonOutcomeDialog() {
        if (!selectedZoneId.isEmpty()) {
            for (GardenZone zone : zones) {
                if (selectedZoneId.equals(zone.getZone_id())) {
                    showOutcomeFields(zone);
                    return;
                }
            }
        }
        if (zones.isEmpty()) {
            Toast.makeText(this, "Önce bir bahçe bölgesi oluşturun.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[zones.size()];
        for (int i = 0; i < zones.size(); i++) labels[i] = zones.get(i).getEmoji() + " " + zones.get(i).getName();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hangi bölgenin sezonu tamamlandı?")
                .setItems(labels, (dialog, index) -> showOutcomeFields(zones.get(index)))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showOutcomeFields(GardenZone zone) {
        LinearLayout content = new LinearLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, 0, pad, 0);
        content.setOrientation(LinearLayout.VERTICAL);
        EditText amount = outcomeField("Yaklaşık hasat miktarı (ör. 18 kg)", InputType.TYPE_CLASS_TEXT);
        EditText note = outcomeField("Gelecek sezon notu (isteğe bağlı)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText yield = outcomeField("Verim değerlendirmesi (isteğe bağlı)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText issues = outcomeField("Sezon boyunca görülen sorunlar (isteğe bağlı)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText practices = outcomeField("En başarılı uygulamalar (isteğe bağlı)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(3);
        content.addView(amount);
        content.addView(yield);
        content.addView(issues);
        content.addView(practices);
        content.addView(note);
        String[] results = {"Çok iyi geçti", "İyi geçti", "Orta", "Beklenenin altında"};
        final int[] choice = {1};
        new MaterialAlertDialogBuilder(this)
                .setTitle(zone.getEmoji() + " " + zone.getName() + " · Sezon sonucu")
                .setSingleChoiceItems(results, choice[0], (dialog, which) -> choice[0] = which)
                .setView(content)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String harvest = amount.getText() == null ? "" : amount.getText().toString().trim();
                    String nextNote = note.getText() == null ? "" : note.getText().toString().trim();
                    String yieldNote = yield.getText() == null ? "" : yield.getText().toString().trim();
                    String issuesNote = issues.getText() == null ? "" : issues.getText().toString().trim();
                    String practicesNote = practices.getText() == null ? "" : practices.getText().toString().trim();
                    SeasonOutcome outcome = seasonOutcomeStore.add(zone.getZone_id(), results[choice[0]], harvest, yieldNote, issuesNote, practicesNote, waterSummary(zone.getZone_id()), fertilizerSummary(zone.getZone_id()), nextNote);
                    repository.saveSeasonOutcome(outcome);
                    com.ali.smartgarden.models.GardenEvent event = gardenEventStore.add(zone.getZone_id(), "Sezon sonucu", results[choice[0]]
                            + (harvest.isBlank() ? "" : " · Hasat: " + harvest)
                            + (nextNote.isBlank() ? "" : " · " + nextNote));
                    repository.saveGardenEvent(event);
                    render();
                    Toast.makeText(this, "Sezon sonucu kaydedildi.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private EditText outcomeField(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextColor(getResources().getColor(R.color.textPrimary));
        input.setHintTextColor(getResources().getColor(R.color.textSecondary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        input.setLayoutParams(params);
        return input;
    }

    private void syncSeasonBackup() {
        for (SeasonOutcome outcome : seasonOutcomeStore.load()) repository.saveSeasonOutcome(outcome);
        for (com.ali.smartgarden.models.GardenEvent event : gardenEventStore.load()) repository.saveGardenEvent(event);
    }

    private String fertilizerLearningSummary() {
        int observed = 0;
        int improved = 0;
        int unchanged = 0;
        int issue = 0;
        double vigorTotal = 0.0;
        int vigorCount = 0;
        Map<String, int[]> byProduct = new HashMap<>();
        for (FertilizerApplication value : fertilizerHistory) {
            String status = value.getOutcome_status();
            if (status == null || status.isBlank()) continue;
            observed++;
            String product = value.getProduct_name() == null
                    || value.getProduct_name().isBlank()
                    ? "Adsız ürün" : value.getProduct_name();
            int[] totals = byProduct.containsKey(product)
                    ? byProduct.get(product) : new int[3];
            totals[0]++;
            if ("IMPROVED".equals(status)) {
                improved++; totals[1]++;
            } else if ("ISSUE".equals(status)) {
                issue++; totals[2]++;
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
            return "AI öğrenme özeti\nHenüz uygulama sonucu gözlemi yok. Gübre geçmişindeki kayıtlara sonuç ekledikçe sezon değerlendirmesi oluşur.";
        }
        String summary = "AI öğrenme özeti\n"
                + "Sonuç kaydı: " + observed + " · İyileşme: " + improved
                + " · Değişiklik yok: " + unchanged + " · Sorun: " + issue;
        if (vigorCount > 0) {
            summary += "\nOrtalama canlılık: "
                    + String.format(Locale.getDefault(), "%.1f", vigorTotal / vigorCount)
                    + "/5";
        }
        if (observed < 3) {
            return summary + "\nGüvenilir sezon eğilimi için en az 3 sonuç gözlemi gerekir.";
        }
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
        if (!best.isBlank()) {
            summary += "\nOlumlu eğilim gösteren ürün: " + best
                    + " (%" + bestRate + " iyileşme bildirimi).";
        } else {
            summary += "\nÜrün karşılaştırması için aynı ürünle en az iki sonuç kaydı gerekir.";
        }
        if (issue > improved) {
            summary += "\nDikkat: sorun bildirimi iyileşmeden fazla. Doz, karışım, su kalitesi ve etiket talimatı değerlendirilmelidir.";
        }
        return summary;
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600, minutes = (seconds % 3600) / 60;
        return hours > 0 ? hours + " sa " + minutes + " dk" : minutes + " dk";
    }

    private void writePdf(Uri uri) {
        if (uri == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            PdfDocument document = new PdfDocument();
            PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
            Canvas canvas = page.getCanvas(); Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(14); paint.setColor(0xFF1F241F);
            int y = 48;
            for (String line : reportText.split("\\n")) {
                canvas.drawText(line, 42, y, paint); y += 24;
            }
            document.finishPage(page); document.writeTo(output); document.close();
            Toast.makeText(this, "PDF kaydedildi.", Toast.LENGTH_LONG).show();
        } catch (Exception error) { Toast.makeText(this, "PDF kaydedilemedi.", Toast.LENGTH_LONG).show(); }
    }
}
