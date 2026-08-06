package com.ali.smartgarden.activities;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.fertilization.FertilizerReminderScheduler;
import com.ali.smartgarden.adapters.HomeZonePagerAdapter;
import com.ali.smartgarden.models.Command;
import com.ali.smartgarden.models.Sensor;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.models.AIExplanation;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.health.GardenHealthCalculator;
import com.ali.smartgarden.health.GardenHealthSummary;
import com.ali.smartgarden.plantdoctor.PlantDoctorRecommendationStore;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.ali.smartgarden.ui.MainMenuBottomSheet;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Handler;
import android.os.Looper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    private long lastStatusReceivedElapsedMillis = 0L;
    private long connectionStartedElapsedMillis;

    // Header
    private MaterialCardView cardOnlineStatus;
    private TextView txtOnline;
    private TextView txtDevice;

    // Sensor
    private TextView txtMoisture;
    private TextView txtMoistureStatus;
    private TextView txtVoltage;
    private TextView txtRaw;
    private ProgressBar progressMoisture;

    // Pump
    private MaterialCardView cardPumpStatus;
    private TextView txtRelay;
    private TextView txtPumpDescription;

    // Automatic mode
    private MaterialCardView cardAutoMode;
    private MaterialSwitch switchAuto;
    private TextView txtAutoDescription;

    // Manual control
    private MaterialButton btnWater;
    private TextView txtManualHint;

    private AppCompatImageView imgPumpStatus;

    private MaterialButton btnMainMenu;
    private TextView txtGardenSensors;
    private TextView txtGardenWateringSummary;
    private TextView txtGardenPumpSummary;
    private MaterialCardView cardGardenSummary;
    private MaterialCardView cardHomePlantDoctorSummary;
    private TextView txtHomePlantDoctorSummary;
    private MaterialCardView cardHomeWateringSummary;
    private MaterialCardView cardHomeFertilizationSummary;
    private TextView txtHomeWateringSummary;
    private MaterialCardView cardHomeHealth;
    private TextView txtHomeFertilizationSummary;
    private TextView txtHomeHealthTitle;
    private TextView txtHomeHealthDetail;
    private TextView txtHomeHealthScore;
    private CircularProgressIndicator progressHomeHealthScore;
    private MaterialCardView cardHomeWeather;
    private TextView txtHomeWeatherIcon;
    private TextView txtHomeWeatherTitle;
    private TextView txtHomeWeatherToday;
    private TextView txtHomeWeatherTomorrow;
    private MaterialCardView cardSimulationWarning;
    private MaterialCardView cardHomeAlerts;
    private LinearLayout layoutHomeAlerts;
    private RecyclerView recyclerHomeZones;
    private LinearLayout layoutHomeZoneDots;
    private HomeZonePagerAdapter homeZonePagerAdapter;
    private LinearLayout layoutHomeZones;
    private boolean homeZonePagerPositioned = false;
    private List<GardenZone> latestZones;
    private MaterialCardView cardHomeAi;
    private TextView txtHomeAiDecision;
    private TextView txtHomeAiSummary;
    private TextView txtHomeAiProgress;
    private ProgressBar progressHomeAi;
    private boolean valveSimulationMode = true;
    private TextView txtPrimaryZoneTitle;
    private TextView txtPrimaryZoneSubtitle;
    private MaterialButton btnHomeZones;
    private MaterialButton btnHomeAi;
    private MaterialButton btnHomeSettings;
    private MaterialButton btnToggleAdvanced;
    private MaterialCardView cardPrimaryZoneDetails;
    private MaterialCardView cardWateringControl;
    private boolean advancedControlsVisible = false;

    private boolean updatingAutoSwitch = false;
    private boolean relayOn = false;

    private static final long ONLINE_TIMEOUT_MILLIS = 30_000L;
    private static final long ONLINE_CHECK_INTERVAL_MILLIS = 5_000L;

    private enum ConnectionState {
        CONNECTING,
        ONLINE,
        OFFLINE
    }

    private final Handler onlineStatusHandler =
            new Handler(Looper.getMainLooper());

    private Status latestStatus;

    private final Runnable onlineStatusChecker =
            new Runnable() {

                @Override
                public void run() {

                    renderEffectiveOnlineStatus();

                    onlineStatusHandler.postDelayed(
                            this,
                            ONLINE_CHECK_INTERVAL_MILLIS
                    );
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        authenticateThenInitialize();
    }

    /**
     * Realtime Database rules require an authenticated Firebase user.  The
     * garden has a single trusted mobile app, so it obtains a persisted
     * anonymous Firebase session before attaching any database listeners.
     */
    private void authenticateThenInitialize() {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            initializeAuthenticatedApp();
            return;
        }

        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                initializeAuthenticatedApp();
                return;
            }

            Log.e(
                    "MainActivity",
                    "Firebase anonymous sign-in failed",
                    task.getException()
            );
            Toast.makeText(
                    this,
                    "Güvenli Firebase bağlantısı kurulamadı. İnternet bağlantısını kontrol edin.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void initializeAuthenticatedApp() {

        connectionStartedElapsedMillis = SystemClock.elapsedRealtime();
        moveZoneCarouselAboveGardenSummary();
        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeButtons();
        FertilizerReminderScheduler.schedule(this);
    }

    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void moveZoneCarouselAboveGardenSummary() {
        View zoneSection = findViewById(
                R.id.layoutHomeZoneCarouselSection
        );
        View gardenSummary = findViewById(
                R.id.cardGardenSummary
        );
        if (
                zoneSection == null
                        || gardenSummary == null
                        || zoneSection.getParent()
                        != gardenSummary.getParent()
        ) {
            return;
        }

        ViewGroup parent =
                (ViewGroup) zoneSection.getParent();
        int targetIndex =
                parent.indexOfChild(gardenSummary);
        parent.removeView(zoneSection);
        parent.addView(zoneSection, targetIndex);
    }

    private void initializeViews() {

        imgPumpStatus = findViewById(R.id.imgPumpStatus);

        btnMainMenu = findViewById(
                R.id.btnMainMenu
        );
        txtGardenSensors = findViewById(
                R.id.txtGardenSensors
        );
        cardGardenSummary = findViewById(
                R.id.cardGardenSummary
        );
        txtGardenWateringSummary = findViewById(
                R.id.txtGardenWateringSummary
        );
        txtGardenPumpSummary = findViewById(
                R.id.txtGardenPumpSummary
        );
        cardHomePlantDoctorSummary = findViewById(
                R.id.cardHomePlantDoctorSummary
        );
        txtHomePlantDoctorSummary = findViewById(
                R.id.txtHomePlantDoctorSummary
        );
        renderHomePlantDoctorRecommendation();
        cardHomeWateringSummary = findViewById(R.id.cardHomeWateringSummary);
        txtHomeWateringSummary = findViewById(R.id.txtHomeWateringSummary);
        cardHomeFertilizationSummary = findViewById(
                R.id.cardHomeFertilizationSummary
        );
        txtHomeFertilizationSummary = findViewById(
                R.id.txtHomeFertilizationSummary
        );
        cardHomeHealth = findViewById(R.id.cardHomeHealth);
        txtHomeHealthTitle = findViewById(R.id.txtHomeHealthTitle);
        txtHomeHealthDetail = findViewById(R.id.txtHomeHealthDetail);
        txtHomeHealthScore = findViewById(R.id.txtHomeHealthScore);
        progressHomeHealthScore = findViewById(R.id.progressHomeHealthScore);
        cardHomeWeather = findViewById(R.id.cardHomeWeather);
        txtHomeWeatherIcon = findViewById(R.id.txtHomeWeatherIcon);
        txtHomeWeatherTitle = findViewById(R.id.txtHomeWeatherTitle);
        txtHomeWeatherToday = findViewById(R.id.txtHomeWeatherToday);
        txtHomeWeatherTomorrow = findViewById(R.id.txtHomeWeatherTomorrow);
        moveWeatherBelowFertilization();
        cardSimulationWarning = findViewById(
                R.id.cardSimulationWarning
        );
        cardHomeAlerts = findViewById(
                R.id.cardHomeAlerts
        );
        layoutHomeAlerts = findViewById(
                R.id.layoutHomeAlerts
        );
        recyclerHomeZones = findViewById(
                R.id.recyclerHomeZones
        );
        layoutHomeZoneDots = findViewById(
                R.id.layoutHomeZoneDots
        );
        layoutHomeZones = findViewById(
                R.id.layoutHomeZones
        );
        initializeHomeZonePager();
        cardHomeAi = findViewById(R.id.cardHomeAi);
        txtHomeAiDecision = findViewById(
                R.id.txtHomeAiDecision
        );
        txtHomeAiSummary = findViewById(
                R.id.txtHomeAiSummary
        );
        txtHomeAiProgress = findViewById(
                R.id.txtHomeAiProgress
        );
        progressHomeAi = findViewById(
                R.id.progressHomeAi
        );
        txtPrimaryZoneTitle = findViewById(
                R.id.txtPrimaryZoneTitle
        );
        txtPrimaryZoneSubtitle = findViewById(
                R.id.txtPrimaryZoneSubtitle
        );
        btnHomeZones = findViewById(R.id.btnHomeZones);
        btnHomeAi = findViewById(R.id.btnHomeAi);
        btnHomeSettings = findViewById(
                R.id.btnHomeSettings
        );
        btnToggleAdvanced = findViewById(
                R.id.btnToggleAdvanced
        );
        cardPrimaryZoneDetails = findViewById(
                R.id.cardPrimaryZoneDetails
        );
        cardWateringControl = findViewById(
                R.id.cardWateringControl
        );

        cardOnlineStatus = findViewById(R.id.cardOnlineStatus);
        txtOnline = findViewById(R.id.txtOnline);
        txtDevice = findViewById(R.id.txtDevice);

        txtMoisture = findViewById(R.id.txtMoisture);
        txtMoistureStatus = findViewById(R.id.txtMoistureStatus);
        txtVoltage = findViewById(R.id.txtVoltage);
        txtRaw = findViewById(R.id.txtRaw);
        progressMoisture = findViewById(R.id.progressMoisture);

        cardPumpStatus = findViewById(R.id.cardPumpStatus);
        txtRelay = findViewById(R.id.txtRelay);
        txtPumpDescription = findViewById(R.id.txtPumpDescription);

        cardAutoMode = findViewById(R.id.cardAutoMode);
        switchAuto = findViewById(R.id.switchAuto);
        txtAutoDescription = findViewById(R.id.txtAutoDescription);

        btnWater = findViewById(R.id.btnWater);
        txtManualHint = findViewById(R.id.txtManualHint);

        txtDevice.setText(
                R.string.default_device_name
        );
    }

    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);
    }

    private void observeViewModel() {

        viewModel.getSensor().observe(
                this,
                this::renderSensor
        );

        viewModel.getStatus().observe(
                this,
                this::renderStatus
        );

        viewModel.getCommand().observe(
                this,
                this::renderCommand
        );

        viewModel.getGardenZones().observe(
                this,
                this::renderGardenZones
        );

        viewModel.getAIExplanation().observe(
                this,
                this::renderHomeAi
        );

        viewModel.getWeatherForecast().observe(this, this::renderHomeWeather);

        viewModel.getError().observe(
                this,
                message -> {

                    if (
                            message == null
                                    || message.isBlank()
                    ) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void renderHomeAi(AIExplanation explanation) {
        if (explanation == null) {
            return;
        }

        String title = explanation.getTitle();
        String summary = explanation.getSummary();
        int progress = (int) Math.max(
                0L,
                Math.min(100L, explanation.getProgressPercent())
        );

        if (title != null && !title.isBlank()) {
            txtHomeAiDecision.setText(title);
        }
        if (summary != null && !summary.isBlank()) {
            txtHomeAiSummary.setText(summary);
        }
        txtHomeAiProgress.setText(
                getString(
                        R.string.home_ai_progress_format,
                        progress
                )
        );
        progressHomeAi.setProgress(progress);
    }

    private void moveWeatherBelowFertilization() {
        if (cardHomeWeather == null || cardHomeFertilizationSummary == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) cardHomeWeather.getParent();
        if (parent == null || parent != cardHomeFertilizationSummary.getParent()) {
            return;
        }
        int targetIndex = parent.indexOfChild(cardHomeFertilizationSummary) + 1;
        parent.removeView(cardHomeWeather);
        parent.addView(cardHomeWeather, targetIndex);
    }

    private void renderHomeWeather(WeatherForecast forecast) {
        if (forecast == null || forecast.getTomorrowTemperatureMax() == null) {
            cardHomeWeather.setVisibility(View.GONE);
            return;
        }
        cardHomeWeather.setVisibility(View.VISIBLE);
        String location = forecast.getDistrict().isBlank()
                ? forecast.getCity()
                : forecast.getDistrict() + " / " + forecast.getCity();
        txtHomeWeatherTitle.setText("Hava durumu · " + location);
        txtHomeWeatherIcon.setText("☀");
        bindWeatherDay(txtHomeWeatherToday, "Bugün", forecast.getTodayTemperatureMax(),
                forecast.getTodayRainProbability(), forecast.getTodayWindMax(), forecast.getTodayWeatherCode());
        bindWeatherDay(txtHomeWeatherTomorrow, "Yarın", forecast.getTomorrowTemperatureMax(),
                forecast.getTomorrowRainProbability(), forecast.getTomorrowWindMax(), forecast.getTomorrowWeatherCode());
    }

    private String weatherCompact(String day, Double temperature, Double rain, Double wind) {
        String tempText = temperature == null ? "—" : Math.round(temperature) + "°C";
        String rainText = rain == null ? "" : " · %" + Math.round(rain) + " yağış";
        String windText = wind == null ? "" : "\n↝ " + Math.round(wind) + " km/sa";
        return day + "\n" + tempText + rainText + windText;
    }

    private void bindWeatherDay(TextView view, String day, Double temperature, Double rain,
                                Double wind, Long code) {
        view.setText(weatherCompact(day, temperature, rain, wind));
        view.setCompoundDrawablesWithIntrinsicBounds(weatherIconResource(code), 0, 0, 0);
        view.setCompoundDrawablePadding(6);
    }

    private int weatherIconResource(Long code) {
        if (code != null && code >= 51) return R.drawable.ic_water_drop_24;
        if (code != null && code >= 2) return R.drawable.ic_weather_cloud_24;
        return R.drawable.ic_weather_sunny_24;
    }

    private String weatherIcon(Long code) {
        if (code == null) return "☀";
        if (code >= 95) return "⛈";
        if (code >= 51) return "🌧";
        if (code >= 45) return "🌫";
        if (code >= 2) return "☁";
        return "☀";
    }

    private void renderSensor(Sensor sensor) {

        if (sensor == null) {
            return;
        }

        long moisture = Math.max(
                0,
                Math.min(
                        100,
                        sensor.getMoisture()
                )
        );

        txtMoisture.setText(
                getString(
                        R.string.moisture_format,
                        moisture
                )
        );

        progressMoisture.setProgress(
                (int) moisture
        );

        txtVoltage.setText(
                getString(
                        R.string.voltage_format,
                        sensor.getVoltage()
                )
        );

        txtRaw.setText(
                String.valueOf(
                        sensor.getRaw()
                )
        );

        updateMoistureUi(moisture);
    }

    private void renderStatus(
            Status status
    ) {

        if (status == null) {
            return;
        }

        latestStatus = status;

        lastStatusReceivedElapsedMillis =
                SystemClock.elapsedRealtime();

        renderEffectiveOnlineStatus();

        relayOn = status.isRelay();

        updatePumpUi(
                relayOn
        );
        renderGardenSummary();
    }

    private void renderPrimaryZoneIdentity(
            List<GardenZone> zones
    ) {
        if (zones == null || zones.isEmpty()) {
            return;
        }

        GardenZone primary = zones.get(0);
        for (GardenZone zone : zones) {
            if ("zone-001".equals(zone.getZone_id())) {
                primary = zone;
                break;
            }
        }

        String emoji = primary.getEmoji() == null
                ? "🌱"
                : primary.getEmoji();
        String name = primary.getName() == null
                ? getString(R.string.title_soil_moisture)
                : primary.getName();
        String sensorId = primary.getSensor_id() == null
                ? "—"
                : primary.getSensor_id();

        txtPrimaryZoneTitle.setText(emoji + " " + name);
        txtPrimaryZoneSubtitle.setText(
                getString(
                        R.string.home_primary_zone_subtitle,
                        sensorId
                )
        );
    }

    private void renderGardenZones(List<GardenZone> zones) {
        latestZones = zones;
        layoutHomeZones.removeAllViews();

        if (zones == null) {
            homeZonePagerAdapter.submitList(null);
            homeZonePagerPositioned = false;
            renderHomeZoneDots(0, 0);
            renderGardenSummary();
            renderHomeWateringSummary(null);
            renderHomeFertilizationSummary(null);
            renderHomeHealthSummary(null);
            return;
        }

        homeZonePagerAdapter.submitList(zones);
        if (
                !homeZonePagerPositioned
                        && !zones.isEmpty()
        ) {
            recyclerHomeZones.scrollToPosition(
                    homeZonePagerAdapter.initialAdapterPosition()
            );
            homeZonePagerPositioned = true;
        }
        renderHomeZoneDots(
                zones.size(),
                homeZonePagerAdapter.toZonePosition(
                        currentHomeZonePage()
                )
        );

        LayoutInflater inflater = LayoutInflater.from(this);
        for (GardenZone zone : zones) {
            View row = inflater.inflate(
                    R.layout.item_home_zone_summary,
                    layoutHomeZones,
                    false
            );
            TextView name = row.findViewById(
                    R.id.txtHomeZoneName
            );
            TextView moisture = row.findViewById(
                    R.id.txtHomeZoneMoisture
            );
            TextView state = row.findViewById(
                    R.id.txtHomeZoneState
            );

            String emoji = zone.getEmoji() == null
                    ? "🌱"
                    : zone.getEmoji();
            name.setText(emoji + " " + zone.getName());

            boolean connected = isZoneConnected(zone);
            moisture.setText(
                    connected
                            ? getString(
                                    R.string.sensor_moisture_format,
                                    zone.getMoisture()
                            )
                            : "—"
            );

            bindHomeZoneState(zone, connected, state);
            row.setOnClickListener(
                    view -> {
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
            layoutHomeZones.addView(row);
        }

        renderPrimaryZoneIdentity(zones);
        renderGardenSummary();
        renderHomeWateringSummary(zones);
        renderHomeFertilizationSummary(zones);
        renderHomeHealthSummary(zones);
        renderHomeAlerts();
    }

    private void renderHomeHealthSummary(List<GardenZone> zones) {
        GardenHealthSummary health = GardenHealthCalculator.calculate(
                zones,
                System.currentTimeMillis() / 1000L
        );
        txtHomeHealthTitle.setText(health.getTitle());
        txtHomeHealthDetail.setText(health.getDetail());
        txtHomeHealthScore.setText(String.valueOf(health.getScore()));
        int healthColor = health.getScore() >= 85 ? R.color.primary
                : health.getScore() >= 65 ? R.color.warning : R.color.moistureLow;
        txtHomeHealthScore.setTextColor(color(healthColor));
        progressHomeHealthScore.setProgressCompat(health.getScore(), true);
        progressHomeHealthScore.setIndicatorColor(color(healthColor));
        progressHomeHealthScore.setTrackColor(color(R.color.divider));
        cardHomeHealth.setVisibility(
                health.getScore() < 85 ? View.VISIBLE : View.GONE
        );
    }

    private void renderHomeWateringSummary(List<GardenZone> zones) {
        if (zones == null || zones.isEmpty()) {
            cardHomeWateringSummary.setVisibility(View.GONE);
            return;
        }

        GardenZone active = null;
        GardenZone queued = null;
        int cooldownCount = 0;
        for (GardenZone zone : zones) {
            ZoneIrrigationStatus irrigation = zone.getIrrigation_status();
            if (irrigation == null) {
                continue;
            }
            if (irrigation.isWatering_active()) {
                active = zone;
                break;
            }
            if (irrigation.isCooldown_active()) {
                cooldownCount++;
            } else if (irrigation.getQueue_position() > 0 && queued == null) {
                queued = zone;
            }
        }

        cardHomeWateringSummary.setVisibility(View.VISIBLE);
        if (active != null) {
            txtHomeWateringSummary.setText(active.getName() + " şu anda sulanıyor");
        } else if (queued != null) {
            txtHomeWateringSummary.setText(queued.getName() + " sulama sırasında bekliyor");
        } else if (cooldownCount > 0) {
            txtHomeWateringSummary.setText(cooldownCount + " bölgede bekleme süresi sürüyor");
        } else {
            txtHomeWateringSummary.setText("Şu anda sulama gerekmiyor");
        }
    }

    private void renderHomeFertilizationSummary(List<GardenZone> zones) {
        if (zones == null || zones.isEmpty()) {
            cardHomeFertilizationSummary.setVisibility(View.GONE);
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        int dueCount = 0;
        int plannedCount = 0;

        for (GardenZone zone : zones) {
            FertilizationProfile profile = zone.getFertilization();
            if (profile == null || !profile.isEnabled()
                    || profile.getNext_application_at_epoch() <= 0L) {
                continue;
            }
            if (profile.getNext_application_at_epoch() <= now) {
                dueCount++;
            } else {
                plannedCount++;
            }
        }

        if (dueCount > 0) {
            cardHomeFertilizationSummary.setVisibility(View.VISIBLE);
            txtHomeFertilizationSummary.setText(
                    dueCount + " bölgede gübreleme kaydı bekleniyor"
            );
        } else if (plannedCount > 0) {
            cardHomeFertilizationSummary.setVisibility(View.VISIBLE);
            txtHomeFertilizationSummary.setText(
                    plannedCount + " bölgede yaklaşan gübreleme planı var"
            );
        } else {
            cardHomeFertilizationSummary.setVisibility(View.GONE);
        }
    }

    private void initializeHomeZonePager() {
        homeZonePagerAdapter = new HomeZonePagerAdapter(
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

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                );
        recyclerHomeZones.setLayoutManager(layoutManager);
        recyclerHomeZones.setAdapter(homeZonePagerAdapter);
        new PagerSnapHelper().attachToRecyclerView(
                recyclerHomeZones
        );
        recyclerHomeZones.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(
                            @androidx.annotation.NonNull RecyclerView view,
                            int newState
                    ) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            renderHomeZoneDots(
                                    homeZonePagerAdapter.getZoneCount(),
                                    homeZonePagerAdapter.toZonePosition(
                                            currentHomeZonePage()
                                    )
                            );
                        }
                    }
                }
        );
    }

    private int currentHomeZonePage() {
        RecyclerView.LayoutManager manager =
                recyclerHomeZones.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager)) {
            return 0;
        }
        int position = (
                (LinearLayoutManager) manager
        ).findFirstCompletelyVisibleItemPosition();
        if (position < 0) {
            position = (
                    (LinearLayoutManager) manager
            ).findFirstVisibleItemPosition();
        }
        return Math.max(0, position);
    }

    private void renderHomeZoneDots(
            int count,
            int selected
    ) {
        layoutHomeZoneDots.removeAllViews();
        for (int index = 0; index < count; index++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(index == selected ? 11f : 8f);
            dot.setTextColor(
                    color(
                            index == selected
                                    ? R.color.primary
                                    : R.color.border
                    )
            );
            dot.setGravity(android.view.Gravity.CENTER);
            int size = Math.round(
                    18f * getResources()
                            .getDisplayMetrics().density
            );
            dot.setLayoutParams(
                    new LinearLayout.LayoutParams(size, size)
            );
            final int page = index;
            dot.setOnClickListener(
                    view -> {
                        int target =
                                homeZonePagerAdapter
                                        .nearestAdapterPosition(
                                                currentHomeZonePage(),
                                                page
                                        );
                        recyclerHomeZones.smoothScrollToPosition(
                                target
                        );
                    }
            );
            layoutHomeZoneDots.addView(dot);
        }
    }

    private void bindHomeZoneState(
            GardenZone zone,
            boolean connected,
            TextView state
    ) {
        if (!connected) {
            setHomeZoneState(
                    state,
                    R.string.home_zone_sensor_waiting,
                    R.color.textSecondary
            );
            return;
        }
        if (!zone.isIrrigation_enabled()) {
            setHomeZoneState(
                    state,
                    R.string.home_zone_disabled,
                    R.color.textSecondary
            );
            return;
        }

        ZoneIrrigationStatus status =
                zone.getIrrigation_status();
        if (status != null && status.isWatering_active()) {
            setHomeZoneState(
                    state,
                    R.string.home_zone_watering,
                    R.color.info
            );
        } else if (
                status != null
                        && status.isCooldown_active()
        ) {
            setHomeZoneState(
                    state,
                    R.string.home_zone_cooldown,
                    R.color.warning
            );
        } else if (
                status != null
                        && status.getQueue_position() > 1
        ) {
            state.setText(
                    getString(
                            R.string.home_zone_queued,
                            status.getQueue_position()
                    )
            );
            state.setTextColor(color(R.color.accentOrange));
        } else {
            setHomeZoneState(
                    state,
                    R.string.home_zone_ready,
                    R.color.online
            );
        }
    }

    private void setHomeZoneState(
            TextView view,
            int textResource,
            int colorResource
    ) {
        view.setText(textResource);
        view.setTextColor(color(colorResource));
    }

    private boolean isZoneConnected(GardenZone zone) {
        if (zone == null || zone.getUpdated_at_epoch() <= 0L) {
            return false;
        }
        long age = Math.max(
                0L,
                System.currentTimeMillis() / 1000L
                        - zone.getUpdated_at_epoch()
        );
        return age <= 90L;
    }

    private void renderGardenSummary() {
        if (latestZones == null) {
            return;
        }

        int connected = 0;
        int cooldownCount = 0;
        GardenZone active = null;
        GardenZone queued = null;
        boolean hasPhysicalValve = false;

        for (GardenZone zone : latestZones) {
            if (isZoneConnected(zone)) {
                connected++;
            }
            if ("PHYSICAL".equalsIgnoreCase(
                    zone.getValve_mode()
            )) {
                hasPhysicalValve = true;
            }

            ZoneIrrigationStatus irrigation =
                    zone.getIrrigation_status();
            if (irrigation == null) {
                continue;
            }
            if (irrigation.isWatering_active()) {
                active = zone;
            } else if (irrigation.isCooldown_active()) {
                cooldownCount++;
            } else if (
                    irrigation.getQueue_position() > 0
                            && queued == null
            ) {
                queued = zone;
            }
        }

        txtGardenSensors.setText(
                getString(
                        R.string.home_sensor_count,
                        connected,
                        latestZones.size()
                )
        );
        cardSimulationWarning.setVisibility(
                hasPhysicalValve ? View.GONE : View.VISIBLE
        );
        valveSimulationMode = !hasPhysicalValve;
        updatePumpUi(relayOn);
        txtGardenPumpSummary.setText(
                relayOn
                        ? R.string.home_pump_running
                        : R.string.home_pump_closed
        );

        if (active != null) {
            txtGardenWateringSummary.setText(
                    getString(
                            R.string.home_watering_active,
                            active.getName()
                    )
            );
        } else if (queued != null) {
            txtGardenWateringSummary.setText(
                    getString(
                            R.string.home_watering_queued,
                            queued.getName()
                    )
            );
        } else if (cooldownCount > 0) {
            txtGardenWateringSummary.setText(
                    getString(
                            R.string.home_watering_cooldown,
                            cooldownCount
                    )
            );
        } else {
            txtGardenWateringSummary.setText(
                    R.string.home_watering_idle
            );
        }
    }

    private void renderEffectiveOnlineStatus() {

        // onStart may run while the anonymous Firebase session is still being
        // created.  Do not render the dashboard until its views are ready.
        if (txtOnline == null || cardOnlineStatus == null) {
            return;
        }

        updateOnlineUi(getConnectionState());
        renderHomeAlerts();
    }

    private ConnectionState getConnectionState() {
        if (
                latestStatus == null
                        || lastStatusReceivedElapsedMillis <= 0L
        ) {
            long connectionWaitMillis =
                    SystemClock.elapsedRealtime()
                            - connectionStartedElapsedMillis;

            return connectionWaitMillis <= ONLINE_TIMEOUT_MILLIS
                    ? ConnectionState.CONNECTING
                    : ConnectionState.OFFLINE;
        }

        return isDeviceEffectivelyOnline()
                ? ConnectionState.ONLINE
                : ConnectionState.OFFLINE;
    }

    private boolean isDeviceEffectivelyOnline() {
        if (
                latestStatus == null
                        || lastStatusReceivedElapsedMillis <= 0L
        ) {
            return false;
        }

        long elapsedSinceLastStatusMillis =
                SystemClock.elapsedRealtime()
                        - lastStatusReceivedElapsedMillis;

        return latestStatus.isOnline()
                && elapsedSinceLastStatusMillis
                <= ONLINE_TIMEOUT_MILLIS;
    }

    private void renderHomeAlerts() {
        if (cardHomeAlerts == null || layoutHomeAlerts == null) {
            return;
        }

        layoutHomeAlerts.removeAllViews();

        if (getConnectionState() == ConnectionState.OFFLINE) {
            addHomeAlert(R.string.home_alert_device_offline);
        }

        if (
                latestStatus != null
                        && latestStatus.getLastError() != null
                        && !latestStatus.getLastError().trim().isEmpty()
        ) {
            addHomeAlert(
                    getString(
                            R.string.home_alert_system_error,
                            latestStatus.getLastError().trim()
                    )
            );
        }

        if (latestZones != null && !latestZones.isEmpty()) {
            boolean anySensorConnected = false;
            for (GardenZone zone : latestZones) {
                if (isZoneConnected(zone)) {
                    anySensorConnected = true;
                    break;
                }
            }
            if (!anySensorConnected) {
                addHomeAlert(R.string.home_alert_no_sensors);
            }
        }

        cardHomeAlerts.setVisibility(
                layoutHomeAlerts.getChildCount() > 0
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void addHomeAlert(int textResource) {
        addHomeAlert(getString(textResource));
    }

    private void addHomeAlert(String message) {
        TextView alert = new TextView(this);
        alert.setText("• " + message);
        alert.setTextColor(color(R.color.textPrimary));
        alert.setTextSize(13f);
        alert.setPadding(0, 4, 0, 4);
        layoutHomeAlerts.addView(alert);
    }


    private void renderCommand(Command command) {

        if (command == null) {
            return;
        }

        boolean autoMode =
                command.isAutoMode();

        if (
                switchAuto.isChecked()
                        != autoMode
        ) {

            updatingAutoSwitch = true;

            switchAuto.setChecked(
                    autoMode
            );

            updatingAutoSwitch = false;
        }

        updateAutoModeUi(
                autoMode
        );
    }

    private void updateOnlineUi(ConnectionState state) {
        boolean online = state == ConnectionState.ONLINE;
        int textResource;
        int textColor;
        int backgroundColor;

        if (online) {
            textResource = R.string.status_online;
            textColor = color(R.color.online);
            backgroundColor = color(R.color.onlineBackground);
        } else if (state == ConnectionState.CONNECTING) {
            textResource = R.string.status_connecting;
            textColor = color(R.color.warning);
            backgroundColor = color(R.color.warningBackground);
        } else {
            textResource = R.string.status_offline;
            textColor = color(R.color.offline);
            backgroundColor = color(R.color.offlineBackground);
        }

        txtOnline.setText(textResource);

        txtOnline.setTextColor(
                textColor
        );

        cardOnlineStatus.setCardBackgroundColor(
                backgroundColor
        );

        cardOnlineStatus.setStrokeColor(
                textColor
        );

        switchAuto.setEnabled(online);
        btnWater.setEnabled(online || relayOn);
    }

    private void updateMoistureUi(long moisture) {

        int statusColor;
        int statusTextResource;

        if (moisture < 30) {

            statusColor =
                    color(R.color.moistureLow);

            statusTextResource =
                    R.string.moisture_very_low;

        } else if (moisture < 50) {

            statusColor =
                    color(R.color.moistureWarning);

            statusTextResource =
                    R.string.moisture_drying;

        } else if (moisture <= 70) {

            statusColor =
                    color(R.color.moistureIdeal);

            statusTextResource =
                    R.string.moisture_ideal;

        } else {

            statusColor =
                    color(R.color.moistureHigh);

            statusTextResource =
                    R.string.moisture_high;
        }

        txtMoisture.setTextColor(
                statusColor
        );

        txtMoistureStatus.setText(
                statusTextResource
        );

        txtMoistureStatus.setTextColor(
                statusColor
        );

        progressMoisture.setProgressTintList(
                ColorStateList.valueOf(
                        statusColor
                )
        );
    }

    private void updatePumpUi(boolean running) {

        if (running) {
            imgPumpStatus.setImageTintList(
                    ColorStateList.valueOf(
                            color(R.color.pumpRunning)
                    )
            );

            txtRelay.setText(
                    R.string.pump_running
            );

            txtRelay.setTextColor(
                    color(R.color.pumpRunning)
            );

            txtPumpDescription.setText(
                    R.string.pump_description_running
            );

            txtPumpDescription.setTextColor(
                    color(R.color.pumpRunning)
            );

            cardPumpStatus.setCardBackgroundColor(
                    color(R.color.pumpRunningBackground)
            );

            cardPumpStatus.setStrokeColor(
                    color(R.color.pumpRunning)
            );

            btnWater.setText(
                    R.string.button_stop_watering
            );

            btnWater.setBackgroundTintList(
                    ColorStateList.valueOf(
                            color(R.color.buttonStop)
                    )
            );

            txtManualHint.setText(
                    R.string.manual_hint_running
            );

            return;
        }

        imgPumpStatus.setImageTintList(
                ColorStateList.valueOf(
                        color(R.color.pumpStopped)
                )
        );

        txtRelay.setText(
                R.string.pump_stopped
        );

        txtRelay.setTextColor(
                color(R.color.pumpStopped)
        );

        txtPumpDescription.setText(
                R.string.pump_description_idle
        );

        txtPumpDescription.setTextColor(
                color(R.color.textSecondary)
        );

        cardPumpStatus.setCardBackgroundColor(
                color(R.color.surfaceSoft)
        );

        cardPumpStatus.setStrokeColor(
                color(R.color.border)
        );

        btnWater.setText(
                valveSimulationMode
                        ? R.string.manual_relay_test_button
                        : R.string.button_start_manual_watering
        );

        btnWater.setBackgroundTintList(
                ColorStateList.valueOf(
                        color(R.color.buttonStart)
                )
        );

        txtManualHint.setText(
                valveSimulationMode
                        ? R.string.manual_relay_test_hint
                        : R.string.manual_hint_idle
        );
    }

    private void updateAutoModeUi(boolean enabled) {

        if (enabled) {

            cardAutoMode.setCardBackgroundColor(
                    color(R.color.onlineBackground)
            );

            cardAutoMode.setStrokeColor(
                    color(R.color.online)
            );

            txtAutoDescription.setText(
                    R.string.auto_mode_active_description
            );

            txtAutoDescription.setTextColor(
                    color(R.color.online)
            );

            return;
        }

        cardAutoMode.setCardBackgroundColor(
                color(R.color.surfaceSoft)
        );

        cardAutoMode.setStrokeColor(
                color(R.color.border)
        );

        txtAutoDescription.setText(
                R.string.auto_mode_inactive_description
        );

        txtAutoDescription.setTextColor(
                color(R.color.textSecondary)
        );
    }

    private void initializeButtons() {

        btnMainMenu.setOnClickListener(
                view -> showMainMenu()
        );

        cardGardenSummary.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                SensorPointsActivity.class
                        )
                )
        );

        cardHomeAi.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                AIAssistantActivity.class
                        )
                )
        );

        cardHomeHealth.setOnClickListener(
                view -> startActivity(
                        new Intent(this, GardenHealthDetailActivity.class)
                )
        );

        cardHomeWateringSummary.setOnClickListener(
                view -> startActivity(
                        new Intent(this, AIAssistantActivity.class)
                )
        );

        cardHomePlantDoctorSummary.setOnClickListener(
                view -> startActivity(
                        new Intent(this, PlantDoctorActivity.class)
                )
        );

        cardHomeWeather.setOnClickListener(
                view -> startActivity(new Intent(this, WeatherForecastActivity.class))
        );

        cardHomeFertilizationSummary.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                FertilizationCalendarActivity.class
                        )
                )
        );

        btnHomeZones.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                SensorPointsActivity.class
                        )
                )
        );

        btnHomeAi.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                AIAssistantActivity.class
                        )
                )
        );

        btnHomeSettings.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                this,
                                SettingsActivity.class
                        )
                )
        );

        btnToggleAdvanced.setOnClickListener(
                view -> toggleAdvancedControls()
        );

        btnWater.setOnClickListener(
                view -> {

                    if (relayOn) {

                        viewModel.setRelay(
                                false
                        );

                        return;
                    }

                    if (valveSimulationMode) {
                        showRelayTestConfirmation();
                    } else {
                        startActivity(
                                new Intent(
                                        this,
                                        SensorPointsActivity.class
                                )
                        );
                    }
                }
        );

        switchAuto.setOnCheckedChangeListener(
                (
                        buttonView,
                        checked
                ) -> {

                    if (updatingAutoSwitch) {
                        return;
                    }

                    updateAutoModeUi(
                            checked
                    );

                    viewModel.setAutoMode(
                            checked
                    );

                    if (checked) {

                        viewModel.setRelay(
                                false
                        );
                    }
                }
        );
    }

    private void toggleAdvancedControls() {
        advancedControlsVisible = !advancedControlsVisible;
        int visibility = advancedControlsVisible
                ? View.VISIBLE
                : View.GONE;

        cardPrimaryZoneDetails.setVisibility(visibility);
        cardWateringControl.setVisibility(visibility);
        btnToggleAdvanced.setText(
                advancedControlsVisible
                        ? R.string.home_hide_advanced
                        : R.string.home_show_advanced
        );
    }

    private void showRelayTestConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.manual_relay_test_title)
                .setMessage(R.string.manual_relay_test_message)
                .setNegativeButton(
                        R.string.manual_relay_test_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.manual_relay_test_confirm,
                        (dialog, which) -> {
                            viewModel.setAutoMode(false);
                            viewModel.setRelay(true);
                        }
                )
                .show();
    }
    private void showMainMenu() {

        String tag =
                "MainMenuBottomSheet";

        if (
                getSupportFragmentManager()
                        .findFragmentByTag(tag)
                        != null
        ) {
            return;
        }

        MainMenuBottomSheet bottomSheet =
                new MainMenuBottomSheet();

        bottomSheet.show(
                getSupportFragmentManager(),
                tag
        );
    }
    @Override
    protected void onStart() {

        super.onStart();
        renderHomePlantDoctorRecommendation();

        onlineStatusHandler.removeCallbacks(
                onlineStatusChecker
        );

        onlineStatusHandler.post(
                onlineStatusChecker
        );
    }

    private void renderHomePlantDoctorRecommendation() {
        if (txtHomePlantDoctorSummary == null) return;
        txtHomePlantDoctorSummary.setText(
                PlantDoctorRecommendationStore.summary(this)
        );
    }


    @Override
    protected void onStop() {

        onlineStatusHandler.removeCallbacks(
                onlineStatusChecker
        );

        super.onStop();
    }
    private int color(int colorResource) {

        return ContextCompat.getColor(
                this,
                colorResource
        );
    }
}
