package com.ali.smartgarden.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.card.MaterialCardView;

import java.util.Collections;
import java.util.List;

public class WateringControlActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TextView pumpState;
    private TextView pumpDescription;
    private TextView autoDescription;
    private TextView safety;
    private MaterialSwitch autoSwitch;
    private MaterialSwitch pumpSwitch;
    private MaterialButton pumpButton;
    private MaterialCardView pumpStatusCard;
    private MaterialCardView pumpIconCard;
    private AppCompatImageView pumpIcon;
    private LinearLayout manualValves;
    private TextView manualValveSafety;
    private List<GardenZone> zones = Collections.emptyList();
    private String activeValveId = "";
    private boolean valveOpen;
    private boolean physicalValveMode;
    private boolean updatingValveSwitch;
    private boolean relayOn;
    private boolean updatingSwitch;
    private boolean updatingPumpSwitch;
    private long lastStatusElapsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_watering_control);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.wateringControlRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );
                    return insets;
                }
        );

        pumpState = findViewById(R.id.txtControlPumpState);
        pumpDescription = findViewById(
                R.id.txtControlPumpDescription
        );
        autoDescription = findViewById(
                R.id.txtControlAutoDescription
        );
        safety = findViewById(R.id.txtControlSafety);
        autoSwitch = findViewById(R.id.switchControlAuto);
        pumpSwitch = findViewById(R.id.switchControlPump);
        pumpButton = findViewById(R.id.btnControlPump);
        pumpStatusCard = findViewById(
                R.id.cardControlPumpStatus
        );
        pumpIconCard = findViewById(
                R.id.cardControlPumpIcon
        );
        pumpIcon = findViewById(R.id.imgControlPump);
        manualValves = findViewById(
                R.id.layoutManualValves
        );
        manualValveSafety = findViewById(
                R.id.txtManualValveSafety
        );
        findViewById(R.id.btnBack).setOnClickListener(
                view -> finish()
        );

        viewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);
        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getCommand().observe(this, this::renderCommand);
        viewModel.getGardenZones().observe(
                this,
                items -> {
                    zones = items != null
                            ? items
                            : Collections.emptyList();
                    renderManualValves();
                }
        );
        viewModel.getError().observe(this, message -> {
            if (message != null && !message.isBlank()) {
                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        autoSwitch.setOnCheckedChangeListener(
                (button, checked) -> {
                    if (updatingSwitch) {
                        return;
                    }
                    if (!isDeviceOnline()) {
                        updatingSwitch = true;
                        autoSwitch.setChecked(!checked);
                        updatingSwitch = false;
                        Toast.makeText(
                                this,
                                "Cihaz çevrimdışıyken otomatik mod değiştirilemez.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (!valveOpen) {
                        Toast.makeText(
                                this,
                                "Pompa açılamadı: önce bir vana açın.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (!physicalValveMode) {
                        Toast.makeText(
                                this,
                                "Pompa koruma altında: vanalar simülasyon modunda.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                    viewModel.setAutoMode(checked);
                    if (checked) {
                        viewModel.setRelay(false);
                    }
                    renderAuto(checked);
                }
        );

        pumpSwitch.setOnCheckedChangeListener(
                (button, checked) -> {
                    if (updatingPumpSwitch) {
                        return;
                    }

                    if (!checked) {
                        viewModel.setRelay(false);
                        return;
                    }

                    updatingPumpSwitch = true;
                    pumpSwitch.setChecked(false);
                    updatingPumpSwitch = false;

                    if (!isDeviceOnline()) {
                        Toast.makeText(
                                this,
                                "Pompa başlatılamadı: cihaz çevrimdışı.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    new MaterialAlertDialogBuilder(this)
                            .setTitle(
                                    R.string.manual_relay_test_title
                            )
                            .setMessage(
                                    R.string.manual_relay_test_message
                            )
                            .setNegativeButton(
                                    R.string.manual_relay_test_cancel,
                                    null
                            )
                            .setPositiveButton(
                                    R.string.manual_relay_test_confirm,
                                    (dialog, which) ->
                                            viewModel.setRelay(true)
                            )
                            .show();
                }
        );

        pumpButton.setOnClickListener(view -> {
            if (relayOn) {
                viewModel.setRelay(false);
                return;
            }
            if (!isDeviceOnline()) {
                Toast.makeText(
                        this,
                        "Pompa başlatılamadı: cihaz çevrimdışı.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.manual_relay_test_title)
                    .setMessage(R.string.manual_relay_test_message)
                    .setNegativeButton(
                            R.string.manual_relay_test_cancel,
                            null
                    )
                    .setPositiveButton(
                            R.string.manual_relay_test_confirm,
                            (dialog, which) ->
                                    viewModel.setRelay(true)
                    )
                    .show();
        });
    }

    private void renderStatus(Status status) {
        if (status == null) {
            return;
        }
        lastStatusElapsed = SystemClock.elapsedRealtime();
        relayOn = status.isRelay();
        activeValveId = status.getActiveValveId() == null
                ? ""
                : status.getActiveValveId();
        valveOpen = status.isValveOpen()
                && !activeValveId.isBlank();
        physicalValveMode = "PHYSICAL".equalsIgnoreCase(
                status.getValveMode()
        );
        renderPump();
        renderManualValves();
    }

    private void renderCommand(Command command) {
        if (command == null) {
            return;
        }
        relayOn = command.isRelay();
        updatingSwitch = true;
        autoSwitch.setChecked(command.isAutoMode());
        updatingSwitch = false;
        renderAuto(command.isAutoMode());
        renderPump();
    }

    private void renderPump() {
        updatingPumpSwitch = true;
        pumpSwitch.setChecked(relayOn);
        updatingPumpSwitch = false;

        int color = ContextCompat.getColor(
                this,
                relayOn ? R.color.online : R.color.textSecondary
        );
        pumpState.setText(
                relayOn
                        ? R.string.pump_running
                        : R.string.pump_stopped
        );
        pumpState.setTextColor(color);
        pumpStatusCard.setStrokeColor(
                relayOn
                        ? ContextCompat.getColor(
                                this,
                                R.color.primary
                        )
                        : ContextCompat.getColor(
                                this,
                                R.color.border
                        )
        );
        pumpStatusCard.setCardBackgroundColor(
                ContextCompat.getColor(
                        this,
                        relayOn
                                ? R.color.surfaceGreen
                                : R.color.surfaceSoft
                )
        );
        pumpIconCard.setCardBackgroundColor(
                ContextCompat.getColor(
                        this,
                        relayOn
                                ? R.color.primaryLight
                                : R.color.offlineBackground
                )
        );
        pumpIcon.setImageTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                relayOn
                                        ? R.color.primary
                                        : R.color.offline
                        )
                )
        );
        pumpDescription.setText(
                relayOn
                        ? R.string.pump_description_running
                        : R.string.pump_description_idle
        );
        pumpButton.setText(
                relayOn
                        ? "Pompayı durdur"
                        : getString(R.string.manual_relay_test_button)
        );
        pumpButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                relayOn
                                        ? R.color.warning
                                        : R.color.primary
                        )
                )
        );
    }

    private void renderAuto(boolean enabled) {
        autoDescription.setText(
                enabled
                        ? R.string.auto_mode_active_description
                        : R.string.auto_mode_inactive_description
        );
    }

    private boolean isDeviceOnline() {
        return lastStatusElapsed > 0L
                && SystemClock.elapsedRealtime()
                - lastStatusElapsed <= 30_000L;
    }

    private void renderManualValves() {
        if (manualValves == null) {
            return;
        }

        updatingValveSwitch = true;
        manualValves.removeAllViews();
        manualValveSafety.setText(
                physicalValveMode
                        ? R.string.manual_valves_physical
                        : R.string.manual_valves_simulation
        );

        for (GardenZone zone : zones) {
            View row = getLayoutInflater().inflate(
                    R.layout.item_manual_valve_switch,
                    manualValves,
                    false
            );
            TextView name = row.findViewById(
                    R.id.txtManualValveName
            );
            TextView detail = row.findViewById(
                    R.id.txtManualValveDetail
            );
            MaterialSwitch valveSwitch = row.findViewById(
                    R.id.switchManualValve
            );

            String emoji = zone.getEmoji() == null
                    ? "🌱"
                    : zone.getEmoji();
            name.setText(emoji + " " + zone.getName());
            detail.setText(
                    zone.getValve_id()
                            + (
                            physicalValveMode
                                    ? " · Fiziksel"
                                    : " · Simülasyon"
                    )
            );
            boolean thisValveOpen =
                    valveOpen
                            && zone.getValve_id()
                            .equals(activeValveId);
            valveSwitch.setChecked(thisValveOpen);
            valveSwitch.setOnCheckedChangeListener(
                    (button, checked) -> {
                        if (updatingValveSwitch) {
                            return;
                        }

                        if (!checked) {
                            if (thisValveOpen) {
                                if (relayOn) {
                                    viewModel.setRelay(false);
                                }
                                viewModel.closeManualValve();
                            }
                            return;
                        }

                        if (valveOpen) {
                            button.setChecked(false);
                            Toast.makeText(
                                    this,
                                    "Önce açık vanayı kapatın.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        if (!isDeviceOnline()) {
                            button.setChecked(false);
                            Toast.makeText(
                                    this,
                                    "Cihaz çevrimdışı.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }
                        viewModel.openManualValve(zone);
                    }
            );
            manualValves.addView(row);
        }
        updatingValveSwitch = false;
    }
}
