package com.ali.smartgarden.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.adapters.FertilizationZoneAdapter;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.FertilizerRecommendation;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.viewmodels.FertilizationCalendarViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class FertilizationCalendarActivity extends AppCompatActivity {

    private final FertilizationZoneAdapter adapter =
            new FertilizationZoneAdapter();
    private final FirebaseRepository repository =
            new FirebaseRepository();

    private TextView txtSummary;
    private TextView txtDueSummary;
    private TextView txtPreparation;
    private List<GardenZone> currentZones = new ArrayList<>();
    private List<FertilizerProduct> currentProducts =
            new ArrayList<>();
    private List<FertilizerRecommendation> currentRecommendations =
            new ArrayList<>();

    private static class ProductNeed {
        FertilizerProduct product;
        double min;
        double max;
        String unit;
    }
    private final ActivityResultLauncher<String>
            notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        // The calendar remains usable if permission is denied.
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilization_calendar);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        txtSummary = findViewById(R.id.txtFertilizationSummary);
        txtDueSummary = findViewById(
                R.id.txtFertilizationDueSummary
        );
        txtPreparation = findViewById(
                R.id.txtFertilizerPreparation
        );
        RecyclerView recycler = findViewById(
                R.id.recyclerFertilizationZones
        );

        btnBack.setOnClickListener(view -> finish());
        findViewById(R.id.btnManageProducts).setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                FertilizerProductsActivity.class
                        )
                )
        );
        findViewById(R.id.btnFertilizerHistory).setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                FertilizerHistoryActivity.class
                        )
                )
        );
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);
        adapter.setOnZoneClickListener(
                zone -> {
                    Intent intent = new Intent(
                            this,
                            FertilizationZoneDetailActivity.class
                    );
                    intent.putExtra(
                            FertilizationZoneDetailActivity.EXTRA_ZONE_ID,
                            zone.getZone_id()
                    );
                    startActivity(intent);
                }
        );

        FertilizationCalendarViewModel viewModel =
                new ViewModelProvider(this).get(
                        FertilizationCalendarViewModel.class
                );
        viewModel.getZones().observe(this, this::renderZones);
        repository.observeFertilizerProducts().observe(this, products -> {
            currentProducts = products == null
                    ? new ArrayList<>()
                    : products;
            renderPreparation();
        });
        repository.observeFertilizerRecommendations().observe(
                this,
                recommendations -> {
                    currentRecommendations =
                            recommendations == null
                                    ? new ArrayList<>()
                                    : recommendations;
                    renderPreparation();
                }
        );
        requestNotificationPermissionIfNeeded();
    }

    private void renderStockSummary(
            List<FertilizerProduct> products
    ) {
        int tracked = 0;
        int low = 0;
        int empty = 0;
        if (products != null) {
            for (FertilizerProduct product : products) {
                if (!product.isEnabled()
                        || product.getStock_unit() == null
                        || product.getStock_unit().isBlank()) {
                    continue;
                }
                tracked++;
                if (product.getStock_amount() <= 0.0) {
                    empty++;
                } else if (product.getLow_stock_threshold() > 0.0
                        && product.getStock_amount()
                        <= product.getLow_stock_threshold()) {
                    low++;
                }
            }
        }
        StringBuilder summary = new StringBuilder();
        if (tracked == 0) {
            summary.append(getString(
                    R.string.fertilizer_stock_summary_empty
            ));
        } else {
            summary.append(getString(
                    R.string.fertilizer_stock_summary,
                    tracked,
                    low,
                    empty
            ));
            for (FertilizerProduct product : products) {
                if (!product.isEnabled()
                        || product.getStock_unit() == null
                        || product.getStock_unit().isBlank()) {
                    continue;
                }
                String status = product.getStock_amount() <= 0.0
                        ? getString(
                        R.string.fertilizer_stock_empty_status
                )
                        : product.getLow_stock_threshold() > 0.0
                        && product.getStock_amount()
                        <= product.getLow_stock_threshold()
                        ? getString(
                        R.string.fertilizer_stock_status_low
                )
                        : getString(R.string.fertilizer_stock_ok);
                summary.append("\n").append(getString(
                        R.string.fertilizer_stock_item,
                        product.getName(),
                        formatAmount(product.getStock_amount()),
                        product.getStock_unit(),
                        status
                ));
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }
    }

    private String formatAmount(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private void renderZones(List<GardenZone> zones) {
        currentZones = zones == null
                ? new ArrayList<>()
                : zones;
        adapter.submitList(zones);

        int total = zones == null ? 0 : zones.size();
        int active = 0;
        int overdue = 0;
        int today = 0;
        int upcoming = 0;
        if (zones != null) {
            for (GardenZone zone : zones) {
                FertilizationProfile profile =
                        zone.getFertilization();
                if (profile != null && profile.isEnabled()) {
                    active++;
                    long days = daysUntil(
                            profile.getNext_application_at_epoch()
                    );
                    if (days < 0L) {
                        overdue++;
                    } else if (days == 0L) {
                        today++;
                    } else if (days <= 7L) {
                        upcoming++;
                    }
                }
            }
        }

        txtSummary.setText(
                getString(
                        R.string.fertilization_summary,
                        active,
                        total
                )
        );
        txtDueSummary.setText(
                getString(
                        R.string.fertilization_due_summary,
                        overdue,
                        today,
                        upcoming
                )
        );
        renderPreparation();
    }

    private void renderPreparation() {
        if (txtPreparation == null) {
            return;
        }
        Map<String, ProductNeed> needs = new LinkedHashMap<>();
        int missingMeasurements = 0;
        for (GardenZone zone : currentZones) {
            FertilizationProfile profile = zone.getFertilization();
            if (profile == null
                    || !profile.isEnabled()
                    || profile.getActive_product_id() == null
                    || profile.getActive_product_id().isBlank()) {
                continue;
            }
            FertilizerProduct product = productById(
                    profile.getActive_product_id()
            );
            if (product == null || !product.isEnabled()) {
                continue;
            }
            double doseMin = product.getLabel_dosage_min() > 0.0
                    ? product.getLabel_dosage_min()
                    : product.getLabel_dosage();
            double doseMax = product.getLabel_dosage_max() > 0.0
                    ? product.getLabel_dosage_max()
                    : product.getLabel_dosage();
            String sourceUnit = safe(product.getDosage_unit());
            FertilizerRecommendation recommendation =
                    recommendationFor(zone, profile, product);
            if (recommendation != null) {
                doseMin = recommendation.getDose_min();
                doseMax = recommendation.getDose_max();
                sourceUnit = safe(recommendation.getDose_unit());
            }
            if (doseMax <= 0.0) {
                doseMax = doseMin;
            }
            String normalized = sourceUnit.toLowerCase(Locale.ROOT)
                    .replace(" ", "");
            double min;
            double max;
            String resultUnit;
            if (normalized.contains("kg/dekar")
                    && profile.getArea_m2() > 0.0) {
                min = doseMin * profile.getArea_m2();
                max = doseMax * profile.getArea_m2();
                resultUnit = "g";
            } else if (normalized.contains("l/dekar")
                    && profile.getArea_m2() > 0.0) {
                min = doseMin * profile.getArea_m2();
                max = doseMax * profile.getArea_m2();
                resultUnit = "ml";
            } else if (normalized.contains("ml/100l")
                    && profile.getTank_liters() > 0.0) {
                min = doseMin * profile.getTank_liters() / 100.0;
                max = doseMax * profile.getTank_liters() / 100.0;
                resultUnit = "ml";
            } else {
                missingMeasurements++;
                continue;
            }
            String key = product.getProduct_id() + "|" + resultUnit;
            ProductNeed need = needs.get(key);
            if (need == null) {
                need = new ProductNeed();
                need.product = product;
                need.unit = resultUnit;
                needs.put(key, need);
            }
            need.min += min;
            need.max += max;
        }

        if (needs.isEmpty() && missingMeasurements == 0) {
            txtPreparation.setText(
                    R.string.fertilizer_preparation_empty
            );
            return;
        }
        StringBuilder text = new StringBuilder();
        for (ProductNeed need : needs.values()) {
            String stockState;
            String stockUnit = safe(need.product.getStock_unit());
            if (stockUnit.isBlank()
                    || !stockUnit.equalsIgnoreCase(need.unit)) {
                stockState = getString(
                        R.string
                                .fertilizer_preparation_stock_unknown
                );
            } else if (need.product.getStock_amount() >= need.max) {
                stockState = getString(
                        R.string.fertilizer_preparation_stock_ok
                );
            } else {
                stockState = getString(
                        R.string.fertilizer_preparation_stock_short,
                        formatNumber(
                                Math.max(
                                        0.0,
                                        need.max
                                                - need.product
                                                .getStock_amount()
                                )
                        ),
                        need.unit
                );
            }
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(getString(
                    R.string.fertilizer_preparation_item,
                    need.product.getName(),
                    formatNumber(need.min),
                    formatNumber(need.max),
                    need.unit,
                    stockState
            ));
        }
        if (missingMeasurements > 0) {
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(getString(
                    R.string.fertilizer_preparation_missing,
                    missingMeasurements
            ));
        }
        txtPreparation.setText(text.toString());
    }

    private FertilizerProduct productById(String productId) {
        for (FertilizerProduct product : currentProducts) {
            if (productId.equals(product.getProduct_id())) {
                return product;
            }
        }
        return null;
    }

    private FertilizerRecommendation recommendationFor(
            GardenZone zone,
            FertilizationProfile profile,
            FertilizerProduct product
    ) {
        for (FertilizerRecommendation recommendation
                : currentRecommendations) {
            if (safe(zone.getPlant_type()).equals(
                    safe(recommendation.getPlant_type())
            ) && safe(profile.getGrowth_stage()).equals(
                    safe(recommendation.getGrowth_stage())
            ) && safe(product.getProduct_id()).equals(
                    safe(recommendation.getProduct_id())
            )) {
                return recommendation;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatNumber(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private long daysUntil(long epochSeconds) {
        if (epochSeconds <= 0L) {
            return Long.MAX_VALUE;
        }
        LocalDate due = Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return ChronoUnit.DAYS.between(LocalDate.now(), due);
    }
}
