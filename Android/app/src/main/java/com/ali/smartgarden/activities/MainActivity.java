package com.ali.smartgarden.activities;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.fertilization.FertilizerReminderScheduler;
import com.ali.smartgarden.notifications.NotificationSignalCoordinator;
import com.ali.smartgarden.notifications.NotificationSignalScheduler;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ali.smartgarden.adapters.HomeZonePagerAdapter;
import com.ali.smartgarden.models.Status;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.FertilizationProfile;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.health.GardenHealthCalculator;
import com.ali.smartgarden.health.GardenHealthSummary;
import com.ali.smartgarden.plantassistant.PlantAssistantRecommendationStore;
import com.ali.smartgarden.plantassistant.PlantAssistantHomeRecommendation;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.ali.smartgarden.ui.MainMenuBottomSheet;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    private long lastStatusReceivedElapsedMillis = 0L;
    private long connectionStartedElapsedMillis;

    // Header
    private MaterialCardView cardOnlineStatus;
    private TextView txtOnline;
    private TextView txtDevice;

    // Sensor

    // Pump

    // Automatic mode

    // Manual control


    private MaterialButton btnMainMenu;
    private TextView txtMainNotificationBadge;
    private MaterialCardView cardHomePlantAssistantSummary;
    private TextView txtHomePlantAssistantSummary;
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
    private TextView txtHomeWeatherLocation;
    private TextView txtHomeWeatherTodayIcon;
    private TextView txtHomeWeatherTodayTemperature;
    private TextView txtHomeWeatherTodayRain;
    private TextView txtHomeWeatherTodayWind;
    private TextView txtHomeWeatherTomorrowIcon;
    private TextView txtHomeWeatherTomorrowTemperature;
    private TextView txtHomeWeatherTomorrowRain;
    private TextView txtHomeWeatherTomorrowWind;
    private TextView txtHomeWeatherImpact;
    private TextView txtHomeWeatherImpactIcon;
    private TextView txtHomeWeatherUpdated;
    private MaterialCardView cardHomeAlerts;
    private LinearLayout layoutHomeAlerts;
    private RecyclerView recyclerHomeZones;
    private LinearLayout layoutHomeZoneDots;
    private HomeZonePagerAdapter homeZonePagerAdapter;
    private boolean homeZonePagerPositioned = false;
    private List<GardenZone> latestZones;
    private WeatherForecast latestWeather;


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
        applyWindowInsets();
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.HOME);
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeButtons();
        FertilizerReminderScheduler.schedule(this);
        NotificationSignalScheduler.schedule(this);
        new GardenNotificationManager(this).restoreCloudBackup(imported -> { });
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(
                token -> new FirebaseRepository().savePushToken(token)
        );
    }

    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.mainRoot),
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


    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (txtMainNotificationBadge == null) return;
        int unread = 0;
        for (com.ali.smartgarden.models.GardenNotification item : new GardenNotificationManager(this).localNotifications()) {
            if (!item.isRead()) unread++;
        }
        if (unread <= 0) {
            txtMainNotificationBadge.setVisibility(View.GONE);
        } else {
            txtMainNotificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            txtMainNotificationBadge.setVisibility(View.VISIBLE);
        }
    }
    private void initializeViews() {


        txtMainNotificationBadge = findViewById(R.id.txtMainNotificationBadge);
        updateNotificationBadge();
        btnMainMenu = findViewById(
                R.id.btnMainMenu
        );
        findViewById(R.id.btnMainNotifications).setOnClickListener(
                view -> startActivity(new Intent(this, NotificationCenterActivity.class))
        );
        cardHomePlantAssistantSummary = findViewById(
                R.id.cardHomePlantAssistantSummary
        );
        txtHomePlantAssistantSummary = findViewById(
                R.id.txtHomePlantAssistantSummary
        );
        renderHomePlantAssistantRecommendation();
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
        txtHomeWeatherLocation = findViewById(R.id.txtHomeWeatherLocation);
        txtHomeWeatherTodayIcon = findViewById(R.id.txtHomeWeatherTodayIcon);
        txtHomeWeatherTodayTemperature = findViewById(R.id.txtHomeWeatherTodayTemperature);
        txtHomeWeatherTodayRain = findViewById(R.id.txtHomeWeatherTodayRain);
        txtHomeWeatherTodayWind = findViewById(R.id.txtHomeWeatherTodayWind);
        txtHomeWeatherTomorrowIcon = findViewById(R.id.txtHomeWeatherTomorrowIcon);
        txtHomeWeatherTomorrowTemperature = findViewById(R.id.txtHomeWeatherTomorrowTemperature);
        txtHomeWeatherTomorrowRain = findViewById(R.id.txtHomeWeatherTomorrowRain);
        txtHomeWeatherTomorrowWind = findViewById(R.id.txtHomeWeatherTomorrowWind);
        txtHomeWeatherImpact = findViewById(R.id.txtHomeWeatherImpact);
        txtHomeWeatherImpactIcon = findViewById(R.id.txtHomeWeatherImpactIcon);
        txtHomeWeatherUpdated = findViewById(R.id.txtHomeWeatherUpdated);
        moveWeatherBelowFertilization();
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
        initializeHomeZonePager();

        cardOnlineStatus = findViewById(R.id.cardOnlineStatus);
        txtOnline = findViewById(R.id.txtOnline);
        txtDevice = findViewById(R.id.txtDevice);





        txtDevice.setText(
                R.string.default_device_name
        );
    }

    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);
    }

    private void observeViewModel() {


        viewModel.getStatus().observe(
                this,
                this::renderStatus
        );


        viewModel.getGardenZones().observe(
                this,
                this::renderGardenZones
        );


        viewModel.getWeatherForecast().observe(this, this::renderHomeWeather);
        viewModel.getWateringHistory().observe(this, values -> NotificationSignalCoordinator.evaluateWatering(this, values));

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


    private void moveWeatherBelowFertilization() {
        if (cardHomeWeather == null || cardHomeFertilizationSummary == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) cardHomeWeather.getParent();
        if (parent == null || parent != cardHomeFertilizationSummary.getParent()) {
            return;
        }
        parent.removeView(cardHomeWeather);
        int targetIndex = parent.indexOfChild(cardHomeFertilizationSummary) + 1;
        parent.addView(cardHomeWeather, Math.min(targetIndex, parent.getChildCount()));
    }

    private void renderHomeWeather(WeatherForecast forecast) {
        latestWeather = forecast;
        if (forecast != null) {
            NotificationSignalCoordinator.evaluateWeather(this, forecast.getTomorrowTemperatureMax(),
                    forecast.getTomorrowRainProbability(), forecast.getTomorrowWindMax(),
                    java.time.LocalDate.now().plusDays(1).toString());
        }
        renderHomePlantAssistantRecommendation();
        if (forecast == null || forecast.getTomorrowTemperatureMax() == null) {
            cardHomeWeather.setVisibility(View.GONE);
            return;
        }
        cardHomeWeather.setVisibility(View.VISIBLE);
        String location = forecast.getDistrict().isBlank()
                ? forecast.getCity()
                : forecast.getDistrict() + " / " + forecast.getCity();
        txtHomeWeatherTitle.setText("Hava durumu");
        txtHomeWeatherLocation.setText(getString(R.string.symbol_middle_dot) + " " + location);
        txtHomeWeatherIcon.setText(getString(R.string.symbol_sun));
        txtHomeWeatherUpdated.setText(
                "↻ Güncellendi: " + new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))
                        .format(new Date())
        );
        bindWeatherDay(txtHomeWeatherTodayIcon, txtHomeWeatherTodayTemperature,
                txtHomeWeatherTodayRain, txtHomeWeatherTodayWind,
                forecast.getTodayTemperatureMax(), forecast.getTodayRainProbability(),
                forecast.getTodayWindMax(), forecast.getTodayWeatherCode());
        bindWeatherDay(txtHomeWeatherTomorrowIcon, txtHomeWeatherTomorrowTemperature,
                txtHomeWeatherTomorrowRain, txtHomeWeatherTomorrowWind,
                forecast.getTomorrowTemperatureMax(), forecast.getTomorrowRainProbability(),
                forecast.getTomorrowWindMax(), forecast.getTomorrowWeatherCode());
        WeatherImpact impact = weatherGardenImpact(forecast);
        txtHomeWeatherImpactIcon.setText(impact.icon);
        setWeatherImpact(impact.message);
    }

    /** A short advisory-only reading of weather conditions for the garden. */
    private WeatherImpact weatherGardenImpact(WeatherForecast forecast) {
        double todayMax = valueOrZero(forecast.getTodayTemperatureMax());
        double tomorrowMax = valueOrZero(forecast.getTomorrowTemperatureMax());
        double hottest = Math.max(todayMax, tomorrowMax);
        double rain = Math.max(valueOrZero(forecast.getTodayRainProbability()),
                valueOrZero(forecast.getTomorrowRainProbability()));
        double wind = Math.max(valueOrZero(forecast.getTodayWindMax()),
                valueOrZero(forecast.getTomorrowWindMax()));

        // Current weather code is an observation from the provider; it is more
        // meaningful than the daily probability when rain is already falling.
        if (isRainNow(forecast.getCurrentWeatherCode())) {
            return new WeatherImpact(getString(R.string.symbol_rain), "Şu an yağış görünüyor — sulama öncesi toprak nemini yeniden kontrol edin.");
        }
        if (rain >= 60D) {
            return new WeatherImpact(getString(R.string.symbol_rain), "Yağış olasılığı yüksek — sulama öncesi toprak nemini yeniden kontrol edin.");
        }
        if (rain >= 30D) {
            return new WeatherImpact(getString(R.string.symbol_rain), "Yağış ihtimali var — sulama öncesi toprak nemini yeniden kontrol edin.");
        }
        if (hottest >= 35D) {
            return new WeatherImpact(getString(R.string.symbol_sun), "Yüksek sıcaklık — toprak nemini ve yapraklarda solmayı takip edin.");
        }
        if (wind >= 30D) {
            return new WeatherImpact(getString(R.string.symbol_wind), "Rüzgâr kuvvetli — toprak nemi daha hızlı düşebilir.");
        }
        if (hottest >= 30D && rain <= 20D) {
            return new WeatherImpact(getString(R.string.symbol_water_drop), "Sulama açısından sıcak ve kurak bir dönem. Toprak nemini takip etmeyi unutmayın.");
        }
        return new WeatherImpact(getString(R.string.symbol_plant), "Bahçe için hava koşulları dengeli görünüyor.");
    }

    private boolean isRainNow(Long weatherCode) {
        if (weatherCode == null) return false;
        long code = weatherCode;
        // Open-Meteo rain/drizzle/showers/thunderstorm ranges; OpenWeather is
        // normalized by the backend into the same representative codes.
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || code >= 95;
    }

    private void setWeatherImpact(String impact) {
        String prefix = "AVORA yorumu: ";
        SpannableString text = new SpannableString(prefix + impact);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        txtHomeWeatherImpact.setText(text);
    }

    private static final class WeatherImpact {
        private final String icon;
        private final String message;

        private WeatherImpact(String icon, String message) {
            this.icon = icon;
            this.message = message;
        }
    }

    private double valueOrZero(Double value) {
        return value == null ? 0D : value;
    }

    private void bindWeatherDay(TextView icon, TextView temperatureView, TextView rainView,
                                TextView windView, Double temperature, Double rain,
                                Double wind, Long code) {
        icon.setText(weatherIcon(code));
        temperatureView.setText(temperature == null ? "—" : Math.round(temperature) + "°C");
        rainView.setText(getString(R.string.format_rain_probability, rain == null ? "%—" : "%" + Math.round(rain)));
        windView.setText(getString(R.string.symbol_wind) + "  " + (wind == null ? "~ —" : "~ " + Math.round(wind)) + " km/sa");
    }

    private int weatherIconResource(Long code) {
        if (code != null && code >= 51) return R.drawable.ic_water_drop_24;
        if (code != null && code >= 2) return R.drawable.ic_weather_cloud_24;
        return R.drawable.ic_weather_sunny_24;
    }

    private String weatherIcon(Long code) {
        if (code == null) return getString(R.string.symbol_sun);
        if (code >= 95) return getString(R.string.symbol_warning);
        if (code >= 51) return getString(R.string.symbol_rain);
        if (code >= 45) return getString(R.string.symbol_weather_cloud);
        if (code >= 2) return getString(R.string.symbol_weather_cloud);
        return getString(R.string.symbol_sun);
    }


    private void renderStatus(
            Status status
    ) {

        if (status == null) {
            return;
        }

        latestStatus = status;
        NotificationSignalCoordinator.evaluateDevice(this, status, null);

        lastStatusReceivedElapsedMillis =
                SystemClock.elapsedRealtime();

        renderEffectiveOnlineStatus();

    }


    private void renderGardenZones(List<GardenZone> zones) {
        latestZones = zones;

        if (zones == null) {
            homeZonePagerAdapter.submitList(null);
            homeZonePagerPositioned = false;
            renderHomeZoneDots(0, 0);
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


        renderHomeWateringSummary(zones);
        renderHomeFertilizationSummary(zones);
        renderHomeHealthSummary(zones);
        renderHomePlantAssistantRecommendation();
        renderHomeAlerts();
    }

    private void renderHomeHealthSummary(List<GardenZone> zones) {
        GardenHealthSummary health = GardenHealthCalculator.calculate(
                zones,
                System.currentTimeMillis() / 1000L,
                PlantAssistantRecommendationStore.healthSignal(this)
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
                    Intent intent = new Intent(this, PlantTimelineActivity.class);
                    intent.putExtra("zone_id", zone.getZone_id());
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
        float density = getResources().getDisplayMetrics().density;
        for (int index = 0; index < count; index++) {
            boolean isSelected = index == selected;
            android.widget.FrameLayout touchTarget =
                    new android.widget.FrameLayout(this);
            int touchSize = Math.round(18f * density);
            touchTarget.setLayoutParams(
                    new LinearLayout.LayoutParams(touchSize, touchSize)
            );

            View dot = new View(this);
            int dotSize = Math.round(
                    (isSelected ? 7f : 5f) * density
            );
            GradientDrawable dotBackground = new GradientDrawable();
            dotBackground.setShape(GradientDrawable.OVAL);
            dotBackground.setColor(
                    color(
                            isSelected
                                    ? R.color.primary
                                    : R.color.border
                    )
            );
            dot.setBackground(dotBackground);
            android.widget.FrameLayout.LayoutParams dotParams =
                    new android.widget.FrameLayout.LayoutParams(
                            dotSize,
                            dotSize,
                            android.view.Gravity.CENTER
                    );
            touchTarget.addView(dot, dotParams);

            final int page = index;
            touchTarget.setOnClickListener(
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
            layoutHomeZoneDots.addView(touchTarget);
        }
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

    }




    private void initializeButtons() {

        btnMainMenu.setOnClickListener(
                view -> showMainMenu()
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

        cardHomePlantAssistantSummary.setOnClickListener(
                view -> startActivity(
                        new Intent(this, PlantAssistantActivity.class)
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
        renderHomePlantAssistantRecommendation();

        onlineStatusHandler.removeCallbacks(
                onlineStatusChecker
        );

        onlineStatusHandler.post(
                onlineStatusChecker
        );
    }

    private void renderHomePlantAssistantRecommendation() {
        if (txtHomePlantAssistantSummary == null) return;
        txtHomePlantAssistantSummary.setText(
                PlantAssistantHomeRecommendation.create(
                        latestZones,
                        latestWeather,
                        PlantAssistantRecommendationStore.healthSignal(this),
                        System.currentTimeMillis() / 1000L
                )
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
