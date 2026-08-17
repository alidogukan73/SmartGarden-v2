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
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.fertilization.FertilizerAdvice;
import com.ali.smartgarden.fertilization.FertilizerApplicationSafety;
import com.ali.smartgarden.fertilization.FertilizerDecisionEngine;
import com.ali.smartgarden.fertilization.FertilizerSafetyPolicy;
import com.ali.smartgarden.fertilization.OrganicFertilizerAiAdvisor;
import com.ali.smartgarden.fertilization.FertilizationPreferenceStore;
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
import java.util.UUID;

public class FertilizationCalendarActivity extends AppCompatActivity {

    private final FirebaseRepository repository =
            new FirebaseRepository();

    private LinearLayout layoutTodayAdvice;
    private List<GardenZone> currentZones = new ArrayList<>();
    private List<FertilizerProduct> currentProducts =
            new ArrayList<>();
    private List<FertilizerApplication> currentHistory =
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
        repository.observeFertilizerHistory().observe(this, history -> {
            currentHistory = history == null
                    ? new ArrayList<>()
                    : history;
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
            Toast.makeText(
                    this,
                    R.string.fertilizer_mix_requires_two_products,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_mix_select_first)
                .setItems(productNames(products), (dialog, index) ->
                        showMixSecondProductPicker(
                                products,
                                products.get(index)
                        ))
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void showMixSecondProductPicker(
            List<FertilizerProduct> products,
            FertilizerProduct first
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_mix_select_second)
                .setItems(productNames(products), (dialog, index) ->
                        showMixResult(first, products.get(index)))
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void showMixResult(
            FertilizerProduct first,
            FertilizerProduct second
    ) {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                first,
                second
        );
        String message = getString(
                R.string.fertilizer_mix_result_message,
                productName(first),
                productName(second),
                result.getMessage()
        );
        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(result.getTitle())
                        .setMessage(message);
        if (result.isBlocked()) {
            builder.setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        builder.setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(
                        R.string.fertilizer_mix_record_action,
                        (dialog, which) -> showMixZonePicker(
                                first,
                                second,
                                result
                        )
                )
                .show();
    }

    private void showMixZonePicker(
            FertilizerProduct first,
            FertilizerProduct second,
            FertilizerMixResult result
    ) {
        List<GardenZone> eligible = new ArrayList<>();
        for (GardenZone zone : currentZones) {
            FertilizationProfile profile = zone.getFertilization();
            if (profile == null || !profile.isEnabled()) {
                continue;
            }
            if (!FertilizerSafetyPolicy.isEligible(first, profile)
                    || !FertilizerSafetyPolicy.isEligible(second, profile)) {
                continue;
            }
            FertilizerApplicationSafety.Dose firstDose =
                    FertilizerApplicationSafety.calculateDose(
                            first,
                            profile
                    );
            FertilizerApplicationSafety.Dose secondDose =
                    FertilizerApplicationSafety.calculateDose(
                            second,
                            profile
                    );
            if (firstDose.isSupported() && secondDose.isSupported()) {
                eligible.add(zone);
            }
        }
        if (eligible.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_mix_no_eligible_zones,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        String[] names = new String[eligible.size()];
        boolean[] checked = new boolean[eligible.size()];
        for (int index = 0; index < eligible.size(); index++) {
            names[index] = safe(eligible.get(index).getEmoji()) + " "
                    + safe(eligible.get(index).getName());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_mix_select_zones)
                .setMultiChoiceItems(
                        names,
                        checked,
                        (dialog, which, selected) -> checked[which] = selected
                )
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(
                        R.string.fertilizer_bulk_select_date,
                        (dialog, which) -> showMixDatePicker(
                                first,
                                second,
                                result,
                                eligible,
                                checked
                        )
                )
                .show();
    }

    private void showMixDatePicker(
            FertilizerProduct first,
            FertilizerProduct second,
            FertilizerMixResult result,
            List<GardenZone> zones,
            boolean[] checked
    ) {
        if (!hasSelectedZone(checked)) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_bulk_choose_zone,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 12, 0, 0);
                    if (year == calendar.get(Calendar.YEAR)
                            && month == calendar.get(Calendar.MONTH)
                            && day == calendar.get(Calendar.DAY_OF_MONTH)) {
                        selected.setTimeInMillis(System.currentTimeMillis());
                    }
                    saveMixApplication(
                            first,
                            second,
                            result,
                            zones,
                            checked,
                            selected.getTimeInMillis() / 1000L
                    );
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void saveMixApplication(
            FertilizerProduct first,
            FertilizerProduct second,
            FertilizerMixResult result,
            List<GardenZone> zones,
            boolean[] checked,
            long appliedAt
    ) {
        if (result.isBlocked()) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_mix_blocked,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        if (appliedAt > System.currentTimeMillis() / 1000L + 60L) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_bulk_future_date,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        List<FirebaseRepository.BulkFertilizerApplication> firstApplications =
                new ArrayList<>();
        List<FirebaseRepository.BulkFertilizerApplication> secondApplications =
                new ArrayList<>();
        List<String> invalidZones = new ArrayList<>();
        List<String> stageBlockedZones = new ArrayList<>();
        List<String> repeatBlockedZones = new ArrayList<>();
        String firstUnit = "";
        String secondUnit = "";
        double firstTotal = 0.0;
        double secondTotal = 0.0;
        String firstType = FertilizerApplicationSafety.applicationType(first);
        String secondType = FertilizerApplicationSafety.applicationType(second);
        String mixGroupId = "mix-" + UUID.randomUUID();
        String riskLevel = result.getRiskLevel().name();

        for (int index = 0; index < zones.size(); index++) {
            if (!checked[index]) {
                continue;
            }
            GardenZone zone = zones.get(index);
            FertilizationProfile profile = zone.getFertilization();
            String zoneName = safe(zone.getName());
            FertilizerApplicationSafety.Dose firstDose =
                    FertilizerApplicationSafety.calculateDose(first, profile);
            FertilizerApplicationSafety.Dose secondDose =
                    FertilizerApplicationSafety.calculateDose(second, profile);
            if (profile == null || !profile.isEnabled()
                    || !firstDose.isSupported()
                    || !secondDose.isSupported()) {
                invalidZones.add(zoneName);
                continue;
            }
            if (!FertilizerSafetyPolicy.isEligible(first, profile)
                    || !FertilizerSafetyPolicy.isEligible(second, profile)) {
                stageBlockedZones.add(zoneName);
                continue;
            }
            boolean repeatBlocked =
                    FertilizerApplicationSafety.isRepeatIntervalBlocked(
                            profile,
                            firstType,
                            appliedAt
                    );
            if (!firstType.equals(secondType)) {
                repeatBlocked = repeatBlocked
                        || FertilizerApplicationSafety
                        .isRepeatIntervalBlocked(
                                profile,
                                secondType,
                                appliedAt
                        );
            }
            if (repeatBlocked) {
                repeatBlockedZones.add(zoneName);
                continue;
            }
            if (firstUnit.isEmpty()) {
                firstUnit = firstDose.getUnit();
            } else if (!firstUnit.equalsIgnoreCase(firstDose.getUnit())) {
                invalidZones.add(zoneName);
                continue;
            }
            if (secondUnit.isEmpty()) {
                secondUnit = secondDose.getUnit();
            } else if (!secondUnit.equalsIgnoreCase(secondDose.getUnit())) {
                invalidZones.add(zoneName);
                continue;
            }
            firstTotal += firstDose.getAmount();
            secondTotal += secondDose.getAmount();
            firstApplications.add(mixApplication(
                    zone,
                    profile,
                    firstDose,
                    first,
                    second,
                    firstType,
                    appliedAt,
                    mixGroupId,
                    riskLevel
            ));
            secondApplications.add(mixApplication(
                    zone,
                    profile,
                    secondDose,
                    second,
                    first,
                    secondType,
                    appliedAt,
                    mixGroupId,
                    riskLevel
            ));
        }

        if (!invalidZones.isEmpty()) {
            showBulkBlockingMessage(
                    R.string.fertilizer_mix_invalid_zone,
                    invalidZones
            );
            return;
        }
        if (!stageBlockedZones.isEmpty()) {
            showBulkBlockingMessage(
                    R.string.fertilizer_bulk_stage_blocked,
                    stageBlockedZones
            );
            return;
        }
        if (!repeatBlockedZones.isEmpty()) {
            showBulkBlockingMessage(
                    R.string.fertilizer_bulk_repeat_blocked,
                    repeatBlockedZones
            );
            return;
        }
        if (firstApplications.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_bulk_choose_zone,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        if (!validateMixStock(first, firstUnit, firstTotal)
                || !validateMixStock(second, secondUnit, secondTotal)) {
            return;
        }
        showMixConfirmation(
                first,
                second,
                result,
                firstApplications,
                secondApplications,
                firstUnit,
                secondUnit,
                firstTotal,
                secondTotal,
                appliedAt
        );
    }

    private FirebaseRepository.BulkFertilizerApplication mixApplication(
            GardenZone zone,
            FertilizationProfile profile,
            FertilizerApplicationSafety.Dose dose,
            FertilizerProduct product,
            FertilizerProduct partner,
            String applicationType,
            long appliedAt,
            String mixGroupId,
            String riskLevel
    ) {
        return new FirebaseRepository.BulkFertilizerApplication(
                zone.getZone_id(),
                safe(zone.getName()),
                dose.getAmount(),
                profile.getArea_m2(),
                profile.getTank_liters(),
                dose.getAmount(),
                dose.getAmount(),
                "DAMLAMA",
                getString(
                        R.string.fertilizer_mix_history_note,
                        productName(partner)
                ),
                appliedAt,
                applicationType,
                mixGroupId,
                partner.getProduct_id(),
                productName(partner),
                riskLevel
        );
    }

    private boolean validateMixStock(
            FertilizerProduct product,
            String appliedUnit,
            double total
    ) {
        if (safe(product.getStock_unit()).isEmpty()) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.fertilizer_mix_stock_unit_missing,
                            productName(product)
                    ),
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
        if (!FertilizerApplicationSafety.isStockUnitCompatible(
                product,
                appliedUnit
        )) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.fertilizer_mix_stock_unit_mismatch,
                            productName(product),
                            product.getStock_unit(),
                            appliedUnit
                    ),
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
        if (!FertilizerApplicationSafety.hasEnoughStock(product, total)) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.fertilizer_mix_stock_insufficient,
                            productName(product),
                            formatAmount(total),
                            appliedUnit,
                            formatAmount(product.getStock_amount())
                    ),
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
        return true;
    }

    private void showMixConfirmation(
            FertilizerProduct first,
            FertilizerProduct second,
            FertilizerMixResult result,
            List<FirebaseRepository.BulkFertilizerApplication>
                    firstApplications,
            List<FirebaseRepository.BulkFertilizerApplication>
                    secondApplications,
            String firstUnit,
            String secondUnit,
            double firstTotal,
            double secondTotal,
            long appliedAt
    ) {
        String date = new java.text.SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
        ).format(new java.util.Date(appliedAt * 1000L));
        String message = getString(
                R.string.fertilizer_mix_confirm_message,
                firstApplications.size(),
                productName(first),
                formatAmount(firstTotal),
                firstUnit,
                productName(second),
                formatAmount(secondTotal),
                secondUnit,
                date,
                result.getMessage()
        );
        com.google.android.material.checkbox.MaterialCheckBox confirmation =
                new com.google.android.material.checkbox.MaterialCheckBox(
                        this
                );
        confirmation.setText(R.string.fertilizer_mix_safety_confirmation);
        int padding = (int) (16 * getResources()
                .getDisplayMetrics().density);
        confirmation.setPadding(padding, padding / 2, padding, padding / 2);
        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.fertilizer_mix_confirm_title)
                        .setMessage(message)
                        .setView(confirmation)
                        .setNegativeButton(R.string.settings_cancel, null)
                        .setPositiveButton(
                                R.string.fertilizer_mix_confirm_action,
                                null
                        )
                        .create();
        dialog.setOnShowListener(unused -> dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener(view -> {
            if (!confirmation.isChecked()) {
                Toast.makeText(
                        this,
                        R.string.fertilizer_mix_confirmation_required,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            dialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
            ).setEnabled(false);
            List<FirebaseRepository.FertilizerApplicationBatch> batches =
                    new ArrayList<>();
            batches.add(new FirebaseRepository.FertilizerApplicationBatch(
                    first,
                    firstApplications,
                    firstUnit,
                    true
            ));
            batches.add(new FirebaseRepository.FertilizerApplicationBatch(
                    second,
                    secondApplications,
                    secondUnit,
                    true
            ));
            repository.recordFertilizerApplicationBatchesSafely(batches)
                    .addOnSuccessListener(unusedResult -> {
                        dialog.dismiss();
                        Toast.makeText(
                                this,
                                R.string.fertilizer_mix_saved,
                                Toast.LENGTH_LONG
                        ).show();
                    })
                    .addOnFailureListener(error -> {
                        dialog.getButton(
                                androidx.appcompat.app.AlertDialog
                                        .BUTTON_POSITIVE
                        ).setEnabled(true);
                        Toast.makeText(
                                this,
                                getString(
                                        R.string.fertilizer_mix_save_failed,
                                        safe(error.getMessage())
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        }));
        dialog.show();
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
        List<FertilizerProduct> products = enabledProducts();
        if (products.isEmpty()) {
            Toast.makeText(this, R.string.fertilizer_bulk_no_products, Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_bulk_select_product)
                .setItems(productNames(products), (dialog, which) ->
                        showBulkZonePicker(products.get(which)))
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void showBulkZonePicker(FertilizerProduct product) {
        List<GardenZone> eligible = new ArrayList<>();
        for (GardenZone zone : currentZones) {
            FertilizationProfile profile = zone.getFertilization();
            FertilizerApplicationSafety.Dose dose =
                    FertilizerApplicationSafety.calculateDose(product, profile);
            if (dose.isSupported()
                    && FertilizerSafetyPolicy.isEligible(product, profile)) {
                eligible.add(zone);
            }
        }
        if (eligible.isEmpty()) {
            Toast.makeText(this, R.string.fertilizer_bulk_no_eligible_zones, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[eligible.size()];
        boolean[] checked = new boolean[eligible.size()];
        for (int index = 0; index < eligible.size(); index++) {
            names[index] = safe(eligible.get(index).getEmoji()) + " "
                    + safe(eligible.get(index).getName());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_bulk_select_zones)
                .setMultiChoiceItems(names, checked,
                        (dialog, which, selected) -> checked[which] = selected)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.fertilizer_bulk_select_date,
                        (dialog, which) -> showBulkDatePicker(product, eligible, checked))
                .show();
    }

    private void showBulkDatePicker(
            FertilizerProduct product,
            List<GardenZone> zones,
            boolean[] checked
    ) {
        if (!hasSelectedZone(checked)) {
            Toast.makeText(this, R.string.fertilizer_bulk_choose_zone, Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 12, 0, 0);
            if (year == calendar.get(Calendar.YEAR)
                    && month == calendar.get(Calendar.MONTH)
                    && day == calendar.get(Calendar.DAY_OF_MONTH)) {
                selected.setTimeInMillis(System.currentTimeMillis());
            }
            saveBulkApplication(
                    product,
                    zones,
                    checked,
                    selected.getTimeInMillis() / 1000L
            );
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private boolean hasSelectedZone(boolean[] checked) {
        if (checked == null) {
            return false;
        }
        for (boolean selected : checked) {
            if (selected) {
                return true;
            }
        }
        return false;
    }

    private void saveBulkApplication(
            FertilizerProduct product,
            List<GardenZone> zones,
            boolean[] checked,
            long appliedAt
    ) {
        if (appliedAt > System.currentTimeMillis() / 1000L + 60L) {
            Toast.makeText(this, R.string.fertilizer_bulk_future_date, Toast.LENGTH_LONG).show();
            return;
        }

        List<FirebaseRepository.BulkFertilizerApplication> applications =
                new ArrayList<>();
        List<String> invalidZones = new ArrayList<>();
        List<String> stageBlockedZones = new ArrayList<>();
        List<String> repeatBlockedZones = new ArrayList<>();
        double total = 0.0;
        String appliedUnit = "";
        String type = FertilizerApplicationSafety.applicationType(product);

        for (int index = 0; index < zones.size(); index++) {
            if (!checked[index]) {
                continue;
            }
            GardenZone zone = zones.get(index);
            FertilizationProfile profile = zone.getFertilization();
            String zoneName = safe(zone.getName());
            FertilizerApplicationSafety.Dose dose =
                    FertilizerApplicationSafety.calculateDose(product, profile);
            if (profile == null || !profile.isEnabled() || !dose.isSupported()) {
                invalidZones.add(zoneName);
                continue;
            }
            if (!FertilizerSafetyPolicy.isEligible(product, profile)) {
                stageBlockedZones.add(zoneName);
                continue;
            }
            if (FertilizerApplicationSafety.isRepeatIntervalBlocked(
                    profile, type, appliedAt
            )) {
                repeatBlockedZones.add(zoneName);
                continue;
            }
            if (appliedUnit.isEmpty()) {
                appliedUnit = dose.getUnit();
            } else if (!appliedUnit.equalsIgnoreCase(dose.getUnit())) {
                invalidZones.add(zoneName);
                continue;
            }
            total += dose.getAmount();
            applications.add(new FirebaseRepository.BulkFertilizerApplication(
                    zone.getZone_id(),
                    zoneName,
                    dose.getAmount(),
                    profile.getArea_m2(),
                    profile.getTank_liters(),
                    dose.getAmount(),
                    dose.getAmount(),
                    "DAMLAMA",
                    getString(R.string.fertilizer_bulk_note),
                    appliedAt,
                    type
            ));
        }

        if (!invalidZones.isEmpty()) {
            showBulkBlockingMessage(R.string.fertilizer_bulk_invalid_zone, invalidZones);
            return;
        }
        if (!stageBlockedZones.isEmpty()) {
            showBulkBlockingMessage(
                    R.string.fertilizer_bulk_stage_blocked,
                    stageBlockedZones
            );
            return;
        }
        if (!repeatBlockedZones.isEmpty()) {
            showBulkBlockingMessage(
                    R.string.fertilizer_bulk_repeat_blocked,
                    repeatBlockedZones
            );
            return;
        }
        if (applications.isEmpty()) {
            Toast.makeText(this, R.string.fertilizer_bulk_choose_zone, Toast.LENGTH_SHORT).show();
            return;
        }
        if (safe(product.getStock_unit()).isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.fertilizer_bulk_stock_unit_missing,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        if (!FertilizerApplicationSafety.isStockUnitCompatible(product, appliedUnit)) {
            Toast.makeText(this, getString(
                    R.string.fertilizer_bulk_stock_unit_mismatch,
                    product.getStock_unit(),
                    appliedUnit
            ), Toast.LENGTH_LONG).show();
            return;
        }
        if (!FertilizerApplicationSafety.hasEnoughStock(product, total)) {
            Toast.makeText(this, getString(
                    R.string.fertilizer_bulk_stock_insufficient,
                    formatAmount(total),
                    appliedUnit,
                    formatAmount(product.getStock_amount())
            ), Toast.LENGTH_LONG).show();
            return;
        }

        String date = new java.text.SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
        ).format(new java.util.Date(appliedAt * 1000L));
        String message = getString(
                R.string.fertilizer_bulk_confirm_message,
                applications.size(),
                productName(product),
                formatAmount(total),
                appliedUnit,
                date
        );
        final String finalAppliedUnit = appliedUnit;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_bulk_confirm_title)
                .setMessage(message)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(
                        R.string.fertilizer_bulk_confirm_action,
                        (dialog, which) -> repository
                                .recordBulkFertilizerApplicationsSafely(
                                        product,
                                        applications,
                                        finalAppliedUnit,
                                        true
                                )
                                .addOnSuccessListener(unused -> Toast.makeText(
                                        this,
                                        R.string.fertilizer_bulk_success,
                                        Toast.LENGTH_LONG
                                ).show())
                                .addOnFailureListener(error -> Toast.makeText(
                                        this,
                                        getString(
                                                R.string.fertilizer_bulk_failure,
                                                safe(error.getMessage())
                                        ),
                                        Toast.LENGTH_LONG
                                ).show())
                )
                .show();
    }

    private void showBulkBlockingMessage(
            int messageResource,
            List<String> zoneNames
    ) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(
                        messageResource,
                        String.join(", ", zoneNames)
                ))
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
                    zone, currentProducts, currentWeather, currentHistory, now,
                    new FertilizationPreferenceStore(this).preferOrganicInputs()
            );
            View card = inflater.inflate(R.layout.item_fertilizer_today_advice,
                    layoutTodayAdvice, false);
            card.setOnClickListener(view -> openZoneDetails(zone));
            card.setClickable(true);
            card.setFocusable(true);
            ((TextView) card.findViewById(R.id.txtTodayAdviceZone)).setText(advice.getZoneTitle());
            long waitDays = summaryWaitDays(zone, advice);
            boolean waiting = waitDays > 0L;
            TextView status = card.findViewById(R.id.txtTodayAdviceStatus);
            status.setText(summaryStatus(advice, waitDays));
            status.setTextColor(ContextCompat.getColor(
                    this,
                    summaryStatusColor(advice, waiting)
            ));
            ((TextView) card.findViewById(R.id.txtTodayAdviceReason))
                    .setText(summaryAction(advice, waiting));
            TextView context = card.findViewById(R.id.txtTodayAdviceContext);
            String support = summarySupport(zone, advice, waiting);
            context.setText(support);
            context.setVisibility(support.isBlank() ? View.GONE : View.VISIBLE);
            TextView products = card.findViewById(R.id.txtTodayAdviceProducts);
            if (advice.getCandidates().isEmpty()) {
                if (OrganicFertilizerAiAdvisor.isRequired(advice)) {
                    products.setVisibility(View.VISIBLE);
                    products.setText(R.string.fertilizer_organic_ai_loading);
                    requestOrganicAiAdvice(products, zone, true);
                } else {
                    products.setVisibility(View.GONE);
                }
            } else {
                products.setVisibility(View.VISIBLE);
                products.setText(primaryProductSummary(
                        advice.getCandidates().get(0),
                        waiting
                ));
            }
            renderCompactExperience(
                    card.findViewById(R.id.txtTodayAdviceExperience),
                    advice.getExperience()
            );
            TextView risks = card.findViewById(R.id.txtTodayAdviceRisks);
            int visibleRiskCount = advice.getRisks().size();
            if (waiting && hasMinimumIntervalRisk(advice)) {
                visibleRiskCount = Math.max(0, visibleRiskCount - 1);
            }
            if (visibleRiskCount == 0) {
                risks.setVisibility(View.GONE);
            } else {
                risks.setVisibility(View.VISIBLE);
                risks.setText(getString(
                        R.string.fertilizer_today_risk_notes,
                        visibleRiskCount
                ));
            }
            layoutTodayAdvice.addView(card);
        }
    }


    private void requestOrganicAiAdvice(TextView target,
                                        GardenZone zone,
                                        boolean compact) {
        OrganicFertilizerAiAdvisor.request(zone,
                new OrganicFertilizerAiAdvisor.Callback() {
                    @Override
                    public void onResult(OrganicFertilizerAiAdvisor.Result result) {
                        if (isFinishing() || isDestroyed()) return;
                        String content = compact
                                ? result.compactText() : result.fullText(FertilizationCalendarActivity.this);
                        target.setText(getString(
                                R.string.fertilizer_organic_ai_heading)
                                + "\n" + content);
                    }

                    @Override
                    public void onUnavailable() {
                        if (isFinishing() || isDestroyed()) return;
                        target.setText(R.string.fertilizer_organic_ai_unavailable);
                    }
                });
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

    private String primaryProductSummary(String candidate, boolean waiting) {
        String[] lines = candidate.split("\\n");
        if (lines.length == 0) return "";
        String productName = lines[0]
                .replaceFirst("^[★☆\\s]+", "")
                .trim();
        StringBuilder summary = new StringBuilder(getString(
                waiting
                        ? R.string.fertilizer_today_product_next
                        : R.string.fertilizer_today_product_recommended
        )).append("\n").append(productName);
        if (lines.length > 1) {
            String[] tags = lines[1].split("\\s+·\\s+");
            summary.append("\n");
            for (int index = 0; index < tags.length; index++) {
                if (index > 0) summary.append("   ");
                summary.append("✓ ").append(tags[index].trim());
            }
        }
        return summary.toString();
    }

    private String summaryStatus(FertilizerAdvice advice, long waitDays) {
        if (waitDays > 0L) {
            return getString(R.string.fertilizer_today_status_wait, waitDays);
        }
        if ("BUGÜNKÜ ÖNERİ".equals(advice.getStatus())) {
            return getString(R.string.fertilizer_today_status_ready);
        }
        if ("VERİYİ YENİLEYİN".equals(advice.getStatus())
                || "ÖNCE SULAMA".equals(advice.getStatus())) {
            return getString(R.string.fertilizer_today_status_check);
        }
        return advice.getStatus();
    }

    private int summaryStatusColor(FertilizerAdvice advice, boolean waiting) {
        if (waiting
                || "ORGANİK ÜRÜN GEREKİYOR".equals(advice.getStatus())
                || "HAZIRLIK GEREKİYOR".equals(advice.getStatus())
                || "HENÜZ ERKEN".equals(advice.getStatus())
                || "VERİYİ YENİLEYİN".equals(advice.getStatus())
                || "ÖNCE SULAMA".equals(advice.getStatus())) {
            return R.color.warning;
        }
        return R.color.primary;
    }

    private String summaryAction(FertilizerAdvice advice, boolean waiting) {
        if (waiting) {
            return getString(R.string.fertilizer_today_action_wait);
        }
        switch (advice.getStatus()) {
            case "BUGÜNKÜ ÖNERİ":
                return getString(R.string.fertilizer_today_action_ready);
            case "ORGANİK ÜRÜN GEREKİYOR":
                return getString(
                        R.string.fertilizer_today_action_organic_missing
                );
            case "HAZIRLIK GEREKİYOR":
                return getString(R.string.fertilizer_today_action_prepare);
            case "ÖNCE SULAMA":
                return getString(
                        R.string.fertilizer_today_action_water_first
                );
            case "VERİYİ YENİLEYİN":
                return getString(
                        R.string.fertilizer_today_action_refresh_data
                );
            default:
                return advice.getReason();
        }
    }

    private String summarySupport(GardenZone zone,
                                  FertilizerAdvice advice,
                                  boolean waiting) {
        if (waiting) {
            return getString(R.string.fertilizer_today_support_wait);
        }
        if ("BUGÜNKÜ ÖNERİ".equals(advice.getStatus())) {
            boolean organicStage = zone.getFertilization() != null
                    && FertilizerSafetyPolicy.requiresOrganicProduct(
                    zone.getFertilization()
            );
            return getString(organicStage
                    ? R.string.fertilizer_today_support_ready_organic
                    : R.string.fertilizer_today_support_ready);
        }
        if ("ORGANİK ÜRÜN GEREKİYOR".equals(advice.getStatus())) {
            return getString(
                    R.string.fertilizer_today_support_organic_missing
            );
        }
        if ("HAZIRLIK GEREKİYOR".equals(advice.getStatus())) {
            return advice.getReason();
        }
        return "";
    }

    private long summaryWaitDays(GardenZone zone, FertilizerAdvice advice) {
        long waitDays = extractWaitDays(advice.getReason());
        for (String risk : advice.getRisks()) {
            waitDays = Math.max(waitDays, extractWaitDays(risk));
        }
        if (waitDays <= 0L) return 0L;
        if ("HENÜZ ERKEN".equals(advice.getStatus())) return waitDays;

        FertilizationProfile profile = zone.getFertilization();
        FertilizerAdvice.Experience experience = advice.getExperience();
        if (profile == null || experience == null) return 0L;
        String activeProductId = safe(profile.getActive_product_id());
        String recommendedProductId = safe(experience.getProductId());
        return !activeProductId.isBlank()
                && activeProductId.equals(recommendedProductId)
                ? waitDays : 0L;
    }

    private long extractWaitDays(String value) {
        if (value == null || value.isBlank()) return 0L;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?i)(?:son uygulamadan sonra|tekrar uygulama)\\s+(\\d+)\\s+g(?:u|ü)n"
        ).matcher(value);
        if (!matcher.find()) return 0L;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean hasMinimumIntervalRisk(FertilizerAdvice advice) {
        for (String risk : advice.getRisks()) {
            if (extractWaitDays(risk) > 0L) return true;
        }
        return false;
    }

    private void renderCompactExperience(
            TextView target,
            FertilizerAdvice.Experience experience
    ) {
        if (experience == null || experience.getObservations() <= 0) {
            target.setVisibility(View.GONE);
            return;
        }
        target.setVisibility(View.VISIBLE);
        target.setText(experience.isReliable()
                ? getString(
                R.string.fertilizer_today_experience_reliable,
                experience.getObservations(),
                experience.getSuccessScore()
        ) : getString(
                R.string.fertilizer_today_experience_learning,
                experience.getObservations()
        ));
    }

}
