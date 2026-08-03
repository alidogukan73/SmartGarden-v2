package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.adapters.FertilizerProductAdapter;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.FertilizerProduct;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class FertilizerProductsActivity extends AppCompatActivity {

    private final FirebaseRepository repository =
            new FirebaseRepository();
    private final FertilizerProductAdapter adapter =
            new FertilizerProductAdapter();
    private TextView txtEmpty;
    private TextView txtProductStockSummary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_products);

        txtEmpty = findViewById(R.id.txtEmpty);
        txtProductStockSummary = findViewById(
                R.id.txtProductStockSummary
        );
        RecyclerView recycler = findViewById(R.id.recyclerProducts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);

        findViewById(R.id.btnBack).setOnClickListener(
                view -> finish()
        );
        findViewById(R.id.btnAddProduct).setOnClickListener(
                view -> showProductDialog(null)
        );
        adapter.setOnProductClickListener(this::showProductDialog);

        repository.observeFertilizerProducts().observe(
                this,
                this::renderProducts
        );
    }

    private void renderProducts(List<FertilizerProduct> products) {
        adapter.submitList(products);
        txtEmpty.setVisibility(
                products == null || products.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
        StringBuilder summary = new StringBuilder();
        for (FertilizerProduct product : products) {
            if (!product.isEnabled()
                    || product.getStock_unit() == null
                    || product.getStock_unit().isBlank()) {
                continue;
            }
            String status = product.getStock_amount() <= 0.0
                    ? getString(R.string.fertilizer_stock_empty_status)
                    : product.getLow_stock_threshold() > 0.0
                    && product.getStock_amount()
                    <= product.getLow_stock_threshold()
                    ? getString(R.string.fertilizer_stock_status_low)
                    : getString(R.string.fertilizer_stock_ok);
            if (summary.length() > 0) {
                summary.append("\n");
            }
            summary.append(product.getName()).append(" · ")
                    .append(formatNumber(product.getStock_amount()))
                    .append(" ").append(product.getStock_unit())
                    .append(" · ").append(status);
        }
        txtProductStockSummary.setText(summary.length() == 0
                ? getString(R.string.fertilizer_stock_summary_empty)
                : summary.toString());
    }

    private void showProductDialog(
            @Nullable FertilizerProduct existing
    ) {
        View content = LayoutInflater.from(this).inflate(
                R.layout.dialog_fertilizer_product,
                null,
                false
        );
        TextInputEditText inputName =
                content.findViewById(R.id.inputName);
        TextInputEditText inputNpk =
                content.findViewById(R.id.inputNpk);
        TextInputEditText inputDose =
                content.findViewById(R.id.inputDose);
        TextInputEditText inputInterval =
                content.findViewById(R.id.inputInterval);
        TextInputEditText inputStockAmount =
                content.findViewById(R.id.inputStockAmount);
        TextInputEditText inputLowStockThreshold =
                content.findViewById(R.id.inputLowStockThreshold);
        MaterialAutoCompleteTextView dropdownForm =
                content.findViewById(R.id.dropdownForm);
        MaterialAutoCompleteTextView dropdownUnit =
                content.findViewById(R.id.dropdownUnit);
        MaterialAutoCompleteTextView dropdownStockUnit =
                content.findViewById(R.id.dropdownStockUnit);
        MaterialAutoCompleteTextView dropdownApplicationType =
                content.findViewById(R.id.dropdownApplicationType);

        String[] forms = {"Sıvı", "Toz", "Granül"};
        String[] units = {
                "kg/dekar",
                "L/dekar",
                "ml / 100 L su",
                "g / 100 L su",
                "ml / litre su",
                "g / litre su",
                "ml / bitki",
                "g / bitki"
        };
        dropdownForm.setSimpleItems(forms);
        dropdownUnit.setSimpleItems(units);
        String[] stockUnits = {"kg", "g", "L", "ml"};
        String[] applicationTypes = {
                getString(R.string.fertilizer_type_nutrition),
                getString(R.string.fertilizer_type_organic),
                getString(R.string.fertilizer_type_conditioner),
                getString(R.string.fertilizer_type_biostimulant)
        };
        dropdownStockUnit.setSimpleItems(stockUnits);
        dropdownApplicationType.setSimpleItems(applicationTypes);

        if (existing == null) {
            dropdownForm.setText(forms[0], false);
            dropdownUnit.setText(units[0], false);
            inputInterval.setText("14");
            dropdownStockUnit.setText(stockUnits[0], false);
            dropdownApplicationType.setText(applicationTypes[0], false);
        } else {
            inputName.setText(existing.getName());
            inputNpk.setText(existing.getNpk());
            inputDose.setText(
                    String.valueOf(existing.getLabel_dosage())
            );
            inputInterval.setText(
                    String.valueOf(
                            existing.getMinimum_interval_days()
                    )
            );
            dropdownForm.setText(
                    "GRANULAR".equals(existing.getForm())
                            ? forms[2]
                            : "POWDER".equals(existing.getForm())
                            ? forms[1]
                            : forms[0],
                    false
            );
            dropdownUnit.setText(existing.getDosage_unit(), false);
            if (existing.getStock_amount() > 0.0) {
                inputStockAmount.setText(
                        formatNumber(existing.getStock_amount())
                );
            }
            if (existing.getLow_stock_threshold() > 0.0) {
                inputLowStockThreshold.setText(
                        formatNumber(
                                existing.getLow_stock_threshold()
                        )
                );
            }
            dropdownStockUnit.setText(
                    existing.getStock_unit() == null
                            || existing.getStock_unit().isBlank()
                            ? ("LIQUID".equals(existing.getForm())
                            ? stockUnits[1] : stockUnits[0])
                            : existing.getStock_unit(),
                    false
            );
            dropdownApplicationType.setText(
                    applicationTypeLabel(
                            existing.getApplication_type()
                    ),
                    false
            );
        }

        MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null
                        ? R.string.fertilizer_add_product
                        : R.string.fertilizer_edit_product)
                .setView(content)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_save, null);
        if (existing != null) {
            dialogBuilder.setNeutralButton(
                    R.string.fertilizer_remove,
                    null
            );
        }
        AlertDialog dialog = dialogBuilder.create();

        dialog.setOnShowListener(unused -> {
            if (existing != null) {
                arrangeEditDialogButtons(dialog);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(view -> {
                            FertilizerProduct product =
                                    readProduct(
                                            existing,
                                            inputName,
                                            inputNpk,
                                            inputDose,
                                            inputInterval,
                                            dropdownForm,
                                            dropdownUnit,
                                            inputStockAmount,
                                            inputLowStockThreshold,
                                            dropdownStockUnit,
                                            dropdownApplicationType
                                    );
                            if (product == null) {
                                Toast.makeText(
                                        this,
                                        R.string.fertilizer_required_fields,
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }
                            repository.saveFertilizerProduct(product)
                                    .addOnSuccessListener(result -> {
                                        dialog.dismiss();
                                        Toast.makeText(
                                                this,
                                                R.string.fertilizer_product_saved,
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    })
                                    .addOnFailureListener(error ->
                                            Toast.makeText(
                                                    this,
                                                    R.string.fertilization_save_failed,
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        });
            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(view ->
                                checkAndRemoveProduct(
                                        dialog,
                                        existing
                                )
                        );
            }
        });
        dialog.show();
    }

    private void arrangeEditDialogButtons(AlertDialog dialog) {
        View save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        View remove = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        View cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (!(save.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup panel = (ViewGroup) save.getParent();
        panel.removeView(save);
        panel.removeView(remove);
        panel.removeView(cancel);
        panel.addView(save);
        panel.addView(remove);
        panel.addView(cancel);
    }

    private void checkAndRemoveProduct(
            AlertDialog editDialog,
            FertilizerProduct product
    ) {
        editDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setEnabled(false);
        repository.findActiveZonesUsingFertilizer(
                product.getProduct_id()
        ).addOnSuccessListener(zoneNames -> {
            editDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setEnabled(true);
            if (zoneNames != null && !zoneNames.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.fertilizer_in_use_title)
                        .setMessage(getString(
                                R.string.fertilizer_in_use_message,
                                String.join(", ", zoneNames)
                        ))
                        .setPositiveButton(
                                android.R.string.ok,
                                null
                        )
                        .show();
                return;
            }
            confirmRemoveProduct(editDialog, product);
        }).addOnFailureListener(error -> {
            editDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setEnabled(true);
            Toast.makeText(
                    this,
                    R.string.fertilization_save_failed,
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void confirmRemoveProduct(
            AlertDialog editDialog,
            FertilizerProduct product
    ) {
        boolean systemProduct = product.isVerified();
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        systemProduct
                                ? R.string.fertilizer_deactivate_title
                                : R.string.fertilizer_delete_title
                )
                .setMessage(getString(
                        systemProduct
                                ? R.string.fertilizer_deactivate_message
                                : R.string.fertilizer_delete_message,
                        product.getName()
                ))
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(
                        R.string.fertilizer_remove_confirm,
                        (dialog, which) -> {
                            com.google.android.gms.tasks.Task<Void> task =
                                    systemProduct
                                            ? repository
                                            .deactivateFertilizerProduct(
                                                    product.getProduct_id()
                                            )
                                            : repository
                                            .deleteFertilizerProduct(
                                                    product.getProduct_id()
                                            );
                            task.addOnSuccessListener(unused -> {
                                editDialog.dismiss();
                                Toast.makeText(
                                        this,
                                        systemProduct
                                                ? R.string
                                                .fertilizer_deactivated
                                                : R.string
                                                .fertilizer_deleted,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }).addOnFailureListener(error ->
                                    Toast.makeText(
                                            this,
                                            R.string
                                                    .fertilization_save_failed,
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                        }
                )
                .show();
    }

    @Nullable
    private FertilizerProduct readProduct(
            @Nullable FertilizerProduct existing,
            TextInputEditText inputName,
            TextInputEditText inputNpk,
            TextInputEditText inputDose,
            TextInputEditText inputInterval,
            MaterialAutoCompleteTextView dropdownForm,
            MaterialAutoCompleteTextView dropdownUnit,
            TextInputEditText inputStockAmount,
            TextInputEditText inputLowStockThreshold,
            MaterialAutoCompleteTextView dropdownStockUnit,
            MaterialAutoCompleteTextView dropdownApplicationType
    ) {
        String name = text(inputName);
        String doseText = text(inputDose).replace(',', '.');
        String intervalText = text(inputInterval);
        String unit = dropdownUnit.getText().toString().trim();
        if (name.isBlank() || doseText.isBlank()
                || intervalText.isBlank() || unit.isBlank()) {
            return null;
        }
        try {
            double dose = Double.parseDouble(doseText);
            int interval = Integer.parseInt(intervalText);
            if (dose <= 0 || interval < 1 || interval > 365) {
                return null;
            }
            FertilizerProduct product = existing == null
                    ? new FertilizerProduct()
                    : existing;
            product.setName(name);
            product.setNpk(text(inputNpk));
            product.setApplication_type(
                    applicationTypeCode(
                            dropdownApplicationType.getText().toString()
                    )
            );
            product.setLabel_dosage(dose);
            product.setMinimum_interval_days(interval);
            product.setDosage_unit(unit);
            product.setForm(
                    "Granül".contentEquals(dropdownForm.getText())
                            ? "GRANULAR"
                            : "Toz".contentEquals(
                                    dropdownForm.getText()
                            )
                            ? "POWDER"
                            : "LIQUID"
            );
            product.setEnabled(true);
            product.setStock_amount(
                    optionalNumber(inputStockAmount)
            );
            product.setLow_stock_threshold(
                    optionalNumber(inputLowStockThreshold)
            );
            product.setStock_unit(
                    dropdownStockUnit.getText().toString().trim()
            );
            return product;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String text(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    private double optionalNumber(TextInputEditText input) {
        String value = text(input).replace(',', '.');
        if (value.isBlank()) {
            return 0.0;
        }
        try {
            return Math.max(0.0, Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private String formatNumber(double value) {
        return value == Math.rint(value)
                ? String.format(java.util.Locale.getDefault(), "%.0f", value)
                : String.format(java.util.Locale.getDefault(), "%.2f", value);
    }

    private String applicationTypeCode(String label) {
        if (getString(R.string.fertilizer_type_organic).equals(label)) {
            return "ORGANIC";
        }
        if (getString(R.string.fertilizer_type_conditioner).equals(label)) {
            return "CONDITIONER";
        }
        if (getString(R.string.fertilizer_type_biostimulant).equals(label)) {
            return "BIOSTIMULANT";
        }
        return "NUTRITION";
    }

    private String applicationTypeLabel(String type) {
        if ("ORGANIC".equals(type)) {
            return getString(R.string.fertilizer_type_organic);
        }
        if ("CONDITIONER".equals(type)) {
            return getString(R.string.fertilizer_type_conditioner);
        }
        if ("BIOSTIMULANT".equals(type)) {
            return getString(R.string.fertilizer_type_biostimulant);
        }
        return getString(R.string.fertilizer_type_nutrition);
    }
}
