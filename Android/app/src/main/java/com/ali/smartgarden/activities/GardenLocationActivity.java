package com.ali.smartgarden.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

/** Saves the garden point and the preferred weather source. */
public class GardenLocationActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private TextInputEditText city, district;
    private MaterialAutoCompleteTextView source;
    private TextView status;
    private MaterialButton gps;
    private Double savedLatitude, savedLongitude;

    private final ActivityResultLauncher<String> permission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) saveGps();
                else status.setText(R.string.runtime_location_permission_denied);
            });

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_garden_location);

        city = findViewById(R.id.inputLocationCity);
        district = findViewById(R.id.inputLocationDistrict);
        source = findViewById(R.id.inputWeatherSource);
        status = findViewById(R.id.txtLocationStatus);
        gps = findViewById(R.id.btnUseGpsLocation);
        source.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new String[]{
                        getString(R.string.runtime_weather_source_auto),
                        getString(R.string.runtime_weather_source_open_meteo),
                        getString(R.string.runtime_weather_source_open_weather)
                }
        ));
        source.setText(getString(R.string.runtime_weather_source_auto), false);

        findViewById(R.id.btnLocationBack).setOnClickListener(view -> finish());
        repository.observeWeatherLocation().observe(this, location -> {
            if (location == null) return;
            city.setText(location.getCity());
            district.setText(location.getDistrict());
            source.setText(sourceText(location.getForecastSource()), false);
            savedLatitude = location.getLatitude();
            savedLongitude = location.getLongitude();
            if (location.getLatitude() != null && location.getLongitude() != null) {
                status.setText(getString(R.string.runtime_location_precise_coordinates,
                        location.getLatitude(), location.getLongitude()));
            }
        });
        findViewById(R.id.btnSaveWeatherSource).setOnClickListener(view -> saveSourcePreference());

        gps.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                saveGps();
            } else {
                permission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        findViewById(R.id.btnSaveManualLocation).setOnClickListener(view -> saveManual());
    }

    private void saveGps() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        try {
            location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {
            // Permission is checked before this method is invoked.
        }
        if (location == null) {
            status.setText(R.string.runtime_location_unavailable);
            return;
        }
        repository.saveWeatherLocation(
                text(city), text(district), location.getLatitude(), location.getLongitude(), sourceValue()
        ).addOnSuccessListener(unused ->
                status.setText(R.string.runtime_location_precise_saved)
        ).addOnFailureListener(error ->
                status.setText(R.string.runtime_location_save_failed)
        );
    }

    private void saveManual() {
        String selectedCity = text(city);
        String selectedDistrict = text(district);
        if (selectedCity.isEmpty() || selectedDistrict.isEmpty()) {
            status.setText(R.string.runtime_location_city_required);
            return;
        }
        repository.saveWeatherLocation(
                selectedCity, selectedDistrict, null, null, sourceValue()
        ).addOnSuccessListener(unused ->
                status.setText(R.string.runtime_location_manual_saved)
        ).addOnFailureListener(error -> status.setText(R.string.runtime_location_save_failed_short));
    }

    private void saveSourcePreference() {
        String selectedCity = text(city);
        String selectedDistrict = text(district);
        if (selectedCity.isEmpty() || selectedDistrict.isEmpty()) {
            status.setText(R.string.runtime_location_capture_first);
            return;
        }
        repository.saveWeatherLocation(
                selectedCity, selectedDistrict, savedLatitude, savedLongitude, sourceValue()
        ).addOnSuccessListener(unused ->
                status.setText(R.string.runtime_weather_source_updated)
        ).addOnFailureListener(error -> status.setText(R.string.runtime_weather_source_save_failed));
    }

    private String sourceValue() {
        String value = source.getText() == null ? "" : source.getText().toString();
        if (value.contains("OpenWeather")) return "openweather";
        if (value.contains("Open-Meteo")) return "open_meteo";
        return "auto";
    }

    private String sourceText(String value) {
        if ("openweather".equals(value)) return getString(R.string.runtime_weather_source_open_weather);
        if ("open_meteo".equals(value)) return getString(R.string.runtime_weather_source_open_meteo);
        return getString(R.string.runtime_weather_source_auto);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
