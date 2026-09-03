package com.alidogukan.avora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alidogukan.avora.R;
import com.alidogukan.avora.adapters.WateringHistoryAdapter;
import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.WateringHistory;
import com.alidogukan.avora.season.SeasonDisplayIdentity;
import com.alidogukan.avora.viewmodels.WateringHistoryViewModel;
import com.alidogukan.avora.zones.ZoneChipRenderer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WateringHistoryActivity extends AppCompatActivity {

    private WateringHistoryViewModel viewModel;
    private WateringHistoryAdapter adapter;

    private MaterialButton btnBack;
    private RecyclerView recyclerHistory;
    private LinearLayout layoutLoading;
    private LinearLayout layoutEmpty;
    private TextView txtHistoryEmptyDescription;
    private TextView txtHistoryStatCount;
    private TextView txtHistoryStatDuration;
    private TextView txtHistoryStatSuccess;
    private TextView txtHistoryStatDelta;
    private ChipGroup chipGroupZones;
    private List<WateringHistory> allHistory =
            Collections.emptyList();
    private String selectedZoneId = "";
    private List<GardenZone> latestZones = Collections.emptyList();
    private List<GardenSeason> latestSeasons = Collections.emptyList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_watering_history
        );

        applyWindowInsets();
        initializeViews();
        initializeRecyclerView();
        initializeViewModel();
        observeViewModel();
        initializeActions();
    }


    /**
     * Edge-to-edge sistem çubuğu boşluklarını uygular.
     */
    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.historyRoot),
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

        btnBack =
                findViewById(R.id.btnBack);

        recyclerHistory =
                findViewById(R.id.recyclerHistory);

        layoutLoading =
                findViewById(R.id.layoutLoading);

        layoutEmpty =
                findViewById(R.id.layoutEmpty);

        txtHistoryEmptyDescription =
                findViewById(R.id.txtHistoryEmptyDescription);

        chipGroupZones =
                findViewById(R.id.chipGroupZones);

        txtHistoryStatCount =
                findViewById(R.id.txtHistoryStatCount);
        txtHistoryStatDuration =
                findViewById(R.id.txtHistoryStatDuration);
        txtHistoryStatSuccess =
                findViewById(R.id.txtHistoryStatSuccess);
        txtHistoryStatDelta =
                findViewById(R.id.txtHistoryStatDelta);
    }


    /**
     * RecyclerView ve adapter yapılandırmasını hazırlar.
     */
    private void initializeRecyclerView() {

        adapter =
                new WateringHistoryAdapter();

        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerHistory.setAdapter(
                adapter
        );

        /*
         * RecyclerView kartlarının boyutu sabitse küçük bir
         * performans iyileştirmesi sağlar.
         */
        recyclerHistory.setHasFixedSize(
                true
        );
    }


    /**
     * WateringHistoryViewModel oluşturur.
     */
    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(WateringHistoryViewModel.class);
    }


    /**
     * Geçmiş, yükleniyor ve hata LiveData değerlerini gözlemler.
     */
    private void observeViewModel() {

        viewModel.getHistory().observe(
                this,
                this::renderHistory
        );

        viewModel.getLoading().observe(
                this,
                loading -> {

                    boolean isLoading =
                            Boolean.TRUE.equals(loading);

                    layoutLoading.setVisibility(
                            isLoading
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    if (isLoading) {

                        recyclerHistory.setVisibility(
                                View.GONE
                        );

                        layoutEmpty.setVisibility(
                                View.GONE
                        );
                    }
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
     * Firebase geçmiş listesini RecyclerView'e aktarır.
     */
    private void renderHistory(
            List<WateringHistory> historyItems
    ) {

        allHistory = historyItems != null
                ? historyItems
                : java.util.Collections.emptyList();

        applyZoneFilter();
    }

    private void applyZoneFilter() {
        List<WateringHistory> visibleItems =
                new ArrayList<>();

        for (WateringHistory item : allHistory) {
            if (
                    selectedZoneId.isEmpty()
                            || selectedZoneId.equals(item.getZoneId())
            ) {
                visibleItems.add(item);
            }
        }

        adapter.submitList(visibleItems);
        renderStatistics(visibleItems);

        boolean isEmpty = visibleItems.isEmpty();

        recyclerHistory.setVisibility(
                isEmpty
                        ? View.GONE
                        : View.VISIBLE
        );

        layoutEmpty.setVisibility(
                isEmpty
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutLoading.setVisibility(
                View.GONE
        );

        txtHistoryEmptyDescription.setText(
                selectedZoneId.isEmpty()
                        ? R.string.history_empty_description
                        : R.string.history_empty_zone_description
        );
    }


    /**
     * Kullanıcı işlemlerini başlatır.
     */
    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        viewModel.getZones().observe(this, zones -> {
            latestZones = zones == null ? Collections.emptyList() : zones;
            renderZoneFilters();
        });
        viewModel.getSeasons().observe(this, seasons -> {
            latestSeasons = seasons == null ? Collections.emptyList() : seasons;
            renderZoneFilters();
        });
    }

    private void renderZoneFilters() {
        if (adapter == null || chipGroupZones == null) return;
        adapter.setZoneLabels(zoneLabels(latestZones));
        ZoneChipRenderer.render(
                this,
                chipGroupZones,
                latestZones,
                selectedZoneId,
                R.string.history_zone_all,
                zoneLabels(latestZones),
                zoneId -> {
                    selectedZoneId = zoneId;
                    applyZoneFilter();
                }
        );
    }

    private java.util.Map<String, String> zoneLabels(
            List<GardenZone> zones
    ) {
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        if (zones == null) return labels;
        for (com.alidogukan.avora.models.GardenZone zone : zones) {
            if (zone == null || zone.getZone_id() == null) continue;
            labels.put(zone.getZone_id(), SeasonDisplayIdentity.operationalLabel(
                    zone, latestSeasons));
        }
        return labels;
    }



    private void renderStatistics(
            List<WateringHistory> items
    ) {
        int count = items.size();
        int completedCount = 0;
        long totalDuration = 0L;
        long totalDelta = 0L;

        for (WateringHistory item : items) {
            totalDuration += Math.max(0L, item.getDuration());
            totalDelta += item.getMoistureDelta();
            if (item.isCompleted()) {
                completedCount++;
            }
        }

        int successRate = count == 0
                ? 0
                : Math.round(
                        completedCount * 100f / count
                );
        double averageDelta = count == 0
                ? 0.0
                : totalDelta / (double) count;

        txtHistoryStatCount.setText(
                getString(
                        R.string.history_stat_count,
                        count
                )
        );
        txtHistoryStatDuration.setText(
                getString(
                        R.string.history_stat_duration,
                        formatTotalDuration(totalDuration)
                )
        );
        txtHistoryStatSuccess.setText(
                getString(
                        R.string.history_stat_success,
                        successRate
                )
        );
        txtHistoryStatDelta.setText(
                getString(
                        R.string.history_stat_delta,
                        averageDelta
                )
        );
    }

    private String formatTotalDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        if (safeSeconds < 60L) {
            return safeSeconds + " sn";
        }

        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long remainingSeconds = safeSeconds % 60L;

        if (hours > 0L) {
            return minutes > 0L
                    ? hours + " sa " + minutes + " dk"
                    : hours + " sa";
        }
        return remainingSeconds > 0L
                ? minutes + " dk " + remainingSeconds + " sn"
                : minutes + " dk";
    }

}
