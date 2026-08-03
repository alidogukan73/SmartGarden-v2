package com.ali.smartgarden.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.DatePickerDialog;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.adapters.FertilizerHistoryAdapter;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.FertilizerApplication;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

public class FertilizerHistoryActivity extends AppCompatActivity {

    private final FirebaseRepository repository =
            new FirebaseRepository();
    private final FertilizerHistoryAdapter adapter =
            new FertilizerHistoryAdapter();
    private TextView empty;
    private RecyclerView recycler;
    private TextView count;
    private TextView products;
    private TextView last;
    private TextView usageSummary;
    private List<FertilizerApplication> allValues =
            Collections.emptyList();
    private String selectedZoneId = "";
    private List<FertilizerApplication> visibleValues =
            Collections.emptyList();
    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            exportCsv(result.getData().getData());
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_history);

        findViewById(R.id.btnBack).setOnClickListener(
                view -> finish()
        );
        empty = findViewById(R.id.txtHistoryEmpty);
        count = findViewById(R.id.txtFertilizerHistoryCount);
        products = findViewById(
                R.id.txtFertilizerHistoryProducts
        );
        last = findViewById(R.id.txtFertilizerHistoryLast);
        usageSummary = findViewById(
                R.id.txtFertilizerUsageSummary
        );
        recycler = findViewById(
                R.id.recyclerFertilizerHistory
        );
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setOnApplicationClickListener(
                this::showApplicationActions
        );
        findViewById(R.id.btnExportFertilizerHistory)
                .setOnClickListener(view -> requestExport());
        repository.observeFertilizerHistory().observe(
                this,
                this::render
        );
        ((ChipGroup) findViewById(
                R.id.chipGroupFertilizerZones
        )).setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty()
                    ? R.id.chipFertilizerAll
                    : checkedIds.get(0);
            if (checkedId == R.id.chipFertilizerTomato) {
                selectedZoneId = "zone-001";
            } else if (checkedId == R.id.chipFertilizerPepper) {
                selectedZoneId = "zone-002";
            } else if (checkedId
                    == R.id.chipFertilizerCucumber) {
                selectedZoneId = "zone-003";
            } else if (checkedId
                    == R.id.chipFertilizerEggplant) {
                selectedZoneId = "zone-004";
            } else if (checkedId == R.id.chipFertilizerBean) {
                selectedZoneId = "zone-005";
            } else {
                selectedZoneId = "";
            }
            applyFilter();
        });
    }

    private void render(List<FertilizerApplication> values) {
        allValues = values == null
                ? Collections.emptyList()
                : values;
        applyFilter();
    }

    private void showApplicationActions(FertilizerApplication value) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_history_action_title)
                .setMessage(value.getProduct_name())
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(
                        R.string.fertilizer_history_edit,
                        (dialog, which) -> showEditDialog(value)
                )
                .setPositiveButton(
                        R.string.fertilizer_history_delete,
                        (dialog, which) -> confirmDelete(value)
                )
                .show();
    }

    private void showEditDialog(FertilizerApplication value) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);
        EditText inputDate = new EditText(this);
        inputDate.setHint("Uygulama tarihi");
        inputDate.setFocusable(false);
        LocalDate date = Instant.ofEpochSecond(
                value.getApplied_at_epoch()
        ).atZone(ZoneId.systemDefault()).toLocalDate();
        inputDate.setText(date.format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        ));
        inputDate.setOnClickListener(view -> new DatePickerDialog(
                this,
                (picker, year, month, day) -> inputDate.setText(
                        LocalDate.of(year, month + 1, day).format(
                                DateTimeFormatter.ofPattern("dd-MM-yyyy")
                        )
                ),
                date.getYear(), date.getMonthValue() - 1,
                date.getDayOfMonth()
        ).show());
        EditText inputDose = new EditText(this);
        inputDose.setHint("Uygulanan miktar");
        inputDose.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        inputDose.setText(String.valueOf(value.getApplied_dose()));
        EditText inputNotes = new EditText(this);
        inputNotes.setHint("Uygulama notu");
        inputNotes.setText(value.getNotes());
        content.addView(inputDate);
        content.addView(inputDose);
        content.addView(inputNotes);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_history_edit)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_save,
                        (dialog, which) -> {
                            try {
                                double dose = Double.parseDouble(
                                        inputDose.getText().toString()
                                                .replace(',', '.')
                                );
                                LocalDate selected = LocalDate.parse(
                                        inputDate.getText().toString(),
                                        DateTimeFormatter.ofPattern(
                                                "dd-MM-yyyy"
                                        )
                                );
                                if (dose <= 0.0) {
                                    return;
                                }
                                long intervalSeconds = Math.max(
                                        86400L,
                                        value.getNext_application_at_epoch()
                                                - value
                                                .getApplied_at_epoch()
                                );
                                value.setApplied_dose(dose);
                                long appliedAt = selected
                                        .atStartOfDay(
                                                ZoneId.systemDefault()
                                        ).toEpochSecond();
                                value.setApplied_at_epoch(appliedAt);
                                value.setNext_application_at_epoch(
                                        appliedAt + intervalSeconds
                                );
                                value.setNotes(inputNotes.getText()
                                        .toString().trim());
                                repository.updateFertilizerApplication(value)
                                        .addOnSuccessListener(result ->
                                                android.widget.Toast.makeText(
                                                        this,
                                                        R.string
                                                                .fertilizer_history_updated,
                                                        android.widget.Toast.LENGTH_LONG
                                                ).show()
                                        );
                            } catch (Exception ignored) {
                                android.widget.Toast.makeText(
                                        this,
                                        "Geçerli tarih ve miktar girin.",
                                        android.widget.Toast.LENGTH_LONG
                                ).show();
                            }
                        })
                .show();
    }

    private void confirmDelete(FertilizerApplication value) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fertilizer_history_delete)
                .setMessage(
                        R.string.fertilizer_history_delete_confirm
                )
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.fertilizer_history_delete,
                        (dialog, which) -> repository
                                .deleteFertilizerApplication(
                                        value,
                                        new ArrayList<>(allValues)
                                )
                                .addOnSuccessListener(result ->
                                        android.widget.Toast.makeText(
                                                this,
                                                R.string
                                                        .fertilizer_history_deleted,
                                                android.widget.Toast.LENGTH_LONG
                                        ).show()
                                )
                )
                .show();
    }

    private void applyFilter() {
        List<FertilizerApplication> visible = new ArrayList<>();
        for (FertilizerApplication value : allValues) {
            if (selectedZoneId.isEmpty()
                    || selectedZoneId.equals(value.getZone_id())) {
                visible.add(value);
            }
        }
        adapter.submitList(visible);
        visibleValues = new ArrayList<>(visible);
        boolean isEmpty = visible.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        renderStatistics(visible);
    }

    private void renderStatistics(
            List<FertilizerApplication> values
    ) {
        count.setText(
                getString(
                        R.string.fertilizer_history_count,
                        values.size()
                )
        );
        Set<String> productIds = new HashSet<>();
        long latest = 0L;
        for (FertilizerApplication value : values) {
            if (value.getProduct_id() != null
                    && !value.getProduct_id().isBlank()) {
                productIds.add(value.getProduct_id());
            }
            latest = Math.max(latest, value.getApplied_at_epoch());
        }
        products.setText(
                getString(
                        R.string.fertilizer_history_products,
                        productIds.size()
                )
        );
        if (latest <= 0L) {
            last.setText(R.string.fertilizer_history_last_empty);
        } else {
            String date = Instant.ofEpochSecond(latest)
                    .atZone(ZoneId.systemDefault())
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd-MM-yyyy",
                                    new Locale("tr", "TR")
                            )
                    );
            last.setText(
                    getString(
                            R.string.fertilizer_history_last,
                            date
                    )
            );
        }
        renderUsageSummary(values);
    }

    private void renderUsageSummary(
            List<FertilizerApplication> values
    ) {
        double totalGram = 0.0;
        double totalMilliliter = 0.0;
        double last30Gram = 0.0;
        double last30Milliliter = 0.0;
        int drip = 0;
        int soil = 0;
        int foliar = 0;
        long thirtyDaysAgo = Instant.now()
                .minusSeconds(30L * 24L * 60L * 60L)
                .getEpochSecond();
        for (FertilizerApplication value : values) {
            boolean gram = "g".equalsIgnoreCase(
                    safe(value.getDose_unit())
            );
            boolean milliliter = "ml".equalsIgnoreCase(
                    safe(value.getDose_unit())
            );
            if (gram) {
                totalGram += value.getApplied_dose();
                if (value.getApplied_at_epoch() >= thirtyDaysAgo) {
                    last30Gram += value.getApplied_dose();
                }
            } else if (milliliter) {
                totalMilliliter += value.getApplied_dose();
                if (value.getApplied_at_epoch() >= thirtyDaysAgo) {
                    last30Milliliter += value.getApplied_dose();
                }
            }
            String method = safe(value.getApplication_method());
            if ("FOLIAR".equals(method)) {
                foliar++;
            } else if ("SOIL".equals(method)) {
                soil++;
            } else if ("DRIP".equals(method)) {
                drip++;
            }
        }
        if (totalGram <= 0.0 && totalMilliliter <= 0.0
                && drip == 0 && soil == 0 && foliar == 0) {
            usageSummary.setText(
                    R.string.fertilizer_history_usage_empty
            );
            return;
        }
        usageSummary.setText(
                getString(
                        R.string.fertilizer_history_usage_total,
                        formatAmount(totalGram),
                        formatAmount(totalMilliliter)
                ) + "\n" + getString(
                        R.string.fertilizer_history_usage_30d,
                        formatAmount(last30Gram),
                        formatAmount(last30Milliliter)
                ) + "\n" + getString(
                        R.string.fertilizer_history_usage_methods,
                        drip,
                        soil,
                        foliar
                )
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatAmount(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private void requestExport() {
        if (visibleValues.isEmpty()) {
            android.widget.Toast.makeText(
                    this,
                    R.string.fertilizer_history_export_empty,
                    android.widget.Toast.LENGTH_LONG
            ).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(
                Intent.EXTRA_TITLE,
                "smartgarden-gubre-gecmisi.csv"
        );
        exportLauncher.launch(intent);
    }

    private void exportCsv(Uri uri) {
        if (uri == null) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        getContentResolver().openOutputStream(uri),
                        java.nio.charset.StandardCharsets.UTF_8
                )
        )) {
            writer.write('\uFEFF');
            writer.write(
                    "Uygulama tarihi,Bölge,Ürün,Miktar,Birim,Yöntem,Alan m2,Tank L,Not\n"
            );
            for (FertilizerApplication value : visibleValues) {
                String date = value.getApplied_at_epoch() <= 0L
                        ? ""
                        : Instant.ofEpochSecond(
                                value.getApplied_at_epoch()
                        ).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern(
                                        "dd-MM-yyyy HH:mm",
                                        new Locale("tr", "TR")
                                ));
                writer.write(String.join(",",
                        csv(date),
                        csv(value.getZone_name()),
                        csv(value.getProduct_name()),
                        csv(formatAmount(value.getApplied_dose())),
                        csv(value.getDose_unit()),
                        csv(methodLabel(value.getApplication_method())),
                        csv(formatAmount(value.getArea_m2())),
                        csv(formatAmount(value.getTank_liters())),
                        csv(value.getNotes())
                ));
                writer.newLine();
            }
            android.widget.Toast.makeText(
                    this,
                    R.string.fertilizer_history_export_success,
                    android.widget.Toast.LENGTH_LONG
            ).show();
        } catch (Exception error) {
            android.widget.Toast.makeText(
                    this,
                    R.string.fertilizer_history_export_failed,
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }

    private String methodLabel(String method) {
        if ("FOLIAR".equals(method)) {
            return getString(R.string.fertilization_method_foliar);
        }
        if ("SOIL".equals(method)) {
            return getString(R.string.fertilization_method_soil);
        }
        return "DRIP".equals(method)
                ? getString(R.string.fertilization_method_drip)
                : "";
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
