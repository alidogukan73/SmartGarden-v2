package com.ali.smartgarden.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.FertilizerProduct;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.fertilization.FertilizerAdvice;
import com.ali.smartgarden.fertilization.FertilizerDecisionEngine;
import com.ali.smartgarden.fertilization.FertilizerMixAdvisor;
import com.ali.smartgarden.fertilization.FertilizerMixResult;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.ali.smartgarden.viewmodels.FertilizationCalendarViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;
import java.util.Calendar;

public class FertilizationCalendarActivity extends AppCompatActivity {

    private final FirebaseRepository repository =
            new FirebaseRepository();

    private LinearLayout layoutTodayAdvice;
    private List<GardenZone> currentZones = new ArrayList<>();
    private List<FertilizerProduct> currentProducts =
            new ArrayList<>();
    private WeatherForecast currentWeather;

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
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.ASSISTANT);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        layoutTodayAdvice = findViewById(R.id.layoutTodayFertilizerAdvice);

        btnBack.setOnClickListener(view -> finish());
        findViewById(R.id.btnManageProducts).setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                FertilizerProductsActivity.class
                        )
                )
        );
        findViewById(R.id.btnFertilizerMixCheck).setOnClickListener(
                view -> showMixFirstProductPicker()
        );
        findViewById(R.id.btnFertilizerHistory).setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                FertilizerHistoryActivity.class
                        )
                )
        );
        findViewById(R.id.btnBulkFertilizerApplication).setOnClickListener(
                view -> showBulkProductPicker()
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
            renderTodayAdvice();
        });
        repository.observeWeatherForecast().observe(this, weather -> {
            currentWeather = weather;
            renderTodayAdvice();
        });
        requestNotificationPermissionIfNeeded();
    }

    private void showMixFirstProductPicker() {
        List<FertilizerProduct> products = enabledProducts();
        if (products.size() < 2) {
            Toast.makeText(this, "Karışım kontrolü için en az iki etkin gübre ekleyin.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("İlk gübreyi seçin")
                .setItems(productNames(products), (dialog, index) ->
                        showMixSecondProductPicker(products, products.get(index)))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showMixSecondProductPicker(
            List<FertilizerProduct> products, FertilizerProduct first) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("İkinci gübreyi seçin")
                .setItems(productNames(products), (dialog, index) ->
                        showMixResult(first, products.get(index)))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showMixResult(FertilizerProduct first, FertilizerProduct second) {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(first, second);
        String message = productName(first) + " + " + productName(second) + "\n\n"
                + result.getMessage() + "\n\n"
                + "Bu kontrol öneri amaçlıdır; ürün etiketindeki karışım "
                + "bilgisi ve kavanoz testi önceliklidir.";
        new MaterialAlertDialogBuilder(this)
                .setTitle(result.getTitle())
                .setMessage(message)
                .setPositiveButton("Anladım", null)
                .show();
    }

    private List<FertilizerProduct> enabledProducts() {
        List<FertilizerProduct> result = new ArrayList<>();
        for (FertilizerProduct product : currentProducts) {
            if (product != null && product.isEnabled()) {
                result.add(product);
            }
        }
        return result;
    }

    private String[] productNames(List<FertilizerProduct> products) {
        String[] labels = new String[products.size()];
        for (int index = 0; index < products.size(); index++) {
            labels[index] = productName(products.get(index));
        }
        return labels;
    }

    private String productName(FertilizerProduct product) {
        String name = product.getName();
        return name == null || name.trim().isEmpty() ? "Adsız ürün" : name;
    }

    private void showBulkProductPicker() {
        List<FertilizerProduct> products = new ArrayList<>();
        for (FertilizerProduct product : currentProducts) {
            if (product.isEnabled()) {
                products.add(product);
            }
        }
        if (products.isEmpty()) {
            Toast.makeText(this, "Önce kullanılacak gübre ürününü ekleyin.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[products.size()];
        for (int i = 0; i < products.size(); i++) names[i] = products.get(i).getName();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Gübre ürünü seçin")
                .setItems(names, (dialog, which) -> showBulkZonePicker(products.get(which)))
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void showBulkZonePicker(FertilizerProduct product) {
        List<GardenZone> eligible = new ArrayList<>();
        for (GardenZone zone : currentZones) {
            FertilizationProfile profile = zone.getFertilization();
            if (profile != null && profile.isEnabled() && calculateBulkDose(product, profile) > 0.0) {
                eligible.add(zone);
            }
        }
        if (eligible.isEmpty()) {
            Toast.makeText(this, "Alan veya tank bilgisi olan etkin bölge bulunamadı.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[eligible.size()];
        boolean[] checked = new boolean[eligible.size()];
        for (int i = 0; i < eligible.size(); i++) names[i] = eligible.get(i).getEmoji() + " " + eligible.get(i).getName();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Uygulanacak bölgeleri seçin")
                .setMultiChoiceItems(names, checked, (dialog, which, selected) -> checked[which] = selected)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton("Tarih seç", (dialog, which) -> showBulkDatePicker(product, eligible, checked))
                .show();
    }

    private void showBulkDatePicker(FertilizerProduct product, List<GardenZone> zones, boolean[] checked) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 12, 0, 0);
            saveBulkApplication(product, zones, checked, selected.getTimeInMillis() / 1000L);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private double calculateBulkDose(FertilizerProduct product, FertilizationProfile profile) {
        double dose = product.getLabel_dosage_min() > 0 ? product.getLabel_dosage_min() : product.getLabel_dosage();
        String unit = safe(product.getDosage_unit()).toLowerCase(Locale.ROOT).replace(" ", "");
        if (unit.contains("kg/dekar") && profile.getArea_m2() > 0) return dose * profile.getArea_m2();
        if (unit.contains("l/dekar") && profile.getArea_m2() > 0) return dose * profile.getArea_m2();
        if (unit.contains("ml/100l") && profile.getTank_liters() > 0) return dose * profile.getTank_liters() / 100.0;
        return 0.0;
    }

    private void saveBulkApplication(FertilizerProduct product, List<GardenZone> zones, boolean[] checked, long appliedAt) {
        List<com.google.android.gms.tasks.Task<Void>> tasks = new ArrayList<>();
        double total = 0.0;
        String dosageUnit = safe(product.getDosage_unit()).toLowerCase(Locale.ROOT).replace(" ", "");
        String appliedUnit = dosageUnit.contains("kg/dekar") ? "g" : "ml";
        String type = safe(product.getApplication_type());
        if (type.isBlank()) type = "NUTRITION";

        for (int i = 0; i < zones.size(); i++) {
            if (!checked[i]) continue;
            GardenZone zone = zones.get(i);
            FertilizationProfile profile = zone.getFertilization();
            double dose = calculateBulkDose(product, profile);
            total += dose;
            tasks.add(repository.recordFertilizerApplication(
                    zone.getZone_id(), zone.getName(), product, dose, appliedUnit,
                    profile.getArea_m2(), profile.getTank_liters(), dose, dose,
                    false, "DAMLAMA", "Toplu uygulama kaydı", appliedAt, type
            ));
        }
        if (tasks.isEmpty()) {
            Toast.makeText(this, "En az bir bölge seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        final double totalAmount = total;
        Tasks.whenAllComplete(tasks).continueWithTask(task ->
                repository.deductBulkFertilizerStock(product, totalAmount, appliedUnit)
        ).addOnSuccessListener(unused -> Toast.makeText(this,
                "Uygulama seçilen bölgelere kaydedildi.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Kayıt tamamlanamadı: " + error.getMessage(), Toast.LENGTH_LONG).show());
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
        renderTodayAdvice();
    }

    private void renderTodayAdvice() {
        if (layoutTodayAdvice == null) {
            return;
        }
        layoutTodayAdvice.removeAllViews();
        if (currentZones.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Bölge verileri hazırlanıyor.");
            layoutTodayAdvice.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        long now = System.currentTimeMillis() / 1000L;
        for (GardenZone zone : currentZones) {
            FertilizerAdvice advice = FertilizerDecisionEngine.advise(
                    zone, currentProducts, currentWeather, now);
            View card = inflater.inflate(R.layout.item_fertilizer_today_advice,
                    layoutTodayAdvice, false);
            card.setOnClickListener(view -> openZoneDetails(zone));
            card.setClickable(true);
            card.setFocusable(true);
            ((TextView) card.findViewById(R.id.txtTodayAdviceZone)).setText(advice.getZoneTitle());
            ((TextView) card.findViewById(R.id.txtTodayAdviceStatus)).setText(advice.getStatus());
            ((TextView) card.findViewById(R.id.txtTodayAdviceReason)).setText(advice.getReason());
            TextView context = card.findViewById(R.id.txtTodayAdviceContext);
            context.setText(advice.getContext());
            context.setVisibility(advice.getContext().isBlank() ? View.GONE : View.VISIBLE);
            TextView products = card.findViewById(R.id.txtTodayAdviceProducts);
            if (advice.getCandidates().isEmpty()) {
                products.setVisibility(View.GONE);
            } else {
                products.setVisibility(View.VISIBLE);
                products.setText(primaryProductSummary(advice.getCandidates().get(0)));
            }
            TextView risks = card.findViewById(R.id.txtTodayAdviceRisks);
            if (advice.getRisks().isEmpty()) {
                risks.setVisibility(View.GONE);
            } else {
                risks.setVisibility(View.VISIBLE);
                risks.setText("Dikkat: " + advice.getRisks().get(0));
            }
            layoutTodayAdvice.addView(card);
        }
    }

    private void openZoneDetails(GardenZone zone) {
        Intent intent = new Intent(this, FertilizationZoneDetailActivity.class);
        intent.putExtra(FertilizationZoneDetailActivity.EXTRA_ZONE_ID,
                zone.getZone_id());
        startActivity(intent);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String primaryProductSummary(String candidate) {
        String[] lines = candidate.split("\\n");
        if (lines.length == 0) return "";
        StringBuilder summary = new StringBuilder("En uygun ürün: ")
                .append(lines[0]);
        if (lines.length > 1) {
            summary.append("\n").append(lines[1]);
        }
        return summary.toString();
    }

}
