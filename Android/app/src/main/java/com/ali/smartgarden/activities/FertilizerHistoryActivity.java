package com.ali.smartgarden.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
    private TextView outcomeSummary;
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
        outcomeSummary = findViewById(
                R.id.txtFertilizerOutcomeSummary
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
                .setItems(new String[]{
                                "Uygulama sonucunu kaydet",
                                getString(R.string.fertilizer_history_edit),
                                getString(R.string.fertilizer_history_delete)
                        },
                        (dialog, which) -> {
                            if (which == 0) {
                                showOutcomeDialog(value);
                            } else if (which == 1) {
                                showEditDialog(value);
                            } else {
                                confirmDelete(value);
                            }
                        })
                .show();
    }

    private void showOutcomeDialog(FertilizerApplication value) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);

        EditText dateInput = new EditText(this);
        dateInput.setHint("Gözlem tarihi");
        dateInput.setFocusable(false);
        LocalDate initialDate = value.getOutcome_observed_at_epoch() > 0
                ? Instant.ofEpochSecond(value.getOutcome_observed_at_epoch())
                .atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        dateInput.setText(initialDate.format(formatter));
        dateInput.setOnClickListener(view -> new DatePickerDialog(
                this,
                (picker, year, month, day) -> dateInput.setText(
                        LocalDate.of(year, month + 1, day).format(formatter)
                ),
                initialDate.getYear(), initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        ).show());

        Spinner status = new Spinner(this);
        String[] statuses = {"İyileşme gözlendi", "Belirgin değişiklik yok", "Sorun gözlendi"};
        status.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statuses));
        if ("UNCHANGED".equals(value.getOutcome_status())) {
            status.setSelection(1);
        } else if ("ISSUE".equals(value.getOutcome_status())) {
            status.setSelection(2);
        }

        Spinner vigor = new Spinner(this);
        String[] vigorValues = {"Canlılık puanı seçin", "1 / 5", "2 / 5", "3 / 5", "4 / 5", "5 / 5"};
        vigor.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, vigorValues));
        if (value.getOutcome_vigor_score() > 0
                && value.getOutcome_vigor_score() <= 5) {
            vigor.setSelection(value.getOutcome_vigor_score());
        }
        EditText notes = new EditText(this);
        notes.setHint("Gözlem notu (isteğe bağlı)");
        notes.setMinLines(2);
        notes.setText(value.getOutcome_notes());
        content.addView(dateInput);
        content.addView(status);
        content.addView(vigor);
        content.addView(notes);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Uygulama sonucu")
                .setMessage("Bu kayıt, AI Gübre Asistanı'nın sonraki önerilerini geliştirmek için kullanılır.")
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_save, (dialog, which) -> {
                    try {
                        LocalDate observed = LocalDate.parse(
                                dateInput.getText().toString(), formatter);
                        String[] values = {"IMPROVED", "UNCHANGED", "ISSUE"};
                        value.setOutcome_status(values[status.getSelectedItemPosition()]);
                        value.setOutcome_vigor_score(
                                Math.max(0, vigor.getSelectedItemPosition()));
                        value.setOutcome_notes(notes.getText().toString().trim());
                        value.setOutcome_observed_at_epoch(observed.atStartOfDay(
                                ZoneId.systemDefault()).toEpochSecond());
                        repository.updateFertilizerApplication(value)
                                .addOnSuccessListener(result -> showPhotoPrompt(value));
                    } catch (Exception ignored) {
                        android.widget.Toast.makeText(this,
                                "Geçerli bir gözlem tarihi seçin.",
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void showPhotoPrompt(FertilizerApplication value) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Uygulama sonucu kaydedildi")
                .setMessage("İsterseniz bu gözleme ait bir bitki fotoğrafını da yerel arşive ekleyebilirsiniz.")
                .setNegativeButton("Şimdi değil", null)
                .setPositiveButton("Fotoğraf ekle", (dialog, which) -> {
                    Intent intent = new Intent(this,
                            GardenPhotoArchiveActivity.class);
                    intent.putExtra("zone_id", value.getZone_id());
                    intent.putExtra("related_application_id",
                            value.getApplication_id());
                    startActivity(intent);
                })
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
        renderOutcomeSummary(values);
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

    private void renderOutcomeSummary(
            List<FertilizerApplication> values
    ) {
        int observed = 0;
        int improved = 0;
        int unchanged = 0;
        int issue = 0;
        double vigorTotal = 0.0;
        int vigorCount = 0;
        String latestProduct = "";
        long latestObservedAt = 0L;
        for (FertilizerApplication value : values) {
            String status = safe(value.getOutcome_status());
            if (status.isBlank()) {
                continue;
            }
            observed++;
            if ("IMPROVED".equals(status)) {
                improved++;
            } else if ("ISSUE".equals(status)) {
                issue++;
            } else {
                unchanged++;
            }
            if (value.getOutcome_vigor_score() > 0) {
                vigorTotal += value.getOutcome_vigor_score();
                vigorCount++;
            }
            if (value.getOutcome_observed_at_epoch() >= latestObservedAt) {
                latestObservedAt = value.getOutcome_observed_at_epoch();
                latestProduct = safe(value.getProduct_name());
            }
        }
        if (observed == 0) {
            outcomeSummary.setText(
                    "Henüz sonuç gözlemi yok. Bir uygulama kaydına dokunup “Uygulama sonucunu kaydet” seçin."
            );
            return;
        }
        String base = observed + " sonuç kaydı: " + improved
                + " iyileşme · " + unchanged + " değişiklik yok · " + issue
                + " sorun.";
        if (vigorCount > 0) {
            base += " Ortalama canlılık: "
                    + String.format(Locale.getDefault(), "%.1f", vigorTotal / vigorCount)
                    + "/5.";
        }
        if (observed < 3) {
            outcomeSummary.setText(base
                    + " Güvenilir eğilim için en az 3 gözlem gerekir.");
        } else if (issue > improved) {
            outcomeSummary.setText(base
                    + " Bu kayıtlar beklenen faydayı göstermiyor; doz, karışım ve etiket talimatı gözden geçirilmeli.");
        } else if (improved > issue) {
            outcomeSummary.setText(base
                    + (latestProduct.isBlank() ? " Olumlu eğilim gözleniyor."
                    : " Son gözlem: " + latestProduct + ". Olumlu eğilim gözleniyor."));
        } else {
            outcomeSummary.setText(base
                    + " Sonuçlar karışık; aynı uygulama koşullarında birkaç gözlem daha kaydedin.");
        }
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
                "AVORA-gubre-gecmisi.csv"
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
                    "Uygulama tarihi,Bölge,Ürün,Miktar,Birim,Yöntem,Alan m2,Tank L,Not,Sonuç,Canlılık puanı,Sonuç notu\n"
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
                        csv(value.getNotes()),
                        csv(outcomeLabel(value.getOutcome_status())),
                        csv(value.getOutcome_vigor_score() > 0
                                ? String.valueOf(value.getOutcome_vigor_score()) : ""),
                        csv(value.getOutcome_notes())
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

    private String outcomeLabel(String status) {
        if ("IMPROVED".equals(status)) return "İyileşme gözlendi";
        if ("UNCHANGED".equals(status)) return "Belirgin değişiklik yok";
        if ("ISSUE".equals(status)) return "Sorun gözlendi";
        return "";
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
