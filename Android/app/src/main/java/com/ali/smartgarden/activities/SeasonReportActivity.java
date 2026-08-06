package com.ali.smartgarden.activities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.ali.smartgarden.models.WeatherForecast;
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
    private Statistics statistics = new Statistics();
    private List<GardenZone> zones = new ArrayList<>();
    private List<FertilizerApplication> fertilizerHistory = new ArrayList<>();
    private WeatherForecast weather;
    private String reportText = "Rapor hazırlanıyor...";

    private final ActivityResultLauncher<String> pdfCreator = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), this::writePdf);

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_season_report);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        reportView = findViewById(R.id.txtSeasonReport);
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
        repository.observeWeatherForecast().observe(this, value -> { weather = value; render(); });
    }

    private void render() {
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
