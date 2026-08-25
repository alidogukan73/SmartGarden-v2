package com.ali.smartgarden.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.calibration.SensorCalibrationSampler;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.zones.ZoneCapacityPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Guided five-sample dry/wet calibration for assigned ADS1115 soil sensors. */
public class SensorCalibrationWizardActivity extends AppCompatActivity {
    private static final long LIVE_READING_MAX_AGE_SECONDS = 30L;
    private static final String STATE_SENSOR_ID = "calibration_sensor_id";
    private static final String STATE_SESSION_ACTIVE = "calibration_session_active";
    private static final String STATE_SESSION_ZONE_ID = "calibration_session_zone_id";
    private static final String STATE_RESTORE_IRRIGATION = "calibration_restore_irrigation";
    private static final String STATE_DRY_RAW = "calibration_dry_raw";
    private static final String STATE_WET_RAW = "calibration_wet_raw";
    private static final String STATE_PHASE = "calibration_phase";
    private static final String STATE_SAMPLES = "calibration_samples";
    private static final String STATE_LAST_SAMPLE_EPOCH = "calibration_last_sample_epoch";

    private final FirebaseRepository repository = new FirebaseRepository();
    private final SensorCalibrationSampler sampler = new SensorCalibrationSampler();
    private final List<GardenZone> sensorZones = new ArrayList<>();

    private MaterialAutoCompleteTextView sensorDropdown;
    private MaterialButton openZoneManagement;
    private MaterialButton captureDry;
    private MaterialButton captureWet;
    private MaterialButton saveCalibration;
    private MaterialButton cancelCalibration;
    private LinearProgressIndicator sampleProgress;
    private LinearProgressIndicator operationProgress;
    private TextView liveStatus;
    private TextView liveRaw;
    private TextView liveVoltage;
    private TextView liveAge;
    private TextView currentCalibration;
    private TextView dryResult;
    private TextView wetResult;
    private TextView sampleStatus;
    private TextView validationStatus;

    private GardenZone selectedZone;
    private String selectedSensorId = "";
    private String sessionZoneId = "";
    private boolean sessionActive;
    private boolean restoreIrrigationEnabled;
    private boolean operationBusy;
    private Integer capturedDryRaw;
    private Integer capturedWetRaw;
    private CapturePhase capturePhase = CapturePhase.NONE;

    private enum CapturePhase {
        NONE,
        DRY,
        WET
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensor_calibration_wizard);
        applyWindowInsets();
        bindViews();
        configureToolbar();
        restoreState(state);
        configureActions();
        configureBackHandling();
        repository.observeGardenZones().observe(this, this::renderZones);
        renderWizardState();
    }

    private void bindViews() {
        sensorDropdown = findViewById(R.id.dropdownCalibrationSensor);
        openZoneManagement = findViewById(R.id.btnCalibrationOpenZoneManagement);
        captureDry = findViewById(R.id.btnCalibrationCaptureDry);
        captureWet = findViewById(R.id.btnCalibrationCaptureWet);
        saveCalibration = findViewById(R.id.btnCalibrationSave);
        cancelCalibration = findViewById(R.id.btnCalibrationCancel);
        sampleProgress = findViewById(R.id.progressCalibrationSamples);
        operationProgress = findViewById(R.id.progressCalibrationOperation);
        liveStatus = findViewById(R.id.txtCalibrationLiveStatus);
        liveRaw = findViewById(R.id.txtCalibrationLiveRaw);
        liveVoltage = findViewById(R.id.txtCalibrationLiveVoltage);
        liveAge = findViewById(R.id.txtCalibrationLiveAge);
        currentCalibration = findViewById(R.id.txtCalibrationCurrentValues);
        dryResult = findViewById(R.id.txtCalibrationDryResult);
        wetResult = findViewById(R.id.txtCalibrationWetResult);
        sampleStatus = findViewById(R.id.txtCalibrationSampleStatus);
        validationStatus = findViewById(R.id.txtCalibrationValidation);
    }

    private void configureToolbar() {
        ((TextView) findViewById(R.id.txtSettingsToolbarTitle))
                .setText(R.string.sensor_calibration_title);
        findViewById(R.id.btnSettingsToolbarAction).setVisibility(View.GONE);
        findViewById(R.id.btnSettingsToolbarBack)
                .setOnClickListener(view -> requestClose());
    }

    private void configureActions() {
        sensorDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (sessionActive || position < 0 || position >= sensorZones.size()) {
                return;
            }
            selectZone(sensorZones.get(position));
        });
        openZoneManagement.setOnClickListener(view ->
                startActivity(new Intent(this, ZoneManagementActivity.class)));
        captureDry.setOnClickListener(view -> requestCapture(CapturePhase.DRY));
        captureWet.setOnClickListener(view -> requestCapture(CapturePhase.WET));
        saveCalibration.setOnClickListener(view -> saveCalibration());
        cancelCalibration.setOnClickListener(view -> requestClose());
    }

    private void configureBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestClose();
            }
        });
    }

    private void renderZones(List<GardenZone> zones) {
        String targetSensorId = selectedSensorId;
        sensorZones.clear();
        for (GardenZone zone : ZoneCapacityPolicy.activeZones(zones)) {
            String sensorId = safe(zone.getSensor_id()).toLowerCase(Locale.US);
            if (!sensorId.isEmpty() && ZoneCapacityPolicy.isValidSensorId(sensorId)) {
                sensorZones.add(zone);
            }
        }
        sensorZones.sort(Comparator.comparing(zone ->
                safe(zone.getSensor_id()).toLowerCase(Locale.US)));

        List<String> labels = new ArrayList<>();
        for (GardenZone zone : sensorZones) {
            labels.add(zoneLabel(zone));
        }
        sensorDropdown.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                labels
        ));

        selectedZone = findBySensorId(targetSensorId);
        if (selectedZone == null && !sessionActive && !sensorZones.isEmpty()) {
            selectedZone = sensorZones.get(0);
            selectedSensorId = safe(selectedZone.getSensor_id());
        }
        if (selectedZone != null) {
            sensorDropdown.setText(zoneLabel(selectedZone), false);
        } else if (sensorZones.isEmpty()) {
            sensorDropdown.setText("", false);
        }

        openZoneManagement.setVisibility(
                sensorZones.isEmpty() ? View.VISIBLE : View.GONE
        );
        renderLiveReading();
        collectCurrentSample();
        renderWizardState();
    }

    private void selectZone(GardenZone zone) {
        selectedZone = zone;
        selectedSensorId = safe(zone.getSensor_id());
        capturedDryRaw = null;
        capturedWetRaw = null;
        capturePhase = CapturePhase.NONE;
        sampler.reset();
        sampleStatus.setText(R.string.sensor_calibration_sample_idle);
        renderLiveReading();
        renderWizardState();
    }

    private GardenZone findBySensorId(String sensorId) {
        for (GardenZone zone : sensorZones) {
            if (safe(zone.getSensor_id()).equalsIgnoreCase(safe(sensorId))) {
                return zone;
            }
        }
        return null;
    }

    private void renderLiveReading() {
        if (selectedZone == null) {
            liveStatus.setText(sensorZones.isEmpty()
                    ? R.string.sensor_calibration_no_assigned_sensor
                    : R.string.sensor_calibration_select_sensor);
            liveStatus.setTextColor(getColor(R.color.warning));
            liveRaw.setText(R.string.sensor_calibration_value_empty);
            liveVoltage.setText(R.string.sensor_calibration_value_empty);
            liveAge.setText(R.string.sensor_calibration_value_empty);
            currentCalibration.setText(R.string.sensor_calibration_current_empty);
            return;
        }

        long age = readingAgeSeconds(selectedZone);
        boolean fresh = isReadingFresh(selectedZone);
        if (!selectedZone.isSensor_enabled()) {
            liveStatus.setText(R.string.sensor_calibration_sensor_disabled);
            liveStatus.setTextColor(getColor(R.color.warning));
        } else if (age < 0L) {
            liveStatus.setText(R.string.sensor_calibration_waiting_data);
            liveStatus.setTextColor(getColor(R.color.warning));
        } else if (!fresh) {
            liveStatus.setText(R.string.sensor_calibration_data_old);
            liveStatus.setTextColor(getColor(R.color.offline));
        } else {
            liveStatus.setText(R.string.sensor_calibration_live_connected);
            liveStatus.setTextColor(getColor(R.color.online));
        }

        liveRaw.setText(String.valueOf(selectedZone.getRaw()));
        liveVoltage.setText(getString(
                R.string.sensor_calibration_voltage_format,
                selectedZone.getVoltage()
        ));
        liveAge.setText(age < 0L
                ? getString(R.string.sensor_calibration_value_empty)
                : getString(R.string.sensor_calibration_age_format, age));
        currentCalibration.setText(getString(
                R.string.sensor_calibration_current_format,
                selectedZone.getSensor_calibration_dry_raw(),
                selectedZone.getSensor_calibration_wet_raw()
        ));
    }

    private void requestCapture(CapturePhase phase) {
        if (operationBusy) {
            return;
        }
        if (selectedZone == null) {
            Toast.makeText(this, R.string.sensor_calibration_select_sensor,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!selectedZone.isSensor_enabled()) {
            Toast.makeText(this, R.string.sensor_calibration_sensor_disabled,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!isReadingFresh(selectedZone)) {
            Toast.makeText(this, R.string.sensor_calibration_waiting_fresh,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (phase == CapturePhase.WET && capturedDryRaw == null) {
            Toast.makeText(this, R.string.sensor_calibration_dry_first,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (sessionActive) {
            startCapture(phase);
            return;
        }
        beginSafeCalibrationSession(phase);
    }

    private void beginSafeCalibrationSession(CapturePhase firstPhase) {
        ZoneIrrigationStatus status = selectedZone.getIrrigation_status();
        if (status != null
                && (status.isWatering_active() || status.getQueue_position() > 0)) {
            Toast.makeText(this, R.string.sensor_calibration_irrigation_busy,
                    Toast.LENGTH_LONG).show();
            return;
        }

        String zoneId = safe(selectedZone.getZone_id());
        if (!ZoneCapacityPolicy.isValidZoneId(zoneId)) {
            Toast.makeText(this, R.string.sensor_calibration_zone_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean wasEnabled = selectedZone.isIrrigation_enabled();
        setOperationBusy(true);
        Task<Void> safetyTask = wasEnabled
                ? repository.setZoneIrrigationEnabledForCalibration(zoneId, false)
                : Tasks.forResult(null);
        safetyTask.addOnSuccessListener(unused -> {
            sessionActive = true;
            sessionZoneId = zoneId;
            restoreIrrigationEnabled = wasEnabled;
            setOperationBusy(false);
            if (wasEnabled) {
                Toast.makeText(this,
                        R.string.sensor_calibration_irrigation_paused,
                        Toast.LENGTH_LONG).show();
            }
            startCapture(firstPhase);
        }).addOnFailureListener(error -> {
            setOperationBusy(false);
            Toast.makeText(this, R.string.sensor_calibration_safety_failed,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void startCapture(CapturePhase phase) {
        capturePhase = phase;
        sampler.reset();
        if (phase == CapturePhase.DRY) {
            capturedDryRaw = null;
            capturedWetRaw = null;
        } else {
            capturedWetRaw = null;
        }
        sampleProgress.setProgressCompat(0, false);
        sampleStatus.setText(phase == CapturePhase.DRY
                ? R.string.sensor_calibration_collecting_dry
                : R.string.sensor_calibration_collecting_wet);
        renderWizardState();
        collectCurrentSample();
    }

    private void collectCurrentSample() {
        if (capturePhase == CapturePhase.NONE
                || selectedZone == null
                || !isReadingFresh(selectedZone)
                || !safe(selectedZone.getZone_id()).equals(sessionZoneId)) {
            return;
        }

        boolean added = sampler.addSample(
                selectedZone.getRaw(),
                selectedZone.getUpdated_at_epoch()
        );
        if (!added) {
            return;
        }

        sampleProgress.setProgressCompat(sampler.getCount(), true);
        sampleStatus.setText(getString(
                R.string.sensor_calibration_sample_progress,
                sampler.getCount(),
                SensorCalibrationSampler.REQUIRED_SAMPLES,
                selectedZone.getRaw()
        ));

        if (!sampler.isComplete()) {
            return;
        }

        CapturePhase completedPhase = capturePhase;
        capturePhase = CapturePhase.NONE;
        if (!sampler.isStable()) {
            if (completedPhase == CapturePhase.DRY) {
                capturedDryRaw = null;
            } else {
                capturedWetRaw = null;
            }
            sampleStatus.setText(getString(
                    R.string.sensor_calibration_unstable,
                    sampler.spread()
            ));
            renderWizardState();
            return;
        }

        int median = sampler.median();
        if (completedPhase == CapturePhase.DRY) {
            capturedDryRaw = median;
        } else {
            capturedWetRaw = median;
        }
        sampleStatus.setText(getString(
                R.string.sensor_calibration_capture_complete,
                median,
                sampler.spread()
        ));
        renderWizardState();
    }

    private void renderWizardState() {
        boolean liveReady = selectedZone != null
                && selectedZone.isSensor_enabled()
                && isReadingFresh(selectedZone);
        boolean capturing = capturePhase != CapturePhase.NONE;

        sensorDropdown.setEnabled(!sessionActive && !operationBusy);
        captureDry.setEnabled(liveReady && !capturing && !operationBusy);
        captureWet.setEnabled(liveReady && sessionActive
                && capturedDryRaw != null && !capturing && !operationBusy);
        saveCalibration.setEnabled(sessionActive
                && capturedDryRaw != null
                && capturedWetRaw != null
                && SensorCalibrationSampler.isValidCalibration(
                        capturedDryRaw,
                        capturedWetRaw)
                && !capturing
                && !operationBusy);
        cancelCalibration.setVisibility(
                sessionActive ? View.VISIBLE : View.GONE
        );
        cancelCalibration.setEnabled(!operationBusy);

        dryResult.setText(capturedDryRaw == null
                ? getString(R.string.sensor_calibration_not_measured)
                : getString(R.string.sensor_calibration_result_format,
                        capturedDryRaw));
        wetResult.setText(capturedWetRaw == null
                ? getString(R.string.sensor_calibration_not_measured)
                : getString(R.string.sensor_calibration_result_format,
                        capturedWetRaw));

        if (capturedDryRaw == null || capturedWetRaw == null) {
            validationStatus.setText(R.string.sensor_calibration_validation_waiting);
            validationStatus.setTextColor(getColor(R.color.textSecondary));
        } else if (SensorCalibrationSampler.isValidCalibration(
                capturedDryRaw, capturedWetRaw)) {
            validationStatus.setText(getString(
                    R.string.sensor_calibration_validation_ready,
                    capturedDryRaw - capturedWetRaw
            ));
            validationStatus.setTextColor(getColor(R.color.online));
        } else {
            validationStatus.setText(R.string.sensor_calibration_validation_invalid);
            validationStatus.setTextColor(getColor(R.color.offline));
        }
    }

    private void saveCalibration() {
        if (capturedDryRaw == null || capturedWetRaw == null
                || !SensorCalibrationSampler.isValidCalibration(
                        capturedDryRaw, capturedWetRaw)) {
            Toast.makeText(this, R.string.sensor_calibration_validation_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }

        setOperationBusy(true);
        repository.completeSensorCalibration(
                sessionZoneId,
                capturedDryRaw,
                capturedWetRaw,
                restoreIrrigationEnabled
        ).addOnSuccessListener(unused -> {
            sessionActive = false;
            setOperationBusy(false);
            Toast.makeText(this, R.string.sensor_calibration_saved,
                    Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(error -> {
            setOperationBusy(false);
            Toast.makeText(this, R.string.sensor_calibration_save_failed,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void requestClose() {
        if (!sessionActive) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sensor_calibration_cancel_title)
                .setMessage(R.string.sensor_calibration_cancel_message)
                .setNegativeButton(R.string.sensor_calibration_continue, null)
                .setPositiveButton(R.string.sensor_calibration_cancel_confirm,
                        (dialog, which) -> cancelSessionAndFinish())
                .show();
    }

    private void cancelSessionAndFinish() {
        setOperationBusy(true);
        Task<Void> restoreTask = restoreIrrigationEnabled
                ? repository.setZoneIrrigationEnabledForCalibration(
                        sessionZoneId, true)
                : Tasks.forResult(null);
        restoreTask.addOnSuccessListener(unused -> {
            sessionActive = false;
            setOperationBusy(false);
            finish();
        }).addOnFailureListener(error -> {
            setOperationBusy(false);
            Toast.makeText(this, R.string.sensor_calibration_restore_failed,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setOperationBusy(boolean busy) {
        operationBusy = busy;
        operationProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        renderWizardState();
    }

    private boolean isReadingFresh(GardenZone zone) {
        long age = readingAgeSeconds(zone);
        return age >= 0L && age <= LIVE_READING_MAX_AGE_SECONDS;
    }

    private long readingAgeSeconds(GardenZone zone) {
        if (zone == null || zone.getUpdated_at_epoch() <= 0L) {
            return -1L;
        }
        return Math.max(0L,
                System.currentTimeMillis() / 1000L - zone.getUpdated_at_epoch());
    }

    private String zoneLabel(GardenZone zone) {
        String emoji = safe(zone.getEmoji());
        String name = safe(zone.getName());
        return (emoji.isEmpty() ? getString(R.string.symbol_plant) : emoji)
                + " " + name + " · " + safe(zone.getSensor_id());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.calibrationWizardRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

    private void restoreState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }
        selectedSensorId = safe(state.getString(STATE_SENSOR_ID));
        sessionActive = state.getBoolean(STATE_SESSION_ACTIVE, false);
        sessionZoneId = safe(state.getString(STATE_SESSION_ZONE_ID));
        restoreIrrigationEnabled =
                state.getBoolean(STATE_RESTORE_IRRIGATION, false);
        int dry = state.getInt(STATE_DRY_RAW, -1);
        int wet = state.getInt(STATE_WET_RAW, -1);
        capturedDryRaw = dry >= 0 ? dry : null;
        capturedWetRaw = wet >= 0 ? wet : null;
        try {
            capturePhase = CapturePhase.valueOf(
                    state.getString(STATE_PHASE, CapturePhase.NONE.name()));
        } catch (IllegalArgumentException error) {
            capturePhase = CapturePhase.NONE;
        }
        sampler.restore(
                state.getIntegerArrayList(STATE_SAMPLES),
                state.getLong(STATE_LAST_SAMPLE_EPOCH, -1L)
        );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SENSOR_ID, selectedSensorId);
        outState.putBoolean(STATE_SESSION_ACTIVE, sessionActive);
        outState.putString(STATE_SESSION_ZONE_ID, sessionZoneId);
        outState.putBoolean(STATE_RESTORE_IRRIGATION,
                restoreIrrigationEnabled);
        outState.putInt(STATE_DRY_RAW,
                capturedDryRaw == null ? -1 : capturedDryRaw);
        outState.putInt(STATE_WET_RAW,
                capturedWetRaw == null ? -1 : capturedWetRaw);
        outState.putString(STATE_PHASE, capturePhase.name());
        outState.putIntegerArrayList(
                STATE_SAMPLES,
                new ArrayList<>(sampler.snapshot())
        );
        outState.putLong(
                STATE_LAST_SAMPLE_EPOCH,
                sampler.getLastSampleEpoch()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
