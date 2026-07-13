package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Statistics;
import com.ali.smartgarden.viewmodels.StatisticsViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class StatisticsActivity extends AppCompatActivity {

    private StatisticsViewModel viewModel;

    // Sulama özeti
    private TextView txtTodayWaterings;
    private TextView txtTotalWaterings;

    // Başarı oranı
    private MaterialCardView cardSuccessRate;
    private TextView txtSuccessRate;
    private TextView txtSuccessDescription;

    // Sulama sonuçları
    private TextView txtCompletedWaterings;
    private TextView txtInterruptedWaterings;

    // Süreler
    private TextView txtAverageDuration;
    private TextView txtLastWateringDuration;
    private TextView txtTotalWateringDuration;

    // Nem değişimi
    private MaterialCardView cardMoistureChange;
    private MaterialCardView cardMoistureDelta;
    private TextView txtBeforeMoisture;
    private TextView txtAfterMoisture;
    private TextView txtMoistureChange;

    // Son durum
    private TextView txtLastStopReason;
    private TextView txtStatisticsDate;


    private MaterialButton btnBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_statistics);

        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                }
        );

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeActions();
    }


    /**
     * Edge-to-edge sistem çubuğu boşluklarını uygular.
     */
    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.statisticsRoot),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }


    /**
     * XML bileşenlerini Java değişkenlerine bağlar.
     */
    private void initializeViews() {

        btnBack = findViewById(
                R.id.btnBack
        );

        // Sulama özeti
        txtTodayWaterings =
                findViewById(R.id.txtTodayWaterings);

        txtTotalWaterings =
                findViewById(R.id.txtTotalWaterings);

        // Başarı oranı
        cardSuccessRate =
                findViewById(R.id.cardSuccessRate);

        txtSuccessRate =
                findViewById(R.id.txtSuccessRate);

        txtSuccessDescription =
                findViewById(R.id.txtSuccessDescription);

        // Sulama sonuçları
        txtCompletedWaterings =
                findViewById(R.id.txtCompletedWaterings);

        txtInterruptedWaterings =
                findViewById(R.id.txtInterruptedWaterings);

        // Süre istatistikleri
        txtAverageDuration =
                findViewById(R.id.txtAverageDuration);

        txtLastWateringDuration =
                findViewById(R.id.txtLastWateringDuration);

        txtTotalWateringDuration =
                findViewById(R.id.txtTotalWateringDuration);

        // Nem değişimi
        cardMoistureChange =
                findViewById(R.id.cardMoistureChange);

        cardMoistureDelta =
                findViewById(R.id.cardMoistureDelta);

        txtBeforeMoisture =
                findViewById(R.id.txtBeforeMoisture);

        txtAfterMoisture =
                findViewById(R.id.txtAfterMoisture);

        txtMoistureChange =
                findViewById(R.id.txtMoistureChange);

        // Son durum
        txtLastStopReason =
                findViewById(R.id.txtLastStopReason);

        txtStatisticsDate =
                findViewById(R.id.txtStatisticsDate);
    }


    /**
     * StatisticsViewModel oluşturur.
     */
    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(StatisticsViewModel.class);
    }


    /**
     * Statistics ve hata LiveData değerlerini gözlemler.
     */
    private void observeViewModel() {

        viewModel.getStatistics().observe(
                this,
                this::renderStatistics
        );

        viewModel.getError().observe(
                this,
                message -> {

                    if (
                            message == null
                                    || message.isBlank()
                    ) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }


    /**
     * Firebase'den gelen bütün istatistikleri ekrana yansıtır.
     */
    private void renderStatistics(Statistics statistics) {

        if (statistics == null) {
            return;
        }

        renderWateringSummary(statistics);
        renderSuccessRate(statistics.getSuccessRate());
        renderWateringResults(statistics);
        renderDurations(statistics);
        renderMoistureChange(statistics);
        renderLastStatus(statistics);
    }


    /**
     * Bugünkü ve toplam sulama sayılarını gösterir.
     */
    private void renderWateringSummary(Statistics statistics) {

        txtTodayWaterings.setText(
                String.valueOf(
                        statistics.getWateringsToday()
                )
        );

        txtTotalWaterings.setText(
                String.valueOf(
                        statistics.getTotalWaterings()
                )
        );
    }


    /**
     * Başarı oranını ve açıklamasını dinamik renklendirir.
     */
    private void renderSuccessRate(long successRate) {

        long safeSuccessRate = Math.max(
                0,
                Math.min(
                        100,
                        successRate
                )
        );

        txtSuccessRate.setText(
                getString(
                        R.string.percentage_format,
                        safeSuccessRate
                )
        );

        int statusColor;
        int backgroundColor;
        int descriptionResource;

        if (safeSuccessRate >= 90) {

            statusColor =
                    color(R.color.online);

            backgroundColor =
                    color(R.color.onlineBackground);

            descriptionResource =
                    R.string.statistics_success_excellent;

        } else if (safeSuccessRate >= 70) {

            statusColor =
                    color(R.color.primary);

            backgroundColor =
                    color(R.color.primaryLight);

            descriptionResource =
                    R.string.statistics_success_good;

        } else {

            statusColor =
                    color(R.color.warning);

            backgroundColor =
                    color(R.color.warningBackground);

            descriptionResource =
                    R.string.statistics_success_warning;
        }

        txtSuccessRate.setTextColor(
                statusColor
        );

        txtSuccessDescription.setText(
                descriptionResource
        );

        txtSuccessDescription.setTextColor(
                statusColor
        );

        cardSuccessRate.setCardBackgroundColor(
                backgroundColor
        );

        cardSuccessRate.setStrokeColor(
                statusColor
        );
    }


    /**
     * Tamamlanan ve kesintiye uğrayan işlemleri gösterir.
     */
    private void renderWateringResults(Statistics statistics) {

        txtCompletedWaterings.setText(
                String.valueOf(
                        statistics.getCompletedWaterings()
                )
        );

        txtInterruptedWaterings.setText(
                String.valueOf(
                        statistics.getInterruptedWaterings()
                )
        );
    }


    /**
     * Ortalama, son ve toplam süreleri gösterir.
     */
    private void renderDurations(Statistics statistics) {

        txtAverageDuration.setText(
                formatDuration(
                        statistics.getAverageDuration()
                )
        );

        txtLastWateringDuration.setText(
                formatDuration(
                        statistics.getLastWateringDuration()
                )
        );

        txtTotalWateringDuration.setText(
                formatDuration(
                        statistics.getTotalWateringSeconds()
                )
        );
    }


    /**
     * Nem öncesi, sonrası ve değişimini gösterir.
     */
    private void renderMoistureChange(Statistics statistics) {

        long beforeMoisture =
                statistics.getBeforeMoisture();

        long afterMoisture =
                statistics.getAfterMoisture();

        long moistureDelta =
                statistics.getMoistureDelta();

        txtBeforeMoisture.setText(
                getString(
                        R.string.percentage_format,
                        beforeMoisture
                )
        );

        txtAfterMoisture.setText(
                getString(
                        R.string.percentage_format,
                        afterMoisture
                )
        );

        txtMoistureChange.setText(
                getString(
                        R.string.signed_percentage_format,
                        moistureDelta
                )
        );

        updateMoistureDeltaUi(
                moistureDelta
        );
    }


    /**
     * Nem farkı kartını değere göre renklendirir.
     */
    private void updateMoistureDeltaUi(long moistureDelta) {

        int statusColor;
        int backgroundColor;

        if (moistureDelta > 0) {

            statusColor =
                    color(R.color.moistureIdeal);

            backgroundColor =
                    color(R.color.moistureIdealBackground);

        } else if (moistureDelta < 0) {

            statusColor =
                    color(R.color.moistureLow);

            backgroundColor =
                    color(R.color.moistureLowBackground);

        } else {

            statusColor =
                    color(R.color.textSecondary);

            backgroundColor =
                    color(R.color.surfaceSoft);
        }

        txtMoistureChange.setTextColor(
                statusColor
        );

        cardMoistureDelta.setCardBackgroundColor(
                backgroundColor
        );

        cardMoistureDelta.setStrokeColor(
                statusColor
        );

        cardMoistureChange.setStrokeColor(
                moistureDelta > 0
                        ? color(R.color.moistureIdeal)
                        : color(R.color.border)
        );
    }


    /**
     * Son durdurma nedenini ve tarihi gösterir.
     */
    private void renderLastStatus(Statistics statistics) {

        String stopReason =
                statistics.getLastStopReason();

        String statisticsDate =
                statistics.getStatisticsDate();

        if (
                stopReason == null
                        || stopReason.isBlank()
        ) {

            txtLastStopReason.setText(
                    R.string.statistics_waiting
            );

        } else {

            txtLastStopReason.setText(
                    formatStopReason(stopReason)
            );
        }

        if (
                statisticsDate == null
                        || statisticsDate.isBlank()
        ) {

            txtStatisticsDate.setText("-");

        } else {

            txtStatisticsDate.setText(
                    statisticsDate
            );
        }
    }


    /**
     * Backend durdurma nedenlerini kullanıcı dostu metne çevirir.
     */
    private String formatStopReason(String stopReason) {

        String normalizedReason =
                stopReason
                        .trim()
                        .toLowerCase();

        switch (normalizedReason) {

            case "completed":
            case "duration_completed":
            case "watering_completed":
                return "Başarıyla tamamlandı";

            case "manual_stop":
            case "manual":
            case "user_stopped":
                return "Kullanıcı tarafından durduruldu";

            case "moisture_reached":
            case "target_reached":
                return "Hedef nem seviyesine ulaşıldı";

            case "system_disabled":
                return "Sistem devre dışı bırakıldı";

            case "device_offline":
                return "Cihaz bağlantısı kesildi";

            case "safety_timeout":
            case "timeout":
                return "Güvenlik süresi doldu";

            default:
                return stopReason
                        .replace("_", " ");
        }
    }


    /**
     * Saniye değerini okunabilir süreye dönüştürür.
     */
    private String formatDuration(long seconds) {

        long safeSeconds = Math.max(
                0,
                seconds
        );

        if (safeSeconds < 60) {

            return getString(
                    R.string.duration_seconds_format,
                    safeSeconds
            );
        }

        long hours =
                safeSeconds / 3600;

        long minutes =
                (safeSeconds % 3600) / 60;

        long remainingSeconds =
                safeSeconds % 60;

        if (hours > 0) {

            return String.format(
                    java.util.Locale.getDefault(),
                    "%d sa %d dk",
                    hours,
                    minutes
            );
        }

        return getString(
                R.string.duration_minutes_seconds_format,
                minutes,
                remainingSeconds
        );
    }


    /**
     * Renk kaynağını güvenli biçimde çözer.
     */
    private int color(int colorResource) {

        return ContextCompat.getColor(
                this,
                colorResource
        );
    }


    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );
    }
}