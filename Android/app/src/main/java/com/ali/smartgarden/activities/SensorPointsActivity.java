package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.models.SoilSensor;
import com.ali.smartgarden.viewmodels.SensorPointsViewModel;
import com.ali.smartgarden.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.google.android.material.button.MaterialButton;

public class SensorPointsActivity extends AppCompatActivity {

    private static final long SENSOR_CONNECTED_SECONDS = 30L;
    private static final long SENSOR_WEAK_SECONDS = 90L;

    private static final long SENSOR_STATUS_REFRESH_MILLIS = 5_000L;


    private final Handler sensorStatusHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private SoilSensor latestTomatoSensor;


    private SensorPointsViewModel viewModel;

    private MaterialButton btnBack;

    private TextView txtSensorSummary;

    private TextView txtTomatoStatus;
    private TextView txtTomatoSensorId;
    private TextView txtTomatoMoisture;
    private TextView txtTomatoRssi;
    private TextView txtTomatoLastUpdate;

    private TextView txtPepperStatus;
    private TextView txtPepperSensorId;

    private TextView txtCucumberStatus;
    private TextView txtCucumberSensorId;

    private final Runnable sensorStatusUpdater =
            new Runnable() {

                @Override
                public void run() {

                    refreshTomatoSensorStatus();

                    sensorStatusHandler.postDelayed(
                            this,
                            SENSOR_STATUS_REFRESH_MILLIS
                    );
                }
            };

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_sensor_points
        );

        initializeViews();
        initializeActions();
        initializeViewModel();
        observeViewModel();
    }

    private void initializeViews() {

        btnBack = findViewById(
                R.id.btnBack
        );

        txtSensorSummary = findViewById(
                R.id.txtSensorSummary
        );

        txtTomatoStatus = findViewById(
                R.id.txtTomatoStatus
        );

        txtTomatoSensorId = findViewById(
                R.id.txtTomatoSensorId
        );

        txtTomatoMoisture = findViewById(
                R.id.txtTomatoMoisture
        );

        txtTomatoRssi = findViewById(
                R.id.txtTomatoRssi
        );

        txtTomatoLastUpdate = findViewById(
                R.id.txtTomatoLastUpdate
        );

        txtPepperStatus = findViewById(
                R.id.txtPepperStatus
        );

        txtPepperSensorId = findViewById(
                R.id.txtPepperSensorId
        );

        txtCucumberStatus = findViewById(
                R.id.txtCucumberStatus
        );

        txtCucumberSensorId = findViewById(
                R.id.txtCucumberSensorId
        );
    }

    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );
    }

    private void initializeViewModel() {

        viewModel =
                new ViewModelProvider(this)
                        .get(
                                SensorPointsViewModel.class
                        );
    }

    private void observeViewModel() {

        viewModel.getSoilSensor().observe(
                this,
                this::renderTomatoSensor
        );
    }

    private void renderTomatoSensor(
            SoilSensor sensor
    ) {

        latestTomatoSensor = sensor;

        if (sensor == null) {

            txtSensorSummary.setText(
                    "0 / 3 sensör bağlı"
            );

            txtTomatoStatus.setText(
                    "Bağlantı yok"
            );

            txtTomatoStatus.setTextColor(
                    getColor(
                            R.color.offline
                    )
            );

            txtTomatoSensorId.setText(
                    "soil-001"
            );

            txtTomatoMoisture.setText(
                    "-"
            );

            txtTomatoRssi.setText(
                    "-"
            );

            txtTomatoLastUpdate.setText(
                    "Son veri: Bekleniyor"
            );

            return;
        }

        txtSensorSummary.setText(
                "1 / 3 sensör bağlı"
        );

        updateTomatoConnectionStatus(
                sensor
        );

        String sensorId =
                sensor.getSensor_id();

        if (
                sensorId == null
                        || sensorId.isBlank()
        ) {

            sensorId = "soil-001";
        }

        txtTomatoSensorId.setText(
                sensorId
        );

        txtTomatoMoisture.setText(
                "%" + sensor.getMoisture()
        );

        txtTomatoRssi.setText(
                sensor.getRssi() + " dBm"
        );

        txtTomatoLastUpdate.setText(
                getLastUpdateText(
                        sensor
                )
        );
    }

    private void refreshTomatoSensorStatus() {

        if (latestTomatoSensor == null) {

            txtSensorSummary.setText(
                    "0 / 3 sensör bağlı"
            );

            txtTomatoStatus.setText(
                    "Bağlantı yok"
            );

            txtTomatoStatus.setTextColor(
                    getColor(
                            R.color.offline
                    )
            );

            txtTomatoLastUpdate.setText(
                    "Son veri: Bekleniyor"
            );

            return;
        }

        String updatedAt =
                latestTomatoSensor.getUpdated_at();

        updateTomatoConnectionStatus(
                latestTomatoSensor
        );

        txtTomatoLastUpdate.setText(
                getLastUpdateText(
                        latestTomatoSensor
                )
        );

        updateSensorSummary();
    }

    private void updateSensorSummary() {

        if (latestTomatoSensor == null) {

            txtSensorSummary.setText(
                    "0 / 3 sensör bağlı"
            );

            return;
        }

        long ageSeconds =
                getSensorAgeSeconds(
                        latestTomatoSensor
                );

        boolean connected =
                ageSeconds >= 0
                        && ageSeconds
                        <= SENSOR_WEAK_SECONDS;

        txtSensorSummary.setText(
                connected
                        ? "1 / 3 sensör bağlı"
                        : "0 / 3 sensör bağlı"
        );
    }

    private void updateTomatoConnectionStatus(
            SoilSensor sensor
    ) {

        long ageSeconds =
                getSensorAgeSeconds(
                        sensor
                );

        if (ageSeconds < 0) {

            txtTomatoStatus.setText(
                    "Bağlantı yok"
            );

            txtTomatoStatus.setTextColor(
                    getColor(
                            R.color.offline
                    )
            );

            return;
        }

        if (
                ageSeconds
                        <= SENSOR_CONNECTED_SECONDS
        ) {

            txtTomatoStatus.setText(
                    "Bağlı"
            );

            txtTomatoStatus.setTextColor(
                    getColor(
                            R.color.online
                    )
            );

            return;
        }

        if (
                ageSeconds
                        <= SENSOR_WEAK_SECONDS
        ) {

            txtTomatoStatus.setText(
                    "Bağlantı zayıf"
            );

            txtTomatoStatus.setTextColor(
                    getColor(
                            R.color.warning
                    )
            );

            return;
        }

        txtTomatoStatus.setText(
                "Bağlantı yok"
        );

        txtTomatoStatus.setTextColor(
                getColor(
                        R.color.offline
                )
        );
    }

    private long getSensorAgeSeconds(
            SoilSensor sensor
    ) {

        if (sensor == null) {
            return -1L;
        }

        long updatedAtEpoch =
                sensor.getUpdated_at_epoch();

        if (updatedAtEpoch <= 0L) {
            return -1L;
        }

        long currentEpochSeconds =
                System.currentTimeMillis()
                        / 1000L;

        long ageSeconds =
                currentEpochSeconds
                        - updatedAtEpoch;

        return Math.max(
                ageSeconds,
                0L
        );
    }

    private String getLastUpdateText(
            SoilSensor sensor
    ) {

        long seconds =
                getSensorAgeSeconds(
                        sensor
                );

        if (seconds < 0) {

            return "Son veri: Bilinmiyor";
        }

        if (seconds < 60) {

            return "Son veri: Az önce";
        }

        long minutes =
                seconds / 60;

        if (minutes < 60) {

            return "Son veri: "
                    + minutes
                    + " dk önce";
        }

        long hours =
                minutes / 60;

        if (hours < 24) {

            return "Son veri: "
                    + hours
                    + " saat önce";
        }

        long days =
                hours / 24;

        return "Son veri: "
                + days
                + " gün önce";
    }

    @Override
    protected void onStart() {

        super.onStart();

        sensorStatusHandler.removeCallbacks(
                sensorStatusUpdater
        );

        sensorStatusHandler.post(
                sensorStatusUpdater
        );
    }

    @Override
    protected void onStop() {

        sensorStatusHandler.removeCallbacks(
                sensorStatusUpdater
        );

        super.onStop();
    }

}
