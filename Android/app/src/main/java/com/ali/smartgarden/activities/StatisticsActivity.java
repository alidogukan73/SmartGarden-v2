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
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.viewmodels.StatisticsViewModel;
import com.ali.smartgarden.zones.ZoneChipRenderer;
import com.ali.smartgarden.viewmodels.WateringHistoryViewModel;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private StatisticsViewModel viewModel;
    private WateringHistoryViewModel historyViewModel;
    private Statistics globalStatistics;
    private List<WateringHistory> wateringHistory =
            Collections.emptyList();
    private String selectedZoneId = "";

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
    private ChipGroup chipGroupStatisticZones;


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
     * XML ekranındaki öğeleri Java değişkenlerine bağlar.
     */
    private void initializeViews() {

        btnBack = findViewById(
                R.id.btnBack
        );
        chipGroupStatisticZones =
                findViewById(R.id.chipGroupStatisticZones);

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
        historyViewModel = new ViewModelProvider(this)
                .get(WateringHistoryViewModel.class);
    }


    /**
     * Statistics ve hata LiveData değerlerini gözlemler.
     */
    private void observeViewModel() {

        viewModel.getStatistics().observe(
                this,
                statistics -> {
                    globalStatistics = statistics;
                    renderSelectedStatistics();
                }
        );

        historyViewModel.getHistory().observe(
                this,
                history -> {
                    wateringHistory = history != null
                            ? history
                            : Collections.emptyList();
                    renderSelectedStatistics();
                }
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
        if (statistics.getTotalWaterings() == 0L) {
            renderEmptySuccessRate();
        } else {
            renderSuccessRate(statistics.getSuccessRate());
        }
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


    private void renderEmptySuccessRate() {

        int neutralColor =
                color(R.color.textSecondary);

        txtSuccessRate.setText(
                getString(
                        R.string.percentage_format,
                        0
                )
        );
        txtSuccessRate.setTextColor(
                neutralColor
        );
        txtSuccessDescription.setText(
                R.string.statistics_waiting
        );
        txtSuccessDescription.setTextColor(
                neutralColor
        );
        cardSuccessRate.setCardBackgroundColor(
                color(R.color.surfaceSoft)
        );
        cardSuccessRate.setStrokeColor(
                color(R.color.border)
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
                        .toLowerCase(Locale.ROOT);

        switch (normalizedReason) {

            case "completed":
            case "duration_completed":
            case "watering_completed":
                return getString(R.string.history_reason_completed);

            case "manual_stop":
            case "manual":
            case "user_stopped":
                return getString(R.string.history_reason_manual_stop);

            case "moisture_reached":
            case "target_reached":
                return getString(R.string.history_reason_target_reached);

            case "system_disabled":
                return getString(R.string.history_reason_system_disabled);

            case "device_offline":
                return getString(R.string.history_reason_device_offline);

            case "safety_timeout":
            case "timeout":
                return getString(R.string.history_reason_timeout);

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

        viewModel.getZones().observe(this, zones ->
                ZoneChipRenderer.render(
                        this,
                        chipGroupStatisticZones,
                        zones,
                        selectedZoneId,
                        R.string.history_zone_all,
                        zoneId -> {
                            selectedZoneId = zoneId;
                            renderSelectedStatistics();
                        }
                )
        );
    }


    private void renderSelectedStatistics() {

        if (selectedZoneId.isEmpty()) {
            renderStatistics(globalStatistics);
            return;
        }

        renderStatistics(
                buildZoneStatistics(selectedZoneId)
        );
    }


    private Statistics buildZoneStatistics(String zoneId) {

        Statistics result = new Statistics();
        long completed = 0L;
        long totalDuration = 0L;
        long todayCount = 0L;
        long recordCount = 0L;
        WateringHistory latest = null;

        String today = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());

        for (WateringHistory item : wateringHistory) {

            if (!zoneId.equals(item.getZoneId())) {
                continue;
            }

            if (latest == null) {
                latest = item;
            }

            recordCount++;
            totalDuration += Math.max(
                    0L,
                    item.getDuration()
            );

            if (item.isCompleted()) {
                completed++;
            }

            if (
                    item.getStartedAt() != null
                            && item.getStartedAt().startsWith(today)
            ) {
                todayCount++;
            }
        }

        result.setTotalWaterings(recordCount);
        result.setWateringsToday(todayCount);
        result.setCompletedWaterings(completed);
        result.setInterruptedWaterings(
                recordCount - completed
        );
        result.setTotalWateringSeconds(totalDuration);
        result.setAverageDuration(
                recordCount == 0L
                        ? 0L
                        : totalDuration / recordCount
        );
        result.setSuccessRate(
                recordCount == 0L
                        ? 0L
                        : Math.round(
                                completed * 100.0 / recordCount
                        )
        );

        if (latest != null) {
            result.setLastWateringDuration(
                    latest.getDuration()
            );
            result.setBeforeMoisture(
                    latest.getMoistureBefore()
            );
            result.setAfterMoisture(
                    latest.getMoistureAfter()
            );
            result.setMoistureDelta(
                    latest.getMoistureDelta()
            );
            result.setLastStopReason(
                    latest.getStopReason()
            );

            String finishedAt =
                    latest.getFinishedAt();
            result.setStatisticsDate(
                    finishedAt == null
                            || finishedAt.isBlank()
                            ? latest.getStartedAt()
                            : finishedAt
            );
        }

        return result;
    }


}
