package com.ali.smartgarden.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.adapters.DecisionStepAdapter;
import com.ali.smartgarden.models.AIDecision;
import com.ali.smartgarden.models.AdaptiveRecommendation;
import com.ali.smartgarden.models.AIExplanation;
import com.ali.smartgarden.ui.DecisionFlowFactory;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.ali.smartgarden.models.PredictionValidationStatus;
import com.ali.smartgarden.models.MoisturePrediction;
import com.ali.smartgarden.models.PredictionAccuracy;
import com.ali.smartgarden.models.UnifiedConfidence;
import com.ali.smartgarden.models.SoilLearningProfile;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.ali.smartgarden.models.WeatherForecast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AIAssistantActivity extends AppCompatActivity {

    private static final String TAG = "AIAssistantActivity";

    private RecyclerView recyclerAIDecisionFlow;
    private DecisionStepAdapter decisionStepAdapter;

    private MaterialCardView cardAIDecision;
    private MaterialCardView cardAIZoneSummary;
    private MaterialCardView cardAIReasons;
    private MaterialCardView cardAIDecisionFlow;
    private MaterialCardView cardAIProgress;
    private MaterialCardView cardAINextStep;
    private MaterialCardView cardAIAdaptiveRecommendation;
    private MaterialCardView cardAIWeatherGuidance;
    private MaterialCardView cardAITechnicalSummary;
    private MaterialCardView cardAISeverityBadge;
    private MaterialButton btnBack;
    private MaterialButton btnAIAdvancedDetails;
    private MaterialCardView cardAIWateringControl;
    private MaterialCardView cardAISensorPoints;
    private MaterialCardView cardPredictionValidationStatusBadge;
    private MaterialCardView cardPredictionValidation;
    private MaterialCardView cardMoisturePrediction;
    private MaterialCardView cardMoisturePredictionStatusBadge;
    private MaterialCardView cardPredictionAccuracy;
    private MaterialCardView cardPredictionAccuracyStatusBadge;
    private MaterialCardView cardUnifiedConfidence;
    private MaterialCardView cardUnifiedConfidenceStatusBadge;
    private LinearLayout layoutAIZoneSummary;

    private TextView txtPredictionValidationStatus;
    private TextView txtPredictionValidationRemaining;
    private TextView txtPredictionValidationPercent;
    private TextView txtPredictionValidationTarget;
    private TextView txtPredictionValidationPending;
    private TextView txtPredictionValidationNextTime;
    private TextView txtPredictionValidationUpdatedAt;


    private TextView txtAIDecisionTitle;
    private TextView txtAIAnalysisScope;
    private TextView txtAIDecisionSummary;
    private TextView txtAISeverityBadge;

    private TextView txtAIReasonOne;
    private TextView txtAIReasonTwo;
    private TextView txtAIReasonThree;

    private TextView txtAIProgressPercent;
    private TextView txtAILearningStage;
    private TextView txtAILearningDescription;
    private TextView txtAILearningSensorCount;
    private TextView txtAILearningSensorStatus;
    private TextView txtAILearningWateringCount;
    private TextView txtAILearningWateringStatus;

    private TextView txtAINextStep;
    private TextView txtAIWeatherGuidanceTitle;
    private TextView txtAIWeatherGuidanceSummary;
    private TextView txtAIWeatherGuidanceSafety;
    private TextView txtAIAdaptiveRecommendationTitle;
    private TextView txtAIAdaptiveRecommendationDetail;
    private TextView txtAIConfidence;
    private TextView txtAISoilClassification;
    private TextView txtAITrendClassification;
    private TextView txtAIUpdatedAt;
    private TextView txtAISoilRetention;
    private TextView txtAIAverageDrying;
    private TextView txtAIDryingStatus;
    private TextView txtAIWateringEfficiency;
    private TextView txtAIWateringEfficiencyStatus;

    private TextView txtMoisturePredictionStatus;
    private TextView txtPredictionCurrentMoisture;
    private TextView txtMoisturePredictionTitle;
    private TextView txtMoisturePredictionZone;
    private TextView txtPredictionMoistureLimit;

    private TextView txtPredictionOneHour;
    private TextView txtPredictionThreeHours;
    private TextView txtPredictionSixHours;

    private TextView txtPredictionTimeUntilLimit;
    private TextView txtMoisturePredictionConfidence;
    private TextView txtPredictionLimitReachedAt;
    private TextView txtMoisturePredictionUpdatedAt;

    private TextView txtPredictionAccuracyStatus;
    private TextView txtPredictionAccuracyPercent;
    private TextView txtPredictionConfidenceMultiplier;

    private TextView txtPredictionCount;
    private TextView txtSuccessfulPredictions;
    private TextView txtPredictionAverageError;

    private TextView txtPredictionMinimumError;
    private TextView txtPredictionMaximumError;
    private TextView txtPredictionAccuracyUpdatedAt;
    private TextView txtUnifiedConfidenceStatus;
    private TextView txtUnifiedOverallConfidence;
    private TextView txtUnifiedConfidenceLevel;

    private TextView txtUnifiedSoilConfidence;
    private TextView txtUnifiedPredictionAccuracy;
    private TextView txtUnifiedSensorConfidence;

    private TextView txtUnifiedTrendConfidence;
    private TextView txtUnifiedWeightedScore;
    private TextView txtUnifiedConfidenceUpdatedAt;


    private LinearProgressIndicator progressAILearningSensor;
    private LinearProgressIndicator progressAILearningWatering;
    private LinearProgressIndicator progressPredictionAccuracy;
    private LinearProgressIndicator progressUnifiedConfidence;
    private LinearProgressIndicator progressPredictionValidation;

    private final View[] progressSegments = new View[5];

    private MainViewModel viewModel;

    private ValueAnimator progressAnimator;
    private int currentLearningProgress = 0;
    private boolean advancedDetailsVisible = false;
    private boolean learningDataCollectionCompleted = false;
    private int profileLearningStage = 0;
    private final List<GardenZone> predictionZones = new ArrayList<>();
    private int selectedPredictionZoneIndex = 0;
    private MoisturePrediction latestMoisturePrediction;
    private WeatherForecast latestWeatherForecast;
    private float predictionSwipeStartX;
    private float predictionSwipeStartY;
    private boolean predictionHorizontalSwipe;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ai_assistant);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.ASSISTANT);

        initializeViews();
        initializeRecyclerView();
        initializeViewModel();
        initializeListeners();
        observeViewModel();

        /*
         * Firebase verisi gelmeden önce başlangıç görünümü.
         */
        renderLearningProgressImmediately(0);

        startEntranceAnimations();
    }

    /**
     * XML içerisindeki bütün View bileşenlerini Java tarafına bağlar.
     */
    private void initializeViews() {

        cardAIDecision =
                findViewById(R.id.cardAIDecision);

        cardAIZoneSummary =
                findViewById(R.id.cardAIZoneSummary);

        cardAIReasons =
                findViewById(R.id.cardAIReasons);

        cardAIDecisionFlow =
                findViewById(R.id.cardAIDecisionFlow);

        cardAIProgress =
                findViewById(R.id.cardAIProgress);

        cardAINextStep =
                findViewById(R.id.cardAINextStep);
        cardAIWeatherGuidance =
                findViewById(R.id.cardAIWeatherGuidance);

        cardAIAdaptiveRecommendation =
                findViewById(R.id.cardAIAdaptiveRecommendation);

        cardAITechnicalSummary =
                findViewById(R.id.cardAITechnicalSummary);

        cardAISeverityBadge =
                findViewById(R.id.cardAISeverityBadge);

        cardUnifiedConfidence =
                findViewById(
                        R.id.cardUnifiedConfidence
                );

        cardUnifiedConfidenceStatusBadge =
                findViewById(
                        R.id.cardUnifiedConfidenceStatusBadge
                );

        layoutAIZoneSummary =
                findViewById(R.id.layoutAIZoneSummary);


        btnBack =
                findViewById(R.id.btnBack);

        btnAIAdvancedDetails =
                findViewById(R.id.btnAIAdvancedDetails);
        cardAIWateringControl =
                findViewById(R.id.cardAIWateringControl);
        cardAISensorPoints =
                findViewById(R.id.cardAISensorPoints);

        txtAIDecisionTitle =
                findViewById(R.id.txtAIDecisionTitle);

        txtAIAnalysisScope =
                findViewById(R.id.txtAIAnalysisScope);

        txtAIDecisionSummary =
                findViewById(R.id.txtAIDecisionSummary);

        txtAISeverityBadge =
                findViewById(R.id.txtAISeverityBadge);

        txtAIReasonOne =
                findViewById(R.id.txtAIReasonOne);

        txtAIReasonTwo =
                findViewById(R.id.txtAIReasonTwo);

        txtAIReasonThree =
                findViewById(R.id.txtAIReasonThree);

        txtAIProgressPercent =
                findViewById(R.id.txtAIProgressPercent);

        txtAILearningStage =
                findViewById(R.id.txtAILearningStage);

        txtAILearningDescription =
                findViewById(R.id.txtAILearningDescription);

        txtAILearningSensorCount =
                findViewById(R.id.txtAILearningSensorCount);

        txtAILearningSensorStatus =
                findViewById(R.id.txtAILearningSensorStatus);

        txtAILearningWateringCount =
                findViewById(R.id.txtAILearningWateringCount);

        txtAILearningWateringStatus =
                findViewById(R.id.txtAILearningWateringStatus);


        progressAILearningSensor =
                findViewById(R.id.progressAILearningSensor);

        progressAILearningWatering =
                findViewById(R.id.progressAILearningWatering);

        progressUnifiedConfidence =
                findViewById(
                        R.id.progressUnifiedConfidence
                );


        txtAINextStep =
                findViewById(R.id.txtAINextStep);
        txtAIWeatherGuidanceTitle =
                findViewById(R.id.txtAIWeatherGuidanceTitle);
        txtAIWeatherGuidanceSummary =
                findViewById(R.id.txtAIWeatherGuidanceSummary);
        txtAIWeatherGuidanceSafety =
                findViewById(R.id.txtAIWeatherGuidanceSafety);

        txtAIAdaptiveRecommendationTitle =
                findViewById(R.id.txtAIAdaptiveRecommendationTitle);

        txtAIAdaptiveRecommendationDetail =
                findViewById(R.id.txtAIAdaptiveRecommendationDetail);

        txtAIConfidence =
                findViewById(R.id.txtAIConfidence);

        txtAISoilClassification =
                findViewById(R.id.txtAISoilClassification);

        txtAITrendClassification =
                findViewById(R.id.txtAITrendClassification);

        txtAIUpdatedAt =
                findViewById(R.id.txtAIUpdatedAt);

        txtAISoilRetention =
                findViewById(R.id.txtAISoilRetention);

        txtAIAverageDrying =
                findViewById(R.id.txtAIAverageDrying);

        txtAIDryingStatus =
                findViewById(
                        R.id.txtAIDryingStatus
                );

        txtAIWateringEfficiency =
                findViewById(R.id.txtAIWateringEfficiency);

        txtAIWateringEfficiencyStatus =
                findViewById(
                        R.id.txtAIWateringEfficiencyStatus
                );

        txtPredictionValidationStatus =
                findViewById(
                        R.id.txtPredictionValidationStatus
                );

        txtPredictionValidationRemaining =
                findViewById(
                        R.id.txtPredictionValidationRemaining
                );

        txtPredictionValidationPercent =
                findViewById(
                        R.id.txtPredictionValidationPercent
                );

        progressPredictionValidation =
                findViewById(
                        R.id.progressPredictionValidation
                );
        progressPredictionAccuracy =
                findViewById(
                        R.id.progressPredictionAccuracy
                );

        txtPredictionValidationTarget =
                findViewById(
                        R.id.txtPredictionValidationTarget
                );

        txtPredictionValidationPending =
                findViewById(
                        R.id.txtPredictionValidationPending
                );

        txtPredictionValidationNextTime =
                findViewById(
                        R.id.txtPredictionValidationNextTime
                );

        txtPredictionValidationUpdatedAt =
                findViewById(
                        R.id.txtPredictionValidationUpdatedAt
                );
        cardPredictionValidation =
                findViewById(
                        R.id.cardPredictionValidation
                );

        cardPredictionValidationStatusBadge =
                findViewById(
                        R.id.cardPredictionValidationStatusBadge
                );

        cardPredictionAccuracy =
                findViewById(
                        R.id.cardPredictionAccuracy
                );

        cardPredictionAccuracyStatusBadge =
                findViewById(
                        R.id.cardPredictionAccuracyStatusBadge
                );

        cardMoisturePrediction =
                findViewById(
                        R.id.cardMoisturePrediction
                );

        cardMoisturePredictionStatusBadge =
                findViewById(
                        R.id.cardMoisturePredictionStatusBadge
                );

        txtMoisturePredictionTitle =
                findViewById(R.id.txtMoisturePredictionTitle);

        txtMoisturePredictionZone =
                findViewById(R.id.txtMoisturePredictionZone);

        txtMoisturePredictionStatus =
                findViewById(
                        R.id.txtMoisturePredictionStatus
                );

        txtPredictionCurrentMoisture =
                findViewById(
                        R.id.txtPredictionCurrentMoisture
                );

        txtPredictionMoistureLimit =
                findViewById(
                        R.id.txtPredictionMoistureLimit
                );

        txtPredictionOneHour =
                findViewById(
                        R.id.txtPredictionOneHour
                );

        txtPredictionThreeHours =
                findViewById(
                        R.id.txtPredictionThreeHours
                );

        txtPredictionSixHours =
                findViewById(
                        R.id.txtPredictionSixHours
                );

        txtPredictionTimeUntilLimit =
                findViewById(
                        R.id.txtPredictionTimeUntilLimit
                );

        txtMoisturePredictionConfidence =
                findViewById(
                        R.id.txtMoisturePredictionConfidence
                );

        txtPredictionLimitReachedAt =
                findViewById(
                        R.id.txtPredictionLimitReachedAt
                );
        txtPredictionAccuracyStatus =
                findViewById(
                        R.id.txtPredictionAccuracyStatus
                );

        txtPredictionAccuracyPercent =
                findViewById(
                        R.id.txtPredictionAccuracyPercent
                );

        txtPredictionConfidenceMultiplier =
                findViewById(
                        R.id.txtPredictionConfidenceMultiplier
                );

        txtPredictionCount =
                findViewById(
                        R.id.txtPredictionCount
                );

        txtSuccessfulPredictions =
                findViewById(
                        R.id.txtSuccessfulPredictions
                );

        txtPredictionAverageError =
                findViewById(
                        R.id.txtPredictionAverageError
                );

        txtPredictionMinimumError =
                findViewById(
                        R.id.txtPredictionMinimumError
                );

        txtPredictionMaximumError =
                findViewById(
                        R.id.txtPredictionMaximumError
                );

        txtPredictionAccuracyUpdatedAt =
                findViewById(
                        R.id.txtPredictionAccuracyUpdatedAt
                );

        txtMoisturePredictionUpdatedAt =
                findViewById(
                        R.id.txtMoisturePredictionUpdatedAt
                );
        txtUnifiedConfidenceStatus =
                findViewById(
                        R.id.txtUnifiedConfidenceStatus
                );

        txtUnifiedOverallConfidence =
                findViewById(
                        R.id.txtUnifiedOverallConfidence
                );

        txtUnifiedConfidenceLevel =
                findViewById(
                        R.id.txtUnifiedConfidenceLevel
                );

        txtUnifiedSoilConfidence =
                findViewById(
                        R.id.txtUnifiedSoilConfidence
                );

        txtUnifiedPredictionAccuracy =
                findViewById(
                        R.id.txtUnifiedPredictionAccuracy
                );

        txtUnifiedSensorConfidence =
                findViewById(
                        R.id.txtUnifiedSensorConfidence
                );

        txtUnifiedTrendConfidence =
                findViewById(
                        R.id.txtUnifiedTrendConfidence
                );

        txtUnifiedWeightedScore =
                findViewById(
                        R.id.txtUnifiedWeightedScore
                );

        txtUnifiedConfidenceUpdatedAt =
                findViewById(
                        R.id.txtUnifiedConfidenceUpdatedAt
                );

        initializeProgressSegments();
        organizeScreenForDailyUse();
    }

    /**
     * XML'deki 10 ilerleme segmentini bir diziye bağlar.
     *
     * Böylece her segment için ayrı ayrı kod yazmak yerine
     * döngüyle aktif veya pasif görünüm uygulanabilir.
     */
    private void initializeProgressSegments() {

        progressSegments[0] =
                findViewById(R.id.segmentAIProgress1);

        progressSegments[1] =
                findViewById(R.id.segmentAIProgress2);

        progressSegments[2] =
                findViewById(R.id.segmentAIProgress3);

        progressSegments[3] =
                findViewById(R.id.segmentAIProgress4);

        progressSegments[4] =
                findViewById(R.id.segmentAIProgress5);

    }

    /**
     * AI karar akışı RecyclerView yapısını hazırlar.
     */
    private void initializeRecyclerView() {

        recyclerAIDecisionFlow =
                findViewById(R.id.recyclerAIDecisionFlow);

        decisionStepAdapter =
                new DecisionStepAdapter();

        recyclerAIDecisionFlow.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerAIDecisionFlow.setAdapter(
                decisionStepAdapter
        );

        /*
         * RecyclerView, NestedScrollView içinde bulunduğu için
         * kendi kaydırma davranışı kapatılır.
         */
        recyclerAIDecisionFlow.setNestedScrollingEnabled(false);
    }

    /**
     * Activity için ViewModel nesnesini oluşturur.
     */
    private void initializeViewModel() {

        viewModel =
                new ViewModelProvider(this)
                        .get(MainViewModel.class);
    }

    /**
     * Buton tıklama işlemlerini tanımlar.
     */
    private void initializeListeners() {

        btnBack.setOnClickListener(
                view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
        );

        btnAIAdvancedDetails.setOnClickListener(
                view -> setAdvancedDetailsVisible(
                        !advancedDetailsVisible
                )
        );

        cardAIWateringControl.setOnClickListener(view ->
                startActivity(new Intent(this,
                        WateringControlActivity.class))
        );
        cardAISensorPoints.setOnClickListener(view ->
                startActivity(new Intent(this, SensorPointsActivity.class)));

        cardMoisturePrediction.setOnTouchListener(
                (view, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        predictionSwipeStartX = event.getX();
                        predictionSwipeStartY = event.getY();
                        predictionHorizontalSwipe = false;
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        float distance = event.getX() - predictionSwipeStartX;
                        float verticalDistance = event.getY() - predictionSwipeStartY;
                        float touchSlop = ViewConfiguration.get(this)
                                .getScaledTouchSlop();

                        if (!predictionHorizontalSwipe
                                && Math.abs(verticalDistance) > touchSlop
                                && Math.abs(verticalDistance) > Math.abs(distance)) {
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
                        }

                        if (Math.abs(distance) > touchSlop
                                && Math.abs(distance) > Math.abs(verticalDistance)) {
                            predictionHorizontalSwipe = true;
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        float distance = event.getX() - predictionSwipeStartX;
                        if (predictionHorizontalSwipe && Math.abs(distance) >= 44f) {
                            movePredictionZone(distance < 0 ? 1 : -1);
                            view.performClick();
                            return true;
                        }
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return false;
                    }
                    return true;
                }
        );
    }

    /**
     * Firebase verilerini taşıyan LiveData nesnelerini gözlemler.
     */
    private void organizeScreenForDailyUse() {
        ViewGroup parent = (ViewGroup) cardAIDecision.getParent();

        View[] movableViews = {
                cardAIZoneSummary,
                cardAIReasons,
                cardAIDecisionFlow,
                cardAIProgress,
                cardMoisturePrediction,
                cardPredictionAccuracy,
                cardUnifiedConfidence,
                cardPredictionValidation,
                cardAINextStep,
                cardAIWeatherGuidance,
                cardAIAdaptiveRecommendation,
                cardAIWateringControl,
                cardAISensorPoints,
                cardAITechnicalSummary,
                btnAIAdvancedDetails
        };

        for (View view : movableViews) {
            parent.removeView(view);
        }

        int insertIndex = parent.indexOfChild(cardAIDecision) + 1;
        parent.addView(cardAIZoneSummary, insertIndex++);
        parent.addView(cardAINextStep, insertIndex++);
        parent.addView(cardAIWeatherGuidance, insertIndex++);
        parent.addView(cardAIAdaptiveRecommendation, insertIndex++);
        parent.addView(cardAIProgress, insertIndex++);
        parent.addView(btnAIAdvancedDetails, insertIndex++);

        parent.addView(cardAIWateringControl, insertIndex++);
        parent.addView(cardAISensorPoints, insertIndex++);
        parent.addView(cardAIReasons, insertIndex++);
        parent.addView(cardAIDecisionFlow, insertIndex++);
        parent.addView(cardMoisturePrediction, insertIndex++);
        parent.addView(cardPredictionAccuracy, insertIndex++);
        parent.addView(cardUnifiedConfidence, insertIndex++);
        parent.addView(cardPredictionValidation, insertIndex++);
        parent.addView(cardAITechnicalSummary, insertIndex);

        setAdvancedDetailsVisible(false);
    }

    private void setAdvancedDetailsVisible(
            boolean visible
    ) {
        advancedDetailsVisible = visible;

        View[] technicalCards = {
                cardAIWateringControl,
                cardAISensorPoints,
                cardAIReasons,
                cardAIDecisionFlow,
                cardMoisturePrediction,
                cardPredictionAccuracy,
                cardUnifiedConfidence,
                cardPredictionValidation,
                cardAITechnicalSummary
        };

        for (View card : technicalCards) {
            card.setVisibility(
                    visible ? View.VISIBLE : View.GONE
            );
        }

        btnAIAdvancedDetails.setText(
                visible
                        ? "Teknik detayları gizle"
                        : "Asistan detaylarını gör"
        );
    }

    private void observeViewModel() {

        viewModel.getAIDecision().observe(
                this,
                this::renderAIDecision
        );

        viewModel.getAdaptiveRecommendation().observe(
                this,
                this::renderAdaptiveRecommendation
        );

        viewModel.getAIExplanation().observe(
                this,
                this::renderAIExplanation
        );

        viewModel.getPredictionValidationStatus().observe(
                this,
                this::renderPredictionValidationStatus
        );

        viewModel.getMoisturePrediction().observe(
                this,
                this::renderMoisturePrediction
        );

        viewModel.getPredictionAccuracy().observe(
                this,
                this::renderPredictionAccuracy
        );

        viewModel.getUnifiedConfidence().observe(
                this,
                this::renderUnifiedConfidence
        );

        viewModel.getSoilLearningProfile().observe(
                this,
                this::renderSoilLearningProfile
        );

        viewModel.getGardenZones().observe(
                this,
                this::renderZoneDecisionSummary
        );

        viewModel.getWeatherForecast().observe(
                this,
                forecast -> {
                    latestWeatherForecast = forecast;
                    renderWeatherGuidance();
                }
        );
    }

    private void renderZoneDecisionSummary(
            List<GardenZone> zones
    ) {
        updatePredictionZones(zones);
        layoutAIZoneSummary.removeAllViews();

        if (zones == null || zones.isEmpty()) {
            addZoneDecisionRow(
                    getString(R.string.symbol_plant),
                    getString(R.string.ai_zone_waiting),
                    R.color.textSecondary
            );
            return;
        }

        for (GardenZone zone : zones) {
            String emoji = zone.getEmoji() == null
                    ? getString(R.string.symbol_plant)
                    : zone.getEmoji();
            String name = zone.getName() == null
                    ? zone.getZone_id()
                    : zone.getName();
            ZoneIrrigationStatus status =
                    zone.getIrrigation_status();

            String detail;
            int detailColor = R.color.textSecondary;

            if (!zone.isIrrigation_enabled()) {
                detail = getString(R.string.ai_zone_disabled);
            } else if (!isZoneFresh(zone)) {
                detail = getString(R.string.ai_zone_waiting);
            } else if (
                    status != null
                            && status.isWatering_active()
            ) {
                detail = getString(R.string.ai_zone_watering);
                detailColor = R.color.info;
            } else if (
                    status != null
                            && status.isCooldown_active()
            ) {
                detail = getString(
                        R.string.ai_zone_cooldown,
                        formatZoneDuration(
                                status.getCooldown_remaining()
                        )
                );
                detailColor = R.color.warning;
            } else if (
                    status != null
                            && status.getQueue_position() > 0
            ) {
                detail = getString(
                        R.string.ai_zone_queued,
                        status.getQueue_position()
                );
                detailColor = R.color.accentOrange;
            } else {
                detail = formatZoneDecision(zone, status);
                if (
                        status != null
                                && status.getMoisture_deficit() > 0
                ) {
                    detailColor = R.color.warning;
                } else {
                    detailColor = R.color.online;
                }
            }

            addZoneDecisionRow(
                    emoji + " " + name,
                    detail,
                    detailColor
            );
        }

        renderWeatherGuidance();
    }

    /**
     * Hava verisini sulama için anlaşılır bir öneriye dönüştürür.
     * Bu bölüm yalnızca kullanıcıya bilgi verir; otomatik sulama
     * kararını veya pompa komutlarını değiştirmez.
     */
    private void renderWeatherGuidance() {
        if (latestWeatherForecast == null) {
            cardAIWeatherGuidance.setVisibility(View.GONE);
            return;
        }

        Double temperature = latestWeatherForecast.getTomorrowTemperatureMax();
        Double rain = latestWeatherForecast.getTomorrowRainProbability();
        Double wind = latestWeatherForecast.getTomorrowWindMax();
        int dryZoneCount = 0;
        for (GardenZone zone : predictionZones) {
            if (zone.isIrrigation_enabled()
                    && zone.hasSensorData()
                    && zone.getMoisture() <= zone.getMoisture_limit()) {
                dryZoneCount++;
            }
        }

        String weatherLine = "Yarın"
                + (temperature == null ? " sıcaklık verisi yok" : " " + Math.round(temperature) + "°C")
                + (rain == null ? "" : " · yağış %" + Math.round(rain))
                + (wind == null ? "" : " · rüzgâr " + Math.round(wind) + " km/sa");

        if (rain != null && rain >= 60d) {
            txtAIWeatherGuidanceTitle.setText("Yağış ihtimali yüksek");
            txtAIWeatherGuidanceSummary.setText(
                    weatherLine + ". Sulama gereksinimi oluşursa uygulamadan önce sabah yeniden kontrol edin."
            );
            txtAIWeatherGuidanceSafety.setText(
                    "Güvenlik: Sistem sulamayı otomatik iptal etmez; nem, vana ve pompa korumaları çalışmaya devam eder."
            );
        } else if (temperature != null && temperature >= 35d) {
            txtAIWeatherGuidanceTitle.setText("Sıcaklık stresi riski");
            txtAIWeatherGuidanceSummary.setText(
                    weatherLine + ". "
                            + (dryZoneCount > 0
                            ? dryZoneCount + " bölge nem sınırında; sabah erken kısa sulama daha uygun olabilir."
                            : "Bölgelerin nemi şu anda yeterli; sensör değerlerini izleyin.")
            );
            txtAIWeatherGuidanceSafety.setText(
                    "Güvenlik: Öneri niteliğindedir. Otomatik karar nem sensörü, bekleme süresi ve güvenlik kurallarına göre verilir."
            );
        } else if (wind != null && wind >= 25d) {
            txtAIWeatherGuidanceTitle.setText("Rüzgâr etkisi izleniyor");
            txtAIWeatherGuidanceSummary.setText(
                    weatherLine + ". Yüksek rüzgâr yüzeyden su kaybını artırabilir; nem sınırındaki bölgeleri kontrol edin."
            );
            txtAIWeatherGuidanceSafety.setText(
                    "Güvenlik: Pompa ve vana davranışı değişmez; bu bilgi yalnızca sulama zamanını planlamaya yardım eder."
            );
        } else {
            txtAIWeatherGuidanceTitle.setText("Hava sulama için uygun görünüyor");
            txtAIWeatherGuidanceSummary.setText(
                    weatherLine + ". "
                            + (dryZoneCount > 0
                            ? dryZoneCount + " bölge nem sınırında; asistanın bölgesel önerisini takip edin."
                            : "Belirgin yağış veya sıcaklık riski yok.")
            );
            txtAIWeatherGuidanceSafety.setText(
                    "Güvenlik: Sulama yalnızca nem ihtiyacı ve mevcut pompa-vana korumaları uygunsa yapılır."
            );
        }

        cardAIWeatherGuidance.setVisibility(View.VISIBLE);
    }

    private String formatZoneDecision(
            GardenZone zone,
            ZoneIrrigationStatus status
    ) {
        if (status == null) {
            return getString(R.string.ai_zone_learning);
        }

        String reason = status.getDecision_reason();
        if ("MOISTURE_SUFFICIENT".equals(reason)) {
            return getString(
                    R.string.ai_zone_moisture_sufficient,
                    zone.getMoisture()
            );
        }
        if ("MOISTURE_BELOW_LIMIT".equals(reason)) {
            return getString(
                    R.string.ai_zone_moisture_low,
                    status.getMoisture_deficit()
            );
        }
        if ("WAITING_FOR_MOISTURE_RECOVERY".equals(reason)) {
            return getString(R.string.ai_zone_recovery_waiting);
        }
        if ("WEATHER_RAIN_DELAY".equals(reason)) {
            return "Yağış beklendiği için kısa süreli ertelendi";
        }
        if ("WEATHER_WIND_DELAY".equals(reason)) {
            return "Yüksek rüzgâr nedeniyle kısa süreli ertelendi";
        }
        if ("SENSOR_UNSTABLE".equals(reason)) {
            return getString(R.string.ai_zone_unstable);
        }
        return getString(R.string.ai_zone_learning);
    }

    private void addZoneDecisionRow(
            String title,
            String detail,
            int detailColor
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 12, 0, 12);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(
                ContextCompat.getColor(this, R.color.textPrimary)
        );
        titleView.setTextSize(15f);
        titleView.setTypeface(
                titleView.getTypeface(),
                android.graphics.Typeface.BOLD
        );

        TextView detailView = new TextView(this);
        detailView.setText(detail);
        detailView.setTextColor(
                ContextCompat.getColor(this, detailColor)
        );
        detailView.setTextSize(13f);
        detailView.setPadding(0, 3, 0, 0);

        row.addView(titleView);
        row.addView(detailView);
        layoutAIZoneSummary.addView(row);
    }

    private boolean isZoneFresh(GardenZone zone) {
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

    private String formatZoneDuration(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        if (safeSeconds < 60) {
            return safeSeconds + " sn";
        }
        int minutes = safeSeconds / 60;
        int remainingSeconds = safeSeconds % 60;
        if (remainingSeconds == 0) {
            return minutes + " dk";
        }
        return minutes + " dk " + remainingSeconds + " sn";
    }

    private void renderSoilLearningProfile(
            SoilLearningProfile profile
    ) {

        if (profile == null) {
            return;
        }


        txtAILearningSensorCount.setText(
                profile.getSensor_history_count()
                        + " / "
                        + (
                        profile.getSensor_history_count()
                                +
                                profile.getRemaining_sensor_samples()
                )
        );


        txtAILearningWateringCount.setText(
                profile.getWatering_count_analyzed()
                        + " / "
                        + (
                        profile.getWatering_count_analyzed()
                                +
                                profile.getRemaining_auto_waterings()
                )
        );
        String sensorStatus =
                getLearningStatus(
                        profile.getSensor_history_count(),
                        profile.getSensor_history_count()
                                +
                                profile.getRemaining_sensor_samples()
                );


        String wateringStatus =
                getLearningStatus(
                        profile.getWatering_count_analyzed(),
                        profile.getWatering_count_analyzed()
                                +
                                profile.getRemaining_auto_waterings()
                );

        learningDataCollectionCompleted =
                "TAMAMLANDI".equals(sensorStatus)
                        && "TAMAMLANDI".equals(wateringStatus);
        profileLearningStage = profile.getLearning_stage();
        updateLearningStage(currentLearningProgress);

        renderLearningItemStatus(
                txtAILearningSensorStatus,
                sensorStatus
        );

        renderLearningItemStatus(
                txtAILearningWateringStatus,
                wateringStatus
        );

        Log.d(
                TAG,
                "AI Learning Sensor="
                        + sensorStatus
                        + " Watering="
                        + wateringStatus
        );
        // -------------------------------------------------
        // AI Learning progress hesapları
        // -------------------------------------------------

        int sensorCompleted =
                profile.getSensor_history_count();

        int sensorTotal =
                profile.getSensor_history_count()
                        +
                        profile.getRemaining_sensor_samples();


        int sensorProgress = 0;

        if (sensorTotal > 0) {

            sensorProgress =
                    (sensorCompleted * 100)
                            /
                            sensorTotal;
        }


        progressAILearningSensor.setProgressCompat(
                sensorProgress,
                true
        );


        // -------------------------------------------------

        int wateringCompleted =
                profile.getWatering_count_analyzed();

        int wateringTotal =
                profile.getWatering_count_analyzed()
                        +
                        profile.getRemaining_auto_waterings();


        int wateringProgress = 0;

        if (wateringTotal > 0) {

            wateringProgress =
                    (wateringCompleted * 100)
                            /
                            wateringTotal;
        }


        progressAILearningWatering.setProgressCompat(
                wateringProgress,
                true
        );

        txtAILearningStage.setText(
                "Öğrenme Aşaması "
                        + profile.getLearning_stage()
                        + " / 5"
        );

        txtAILearningDescription.setText(
                profile.getNext_milestone_text()
        );

        double retention =
                profile.getEstimated_water_retention_minutes();


        if (retention > 0) {

            if (retention >= 60) {

                double hours =
                        retention / 60.0;

                txtAISoilRetention.setText(
                        String.format(
                                Locale.getDefault(),
                                "%.1f saat",
                                hours
                        )
                );

            } else {

                txtAISoilRetention.setText(
                        String.format(
                                Locale.getDefault(),
                                "%.0f dk",
                                retention
                        )
                );
            }

        }
        else {

            txtAISoilRetention.setText(
                    "Bekleniyor"
            );

        }


        double dryingRate =
                profile.getAverage_drying_rate_per_minute();


        if (dryingRate > 0) {

            txtAIAverageDrying.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.3f %%/dk",
                            dryingRate
                    )
            );

            txtAIDryingStatus.setText(
                    "Hızlı kuruma"
            );

        }
        else if (dryingRate < 0) {

            txtAIAverageDrying.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.3f %%/dk",
                            dryingRate
                    )
            );

            txtAIDryingStatus.setText(
                    "Nem artıyor"
            );

        }
        else {

            txtAIAverageDrying.setText(
                    "0.000 %/dk"
            );

            txtAIDryingStatus.setText(
                    "Stabil"
            );
        }


        double efficiency =
                profile.getIrrigation_efficiency();


        txtAIWateringEfficiency.setText(
                formatEfficiency(
                        efficiency
                )
        );


        String efficiencyStatus =
                formatEfficiencyStatus(
                        efficiency
                );


        txtAIWateringEfficiencyStatus.setText(
                efficiencyStatus
        );


        applyEfficiencyStatusStyle(
                efficiencyStatus
        );

    }

    private String getLearningStatus(
            int completed,
            int total
    ) {

        if (total <= 0) {
            return "BEKLENİYOR";
        }


        if (completed >= total) {

            return "TAMAMLANDI";

        }


        if (completed > 0) {

            return "ÖĞRENİYOR";

        }


        return "BAŞLAMADI";
    }

    private void renderLearningItemStatus(
            TextView statusView,
            String status
    ) {
        statusView.setText(status);

        int color = "TAMAMLANDI".equals(status)
                ? R.color.primary
                : R.color.textSecondary;
        statusView.setTextColor(ContextCompat.getColor(this, color));
    }

    private String formatEfficiency(
            double efficiency
    ) {

        if (
                Double.isNaN(efficiency)
                        || Double.isInfinite(efficiency)
                        || efficiency <= 0
        ) {
            return "—";
        }


        return String.format(
                Locale.getDefault(),
                "%.3f %%/sn",
                efficiency
        );
    }
    private void renderUnifiedConfidence(
            UnifiedConfidence confidence
    ) {

        if (confidence == null) {
            renderUnifiedConfidenceEmpty();
            return;
        }

        String status =
                safeText(
                        confidence.getStatus(),
                        "INSUFFICIENT_DATA"
                )
                        .trim()
                        .toUpperCase(Locale.ROOT);

        double overallConfidence =
                normalizePercent(
                        confidence.getOverall_confidence()
                );

        if ("INSUFFICIENT_DATA".equals(status)) {

            renderUnifiedConfidenceLearning(
                    confidence
            );

            return;
        }

        progressUnifiedConfidence.setVisibility(
                View.VISIBLE
        );

        txtUnifiedOverallConfidence.setText(
                formatPercent(
                        overallConfidence
                )
        );

        txtUnifiedConfidenceLevel.setText(
                formatUnifiedConfidenceLevel(
                        confidence.getConfidence_level()
                )
        );

        txtUnifiedSoilConfidence.setText(
                formatPercent(
                        confidence.getSoil_learning_confidence()
                )
        );

        txtUnifiedPredictionAccuracy.setText(
                formatPercent(
                        confidence.getPrediction_accuracy()
                )
        );

        txtUnifiedSensorConfidence.setText(
                formatPercent(
                        confidence.getSensor_confidence()
                )
        );

        txtUnifiedTrendConfidence.setText(
                formatPercent(
                        confidence.getTrend_confidence()
                )
        );

        txtUnifiedWeightedScore.setText(
                formatPercent(
                        confidence.getWeighted_score()
                )
        );

        txtUnifiedConfidenceUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        confidence.getGenerated_at()
                )
        );

        progressUnifiedConfidence.setProgressCompat(
                (int) Math.round(overallConfidence),
                true
        );

        applyUnifiedConfidenceStatusStyle(
                status,
                overallConfidence
        );
    }

    private void renderUnifiedConfidenceLearning(
            UnifiedConfidence confidence
    ) {

        txtUnifiedConfidenceStatus.setText(
                "ÖĞRENİYOR"
        );

        txtUnifiedOverallConfidence.setText(
                "—"
        );

        txtUnifiedConfidenceLevel.setText(
                "Veri bekleniyor"
        );

        txtUnifiedSoilConfidence.setText(
                "—"
        );

        txtUnifiedPredictionAccuracy.setText(
                "—"
        );

        txtUnifiedSensorConfidence.setText(
                formatPercent(
                        confidence.getSensor_confidence()
                )
        );

        txtUnifiedTrendConfidence.setText(
                formatPercent(
                        confidence.getTrend_confidence()
                )
        );

        txtUnifiedWeightedScore.setText(
                "—"
        );

        txtUnifiedConfidenceUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        confidence.getGenerated_at()
                )
        );

        progressUnifiedConfidence.setProgressCompat(
                0,
                false
        );

        progressUnifiedConfidence.setVisibility(
                View.INVISIBLE
        );

        applyUnifiedConfidenceWaitingStyle();
    }

    private void renderUnifiedConfidenceEmpty() {

        txtUnifiedConfidenceStatus.setText(
                "BEKLENİYOR"
        );

        txtUnifiedOverallConfidence.setText(
                "—"
        );

        txtUnifiedConfidenceLevel.setText(
                "—"
        );

        txtUnifiedSoilConfidence.setText(
                "—"
        );

        txtUnifiedPredictionAccuracy.setText(
                "—"
        );

        txtUnifiedSensorConfidence.setText(
                "—"
        );

        txtUnifiedTrendConfidence.setText(
                "—"
        );

        txtUnifiedWeightedScore.setText(
                "—"
        );

        txtUnifiedConfidenceUpdatedAt.setText(
                "Son güncelleme: —"
        );

        progressUnifiedConfidence.setProgressCompat(
                0,
                false
        );

        progressUnifiedConfidence.setVisibility(
                View.INVISIBLE
        );

        applyUnifiedConfidenceWaitingStyle();
    }

    private String formatUnifiedConfidenceLevel(
            String confidenceLevel
    ) {

        if (
                confidenceLevel == null
                        || confidenceLevel.trim().isEmpty()
        ) {
            return "Bekleniyor";
        }

        switch (
                confidenceLevel
                        .trim()
                        .toUpperCase(Locale.ROOT)
        ) {

            case "VERY_HIGH":
                return "Çok yüksek";

            case "HIGH":
                return "Yüksek";

            case "MEDIUM":
                return "Orta";

            case "LOW":
                return "Düşük";

            case "VERY_LOW":
                return "Çok düşük";

            case "INSUFFICIENT_DATA":
                return "Veri bekleniyor";

            default:
                return "Bekleniyor";
        }
    }

    private void applyUnifiedConfidenceStatusStyle(
            String status,
            double overallConfidence
    ) {

        if (
                "READY".equals(status)
                        && overallConfidence >= 70.0
        ) {

            txtUnifiedConfidenceStatus.setText(
                    "YÜKSEK"
            );

            applyUnifiedConfidenceReadyStyle();

        } else if (
                "READY".equals(status)
                        && overallConfidence >= 40.0
        ) {

            txtUnifiedConfidenceStatus.setText(
                    "ORTA"
            );

            applyUnifiedConfidenceMediumStyle();

        } else if (
                "READY".equals(status)
        ) {

            txtUnifiedConfidenceStatus.setText(
                    "DÜŞÜK"
            );

            applyUnifiedConfidenceLowStyle();

        } else if (
                "INSUFFICIENT_DATA".equals(status)
        ) {

            txtUnifiedConfidenceStatus.setText(
                    "ÖĞRENİYOR"
            );

            applyUnifiedConfidenceWaitingStyle();

        } else {

            txtUnifiedConfidenceStatus.setText(
                    "BEKLENİYOR"
            );

            applyUnifiedConfidenceWaitingStyle();
        }
    }

    private void applyUnifiedConfidenceReadyStyle() {

        int online =
                ContextCompat.getColor(
                        this,
                        R.color.online
                );

        int onlineBackground =
                ContextCompat.getColor(
                        this,
                        R.color.onlineBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setCardBackgroundColor(
                        onlineBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setStrokeColor(
                        online
                );

        txtUnifiedConfidenceStatus.setTextColor(
                online
        );
    }

    private void applyUnifiedConfidenceMediumStyle() {

        int warning =
                ContextCompat.getColor(
                        this,
                        R.color.warning
                );

        int warningBackground =
                ContextCompat.getColor(
                        this,
                        R.color.warningBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setCardBackgroundColor(
                        warningBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setStrokeColor(
                        warning
                );

        txtUnifiedConfidenceStatus.setTextColor(
                warning
        );
    }

    private void applyUnifiedConfidenceLowStyle() {

        int offline =
                ContextCompat.getColor(
                        this,
                        R.color.offline
                );

        int offlineBackground =
                ContextCompat.getColor(
                        this,
                        R.color.offlineBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setCardBackgroundColor(
                        offlineBackground
                );

        cardUnifiedConfidenceStatusBadge
                .setStrokeColor(
                        offline
                );

        txtUnifiedConfidenceStatus.setTextColor(
                offline
        );
    }

    private void applyUnifiedConfidenceWaitingStyle() {

        int primary =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int primaryLight =
                ContextCompat.getColor(
                        this,
                        R.color.primaryLight
                );

        cardUnifiedConfidenceStatusBadge
                .setCardBackgroundColor(
                        primaryLight
                );

        cardUnifiedConfidenceStatusBadge
                .setStrokeColor(
                        primary
                );

        txtUnifiedConfidenceStatus.setTextColor(
                primary
        );
    }


    /**
     * Firebase'den gelen AI karar bilgisini ekrana yansıtır.
     */
    private void renderAIDecision(
            AIDecision decision
    ) {

        if (decision == null) {
            return;
        }

        txtAIDecisionTitle.setText(
                safeText(
                        decision.getDecisionTitle(),
                        "AI kararı hazırlanıyor"
                )
        );

        String sensorId = safeText(
                decision.getAnalysisSensorId(),
                "ana sensör"
        );
        txtAIAnalysisScope.setText(
                "Bu analiz " + sensorId + " verisine dayanır"
        );

        txtAIConfidence.setText(
                formatConfidence(
                        decision.getConfidenceLevel(),
                        decision.getConfidence()
                )
        );

        txtAISoilClassification.setText(
                formatSoilClassification(
                        decision.getSoilClassification()
                )
        );

        txtAITrendClassification.setText(
                formatTrendClassification(
                        decision.getTrendClassification()
                )
        );

        txtAIUpdatedAt.setText(
                formatUpdatedAt(
                        decision.getUpdatedAt()
                )
        );

        applyDecisionStyle(
                decision.getDecisionCode(),
                decision.getSeverity()
        );
    }

    private void renderAdaptiveRecommendation(
            AdaptiveRecommendation recommendation
    ) {
        if (
                recommendation == null
                        || !recommendation.isShouldApply()
        ) {
            cardAIAdaptiveRecommendation.setVisibility(View.GONE);
            return;
        }

        boolean increase = "INCREASE_PUMP_DURATION".equals(
                recommendation.getRecommendationType()
        );

        txtAIAdaptiveRecommendationTitle.setText(
                increase
                        ? "Pompa süresi artırılabilir"
                        : "Pompa süresi azaltılabilir"
        );

        txtAIAdaptiveRecommendationDetail.setText(
                "Öneri: "
                        + formatZoneDuration(
                        (int) recommendation.getCurrentPumpDurationSeconds()
                )
                        + " → "
                        + formatZoneDuration(
                        (int) recommendation.getRecommendedPumpDurationSeconds()
                )
                        + " · "
                        + recommendation.getWateringCountAnalyzed()
                        + " sulama kaydı incelendi. "
                        + "Ayarlar otomatik değiştirilmez."
        );

        cardAIAdaptiveRecommendation.setVisibility(View.VISIBLE);
    }

    /**
     * Firebase'den gelen AI açıklama bilgisini ekrana yansıtır.
     */
    private void renderAIExplanation(
            AIExplanation explanation
    ) {

        if (explanation == null) {
            return;
        }

        txtAIDecisionSummary.setText(
                safeText(
                        explanation.getSummary(),
                        "AI açıklaması hazırlanıyor."
                )
        );

        List<String> reasonLines =
                explanation.getReasonLines();

        renderReasonLine(
                txtAIReasonOne,
                reasonLines,
                0
        );

        renderReasonLine(
                txtAIReasonTwo,
                reasonLines,
                1
        );

        renderReasonLine(
                txtAIReasonThree,
                reasonLines,
                2
        );

        int targetProgress =
                clampProgress(
                        explanation.getProgressPercent()
                );

        animateLearningProgress(
                targetProgress
        );

        txtAINextStep.setText(
                safeText(
                        explanation.getNextStep(),
                        "Yeni veriler bekleniyor."
                )
        );

        if (decisionStepAdapter != null) {

            decisionStepAdapter.submitList(
                    DecisionFlowFactory.create(explanation)
            );
        }
    }

    /**
     * AI nedenleri bölümündeki satırı güvenli biçimde gösterir.
     *
     * İlgili indeks boşsa TextView gizlenir.
     */
    private void renderReasonLine(
            TextView textView,
            List<String> reasonLines,
            int index
    ) {

        if (
                reasonLines == null
                        || index < 0
                        || index >= reasonLines.size()
        ) {

            textView.setVisibility(View.GONE);
            return;
        }

        String value =
                reasonLines.get(index);

        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            textView.setVisibility(View.GONE);
            return;
        }

        textView.setVisibility(View.VISIBLE);
        textView.setText(value.trim());
    }

    /**
     * Öğrenme yüzdesini eski değerden yeni değere animasyonlu şekilde taşır.
     */
    private void animateLearningProgress(
            int targetProgress
    ) {

        targetProgress =
                clampProgress(targetProgress);

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }

        if (currentLearningProgress == targetProgress) {

            renderLearningProgressImmediately(
                    targetProgress
            );

            return;
        }

        progressAnimator =
                ValueAnimator.ofInt(
                        currentLearningProgress,
                        targetProgress
                );

        progressAnimator.setDuration(500L);

        progressAnimator.addUpdateListener(
                animation -> {

                    int animatedProgress =
                            (int) animation.getAnimatedValue();

                    renderLearningProgressImmediately(
                            animatedProgress
                    );
                }
        );

        progressAnimator.start();
    }

    /**
     * Yüzde, segmentler, öğrenme aşaması ve açıklama metnini
     * verilen yüzdeye göre aynı anda günceller.
     */
    private void renderLearningProgressImmediately(
            int progress
    ) {

        int safeProgress =
                clampProgress(progress);

        currentLearningProgress =
                safeProgress;

        txtAIProgressPercent.setText(
                "%" + safeProgress
        );

        updateProgressSegments(
                safeProgress
        );

        updateLearningStage(
                safeProgress
        );
    }

    /**
     * 10 parçalı ilerleme göstergesini yüzdeye göre doldurur.
     *
     * Örnek:
     * %40 = 4 aktif segment
     * %75 = 8 aktif segment
     */
    private void updateProgressSegments(
            int progress
    ) {

        int activeSegmentCount;

        if (progress <= 0) {

            activeSegmentCount = 0;

        } else {

            activeSegmentCount =
                    (int) Math.ceil(
                            progress / 20.0
                    );
        }

        activeSegmentCount =
                Math.max(
                        0,
                        Math.min(
                                activeSegmentCount,
                                progressSegments.length
                        )
                );

        for (
                int index = 0;
                index < progressSegments.length;
                index++
        ) {

            View segment =
                    progressSegments[index];

            if (segment == null) {
                continue;
            }

            if (index < activeSegmentCount) {

                segment.setBackgroundResource(
                        R.drawable.bg_ai_progress_segment_active
                );

            } else {

                segment.setBackgroundResource(
                        R.drawable.bg_ai_progress_segment_inactive
                );
            }
        }
    }

    /**
     * Öğrenme yüzdesine göre aşama ve açıklama metnini hesaplar.
     *
     * %1–20   = Aşama 1
     * %21–40  = Aşama 2
     * %41–60  = Aşama 3
     * %61–80  = Aşama 4
     * %81–100 = Aşama 5
     */
    private void updateLearningStage(
            int progress
    ) {

        int learningStage;
        String description;

        if (profileLearningStage > 0) {
            learningStage = profileLearningStage;
            description = getProfileLearningDescription(
                    profileLearningStage,
                    learningDataCollectionCompleted,
                    progress
            );

        } else if (progress <= 20) {

            learningStage = 1;
            description = "Yeni sensör verileri toplanıyor";

        } else if (progress <= 40) {

            learningStage = 2;
            description = "Sensör davranışları öğreniliyor";

        } else if (progress <= 60) {

            learningStage = 3;
            description = "Toprak nem modeli oluşturuluyor";

        } else if (progress <= 80) {

            learningStage = 4;
            description = "Sulama geçmişi analiz ediliyor";

        } else {

            learningStage = 5;

            if (progress >= 100) {

                description = "Öğrenme aşaması tamamlandı";

            } else {

                description = "AI modeli doğrulanıyor";
            }
        }

        txtAILearningStage.setText(
                "Öğrenme Aşaması "
                        + learningStage
                        + " / 5"
        );

        txtAILearningDescription.setText(
                description
        );
    }

    /**
     * İlerleme değerini 0–100 sınırında tutar.
     */
    private String getProfileLearningDescription(
            int stage,
            boolean basicRecordsCompleted,
            int progress
    ) {
        switch (stage) {
            case 1:
                return "Yeni sens\u00F6r verileri toplan\u0131yor";
            case 2:
                return basicRecordsCompleted
                        ? "\u00D6l\u00E7\u00FCm ve sulama kay\u0131tlar\u0131 tamamland\u0131 \u00B7 "
                                + "Nem de\u011Fi\u015Fimi zaman i\u00E7inde do\u011Frulan\u0131yor"
                        : "Topra\u011F\u0131n nem de\u011Fi\u015Fimi zaman i\u00E7inde g\u00F6zlemleniyor";
            case 3:
                return "Otomatik sulama sonu\u00E7lar\u0131 toplan\u0131yor";
            case 4:
                return "Toprak davran\u0131\u015F\u0131 analiz ediliyor";
            case 5:
                return progress >= 100
                        ? "Toprak \u00F6\u011Frenme profili haz\u0131r"
                        : "\u00D6\u011Frenme g\u00FCveni art\u0131r\u0131l\u0131yor";
            default:
                return "\u00D6\u011Frenme s\u00FCreci devam ediyor";
        }
    }

    private int clampProgress(
            double progress
    ) {

        return (int) Math.max(
                0,
                Math.min(
                        Math.round(progress),
                        100
                )
        );
    }

    /**
     * Güven seviyesini kullanıcı dostu Türkçe metne dönüştürür.
     */
    private String formatConfidence(
            String confidenceLevel,
            double confidence
    ) {

        String level;

        if ("HIGH".equals(confidenceLevel)) {

            level = "YÜKSEK";

        } else if ("MEDIUM".equals(confidenceLevel)) {

            level = "ORTA";

        } else {

            level = "DÜŞÜK";
        }

        long percent =
                Math.round(
                        confidence * 100
                );

        /*
         * Backend bazı durumlarda güven değerini doğrudan yüzde
         * olarak gönderebilir. Böyle bir durumda ikinci kez
         * 100 ile çarpılmasını engeller.
         */
        if (confidence > 1.0) {

            percent =
                    Math.round(confidence);
        }

        percent =
                Math.max(
                        0,
                        Math.min(
                                percent,
                                100
                        )
                );

        return level
                + " · %"
                + percent;
    }

    /**
     * Backend toprak sınıflandırmasını Türkçeleştirir.
     */
    private String formatSoilClassification(
            String classification
    ) {

        if (classification == null) {
            return "Bilinmiyor";
        }

        switch (classification) {

            case "HIGH_WATER_RETENTION":
                return "Suyu iyi tutuyor";

            case "SLOW_DRYING":
                return "Yavaş kuruyor";

            case "BALANCED":
                return "Dengeli";

            case "FAST_DRYING":
                return "Hızlı kuruyor";

            case "VERY_FAST_DRYING":
                return "Çok hızlı kuruyor";

            default:
                return "Bilinmiyor";
        }
    }

    /**
     * Backend nem trendi bilgisini Türkçeleştirir.
     */
    private String formatTrendClassification(
            String classification
    ) {

        if (classification == null) {
            return "Bekleniyor";
        }

        switch (classification) {

            case "STABLE":
                return "Kararlı";

            case "RISING":
                return "Yükseliyor";

            case "SLOW_DRYING":
                return "Yavaş kuruma";

            case "NORMAL_DRYING":
                return "Normal kuruma";

            case "FAST_DRYING":
                return "Hızlı kuruma";

            case "VERY_FAST_DRYING":
                return "Çok hızlı kuruma";

            case "INSUFFICIENT_DATA":
                return "Veri toplanıyor";

            default:
                return "Bekleniyor";
        }
    }

    /**
     * ISO tarih biçiminden saat bilgisini alır.
     *
     * Örnek:
     * 2026-07-19T18:45:30
     * sonuç: 18:45
     */
    private String formatUpdatedAt(
            String updatedAt
    ) {

        if (
                updatedAt == null
                        || updatedAt.trim().isEmpty()
        ) {

            return "Bekleniyor";
        }

        try {

            int separatorIndex =
                    updatedAt.indexOf("T");

            if (
                    separatorIndex < 0
                            || updatedAt.length()
                            < separatorIndex + 6
            ) {

                return "Bekleniyor";
            }

            return updatedAt.substring(
                    separatorIndex + 1,
                    separatorIndex + 6
            );

        } catch (RuntimeException exception) {

            return "Son güncelleme bekleniyor";
        }
    }

    /**
     * AI kararına göre sağ üst durum rozetinin rengini ve metnini ayarlar.
     */
    private void applyDecisionStyle(
            String decisionCode,
            String severity
    ) {

        int textColorRes;
        int backgroundColorRes;
        String badgeText;

        if ("LEARNING".equals(decisionCode)) {

            textColorRes = R.color.primary;
            backgroundColorRes = R.color.primaryLight;
            badgeText = "ÖĞRENİYOR";

        } else if ("SENSOR_UNSTABLE".equals(decisionCode)) {

            textColorRes = R.color.warning;
            backgroundColorRes = R.color.warningBackground;
            badgeText = "SENSÖR";

        } else if ("WATERING_RECOMMENDED".equals(decisionCode)) {

            textColorRes = R.color.warning;
            backgroundColorRes = R.color.warningBackground;
            badgeText = "SULAMA";

        } else if ("NO_ACTION_REQUIRED".equals(decisionCode)) {

            textColorRes = R.color.online;
            backgroundColorRes = R.color.onlineBackground;
            badgeText = "UYGUN";

        } else if ("SYSTEM_DISABLED".equals(decisionCode)) {

            textColorRes = R.color.offline;
            backgroundColorRes = R.color.offlineBackground;
            badgeText = "KAPALI";

        } else if ("MANUAL_MODE".equals(decisionCode)) {

            textColorRes = R.color.info;
            backgroundColorRes = R.color.infoBackground;
            badgeText = "MANUEL";

        } else if ("INCREASE_PUMP_DURATION".equals(decisionCode)) {

            textColorRes = R.color.warning;
            backgroundColorRes = R.color.warningBackground;
            badgeText = "ARTIR";

        } else if ("DECREASE_PUMP_DURATION".equals(decisionCode)) {

            textColorRes = R.color.info;
            backgroundColorRes = R.color.infoBackground;
            badgeText = "AZALT";

        } else if ("CRITICAL".equals(severity)) {

            textColorRes = R.color.offline;
            backgroundColorRes = R.color.offlineBackground;
            badgeText = "KRİTİK";

        } else {

            textColorRes = R.color.primary;
            backgroundColorRes = R.color.primaryLight;
            badgeText = "BİLGİ";
        }

        int textColor =
                ContextCompat.getColor(
                        this,
                        textColorRes
                );

        int backgroundColor =
                ContextCompat.getColor(
                        this,
                        backgroundColorRes
                );

        txtAISeverityBadge.setText(
                badgeText
        );

        txtAISeverityBadge.setTextColor(
                textColor
        );

        cardAISeverityBadge.setStrokeColor(
                textColor
        );

        cardAISeverityBadge.setCardBackgroundColor(
                backgroundColor
        );
    }

    /**
     * Kartların ekran açılırken sırayla görünmesini sağlar.
     */
    private void startEntranceAnimations() {

        View[] cards = {
                cardAIDecision,
                cardAIZoneSummary,
                cardAINextStep,
                cardAIProgress,
                btnAIAdvancedDetails
        };

        for (
                int index = 0;
                index < cards.length;
                index++
        ) {

            View card =
                    cards[index];

            if (card == null) {
                continue;
            }

            card.setAlpha(0f);
            card.setTranslationY(28f);

            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 90L)
                    .setDuration(320L)
                    .start();
        }
    }

    /**
     * Boş Firebase metinlerine karşı güvenli değer döndürür.
     */
    private String safeText(
            String value,
            String fallback
    ) {

        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            return fallback;
        }

        return value.trim();
    }

    private void updatePredictionZones(List<GardenZone> zones) {
        String selectedZoneId = predictionZones.isEmpty()
                ? ""
                : predictionZones.get(selectedPredictionZoneIndex).getZone_id();
        predictionZones.clear();

        if (zones != null) {
            predictionZones.addAll(zones);
            Collections.sort(predictionZones, Comparator.comparingInt(GardenZone::getOrder));
        }

        selectedPredictionZoneIndex = 0;
        for (int index = 0; index < predictionZones.size(); index++) {
            if (selectedZoneId.equals(predictionZones.get(index).getZone_id())) {
                selectedPredictionZoneIndex = index;
                break;
            }
        }
        renderSelectedMoisturePrediction();
    }

    private void movePredictionZone(int direction) {
        if (predictionZones.isEmpty()) {
            return;
        }
        selectedPredictionZoneIndex = (selectedPredictionZoneIndex + direction
                + predictionZones.size()) % predictionZones.size();
        renderSelectedMoisturePrediction();
    }

    private void renderSelectedMoisturePrediction() {
        if (predictionZones.isEmpty()) {
            txtMoisturePredictionTitle.setText("Nem Tahmini");
            txtMoisturePredictionZone.setText("BÃ¶lge verisi bekleniyor");
            renderMoisturePredictionData(latestMoisturePrediction);
            return;
        }

        GardenZone zone = predictionZones.get(selectedPredictionZoneIndex);
        String emoji = safeText(zone.getEmoji(), "ğŸŒ±");
        String name = safeText(zone.getName(), zone.getZone_id());
        txtMoisturePredictionTitle.setText("Nem Tahmini · " + emoji + " " + name);
        txtMoisturePredictionZone.setText((selectedPredictionZoneIndex + 1)
                + " / " + predictionZones.size() + " · Sağa/sola kaydırın");

        if (selectedPredictionZoneIndex == 0) {
            renderMoisturePredictionData(latestMoisturePrediction);
            return;
        }

        renderZonePredictionWaiting(zone);
    }

    private void renderZonePredictionWaiting(GardenZone zone) {
        txtMoisturePredictionStatus.setText("ÖĞRENİYOR");
        txtPredictionCurrentMoisture.setText(formatMoistureValue(zone.getMoisture()));
        txtPredictionMoistureLimit.setText(formatMoistureValue(zone.getMoisture_limit()));
        txtPredictionOneHour.setText("—");
        txtPredictionThreeHours.setText("—");
        txtPredictionSixHours.setText("—");
        txtPredictionTimeUntilLimit.setText("Bu bölge için veri hazırlanıyor");
        txtMoisturePredictionConfidence.setText("DÜŞÜK · %0");
        txtPredictionLimitReachedAt.setText("Tahmini sınır zamanı: —");
        txtMoisturePredictionUpdatedAt.setText("Bu bölgenin tahmini henüz oluşmadı");
        applyMoisturePredictionWaitingStyle();
    }

    private void renderMoisturePrediction(
            MoisturePrediction prediction
    ) {
        latestMoisturePrediction = prediction;
        renderSelectedMoisturePrediction();
    }

    private void renderMoisturePredictionData(
            MoisturePrediction prediction
    ) {

        if (prediction == null) {
            renderMoisturePredictionEmpty();
            return;
        }

        String predictionStatus =
                safeText(
                        prediction.getPrediction_status(),
                        "INSUFFICIENT_DATA"
                )
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if ("INSUFFICIENT_DATA".equals(predictionStatus)) {

            renderMoisturePredictionLearning(
                    prediction
            );

            return;
        }

        txtPredictionCurrentMoisture.setText(
                formatMoistureValue(
                        prediction.getCurrent_moisture()
                )
        );

        txtPredictionMoistureLimit.setText(
                formatMoistureValue(
                        prediction.getMoisture_limit()
                )
        );

        txtPredictionOneHour.setText(
                formatMoistureValue(
                        prediction.getPredicted_moisture_1_hour()
                )
        );

        txtPredictionThreeHours.setText(
                formatMoistureValue(
                        prediction.getPredicted_moisture_3_hours()
                )
        );

        txtPredictionSixHours.setText(
                formatMoistureValue(
                        prediction.getPredicted_moisture_6_hours()
                )
        );

        txtPredictionTimeUntilLimit.setText(
                formatMinutesUntilLimit(
                        prediction.getEstimated_minutes_until_limit()
                )
        );

        txtMoisturePredictionConfidence.setText(
                formatConfidence(
                        prediction.getConfidence_level(),
                        prediction.getConfidence()
                )
        );

        txtPredictionLimitReachedAt.setText(
                "Tahmini sınır zamanı: "
                        + formatPredictionDateTime(
                        prediction.getEstimated_limit_reached_at()
                )
        );

        txtMoisturePredictionUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        prediction.getGenerated_at()
                )
        );

        applyMoisturePredictionStatusStyle(
                predictionStatus
        );
    }

    private void renderPredictionAccuracy(
            PredictionAccuracy accuracy
    ) {

        if (accuracy == null) {
            renderPredictionAccuracyEmpty();
            return;
        }

        String status =
                safeText(
                        accuracy.getStatus(),
                        "INSUFFICIENT_DATA"
                )
                        .trim()
                        .toUpperCase(Locale.ROOT);

        long predictionCount =
                Math.max(
                        0,
                        accuracy.getPrediction_count()
                );

        long successfulPredictions =
                Math.max(
                        0,
                        accuracy.getSuccessful_predictions()
                );

        if (
                "INSUFFICIENT_DATA".equals(status)
                        || predictionCount == 0
        ) {

            renderPredictionAccuracyLearning(
                    accuracy
            );

            return;
        }

        double accuracyPercent =
                normalizePercent(
                        accuracy.getAccuracy_percent()
                );

        txtPredictionAccuracyPercent.setText(
                formatPercent(
                        accuracyPercent
                )
        );

        txtPredictionConfidenceMultiplier.setText(
                formatMultiplier(
                        accuracy.getConfidence_multiplier()
                )
        );

        txtPredictionCount.setText(
                String.valueOf(
                        predictionCount
                )
        );

        txtSuccessfulPredictions.setText(
                String.valueOf(
                        successfulPredictions
                )
        );

        txtPredictionAverageError.setText(
                formatErrorValue(
                        accuracy.getAverage_error()
                )
        );

        txtPredictionMinimumError.setText(
                formatErrorValue(
                        accuracy.getMinimum_error()
                )
        );

        txtPredictionMaximumError.setText(
                formatErrorValue(
                        accuracy.getMaximum_error()
                )
        );

        txtPredictionAccuracyUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        accuracy.getGenerated_at()
                )
        );

        progressPredictionAccuracy.setVisibility(
                View.VISIBLE
        );

        int progress =
                (int) Math.round(
                        accuracyPercent
                );

        progressPredictionAccuracy.setProgressCompat(
                progress,
                true
        );

        progressPredictionAccuracy.setVisibility(
                View.INVISIBLE
        );

        applyPredictionAccuracyStatusStyle(
                status,
                predictionCount
        );
    }

    private void renderPredictionAccuracyLearning(
            PredictionAccuracy accuracy
    ) {

        txtPredictionAccuracyStatus.setText(
                "ÖĞRENİYOR"
        );

        txtPredictionAccuracyPercent.setText(
                "—"
        );

        txtPredictionConfidenceMultiplier.setText(
                formatMultiplier(
                        accuracy.getConfidence_multiplier()
                )
        );

        txtPredictionCount.setText(
                String.valueOf(
                        Math.max(
                                0,
                                accuracy.getPrediction_count()
                        )
                )
        );

        txtSuccessfulPredictions.setText(
                String.valueOf(
                        Math.max(
                                0,
                                accuracy.getSuccessful_predictions()
                        )
                )
        );

        txtPredictionAverageError.setText(
                "—"
        );

        txtPredictionMinimumError.setText(
                "—"
        );

        txtPredictionMaximumError.setText(
                "—"
        );

        txtPredictionAccuracyUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        accuracy.getGenerated_at()
                )
        );

        progressPredictionAccuracy.setProgressCompat(
                0,
                false
        );

        progressPredictionAccuracy.setVisibility(
                View.INVISIBLE
        );
        applyPredictionAccuracyWaitingStyle();
    }
    private void renderPredictionAccuracyEmpty() {

        txtPredictionAccuracyStatus.setText(
                "BEKLENİYOR"
        );

        txtPredictionAccuracyPercent.setText(
                "—"
        );

        txtPredictionConfidenceMultiplier.setText(
                "—"
        );

        txtPredictionCount.setText(
                "0"
        );

        txtSuccessfulPredictions.setText(
                "0"
        );

        txtPredictionAverageError.setText(
                "—"
        );

        txtPredictionMinimumError.setText(
                "—"
        );

        txtPredictionMaximumError.setText(
                "—"
        );

        txtPredictionAccuracyUpdatedAt.setText(
                "Son güncelleme: —"
        );

        progressPredictionAccuracy.setProgressCompat(
                0,
                false
        );

        progressPredictionAccuracy.setVisibility(
                View.INVISIBLE
        );

        applyPredictionAccuracyWaitingStyle();
    }

    private double normalizePercent(
            double value
    ) {

        if (
                Double.isNaN(value)
                        || Double.isInfinite(value)
        ) {
            return 0.0;
        }

        double normalizedValue =
                value;

        if (
                normalizedValue >= 0.0
                        && normalizedValue <= 1.0
        ) {
            normalizedValue *= 100.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        normalizedValue,
                        100.0
                )
        );
    }

    private String formatPercent(
            double percent
    ) {

        return String.format(
                Locale.getDefault(),
                "%%%.1f",
                normalizePercent(percent)
        );
    }

    private String formatMinutes(
            double minutes
    ) {

        if (
                Double.isNaN(minutes)
                        || Double.isInfinite(minutes)
                        || minutes <= 0
        ) {
            return "—";
        }


        if (minutes >= 60) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f sa",
                    minutes / 60.0
            );

        }


        return String.format(
                Locale.getDefault(),
                "%.0f dk",
                minutes
        );
    }

    private String formatDryingRate(
            double rate
    ) {

        if (
                Double.isNaN(rate)
                        || Double.isInfinite(rate)
                        || rate <= 0
        ) {
            return "—";
        }


        return String.format(
                Locale.getDefault(),
                "%.3f/dk",
                rate
        );
    }

    private String formatEfficiencyStatus(
            double efficiency
    ) {

        if (efficiency <= 0) {

            return "Öğreniliyor";

        }


        if (efficiency < 0.02) {

            return "Düşük";

        }


        if (efficiency < 0.05) {

            return "Normal";

        }


        return "İyi";
    }

    private void applyEfficiencyStatusStyle(
            String status
    ) {

        if ("İyi".equals(status)) {

            txtAIWateringEfficiencyStatus
                    .setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    R.color.online
                            )
                    );


        } else if ("Normal".equals(status)) {

            txtAIWateringEfficiencyStatus
                    .setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    R.color.warning
                            )
                    );


        } else if ("Düşük".equals(status)) {

            txtAIWateringEfficiencyStatus
                    .setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    R.color.offline
                            )
                    );


        } else {

            txtAIWateringEfficiencyStatus
                    .setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    R.color.primary
                            )
                    );
        }
    }

    private String formatLearningValue(
            double value,
            String fallback
    ) {

        if (
                value <= 0
                        || Double.isNaN(value)
                        || Double.isInfinite(value)
        ) {
            return fallback;
        }

        return String.format(
                Locale.getDefault(),
                "%.2f",
                value
        );
    }

    private String formatErrorValue(
            double error
    ) {

        if (
                Double.isNaN(error)
                        || Double.isInfinite(error)
                        || error < 0
        ) {
            return "—";
        }

        return String.format(
                Locale.getDefault(),
                "%%%.2f",
                error
        );
    }

    private String formatMultiplier(
            double multiplier
    ) {

        if (
                Double.isNaN(multiplier)
                        || Double.isInfinite(multiplier)
                        || multiplier < 0
        ) {
            return "—";
        }

        return String.format(
                Locale.getDefault(),
                "×%.2f",
                multiplier
        );
    }

    private void applyPredictionAccuracyStatusStyle(
            String status,
            long predictionCount
    ) {

        if (
                "READY".equals(status)
                        && predictionCount > 0
        ) {

            txtPredictionAccuracyStatus.setText(
                    "HAZIR"
            );

            applyPredictionAccuracyReadyStyle();

        } else if (
                "INSUFFICIENT_DATA".equals(status)
                        || predictionCount == 0
        ) {

            txtPredictionAccuracyStatus.setText(
                    "ÖĞRENİYOR"
            );

            applyPredictionAccuracyWaitingStyle();

        } else {

            txtPredictionAccuracyStatus.setText(
                    "BEKLENİYOR"
            );

            applyPredictionAccuracyWaitingStyle();
        }
    }

    private void applyPredictionAccuracyReadyStyle() {

        int online =
                ContextCompat.getColor(
                        this,
                        R.color.online
                );

        int onlineBackground =
                ContextCompat.getColor(
                        this,
                        R.color.onlineBackground
                );

        cardPredictionAccuracyStatusBadge
                .setCardBackgroundColor(
                        onlineBackground
                );

        cardPredictionAccuracyStatusBadge
                .setStrokeColor(
                        online
                );

        txtPredictionAccuracyStatus.setTextColor(
                online
        );
    }

    private void applyPredictionAccuracyWaitingStyle() {

        int primary =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int primaryLight =
                ContextCompat.getColor(
                        this,
                        R.color.primaryLight
                );

        cardPredictionAccuracyStatusBadge
                .setCardBackgroundColor(
                        primaryLight
                );

        cardPredictionAccuracyStatusBadge
                .setStrokeColor(
                        primary
                );

        txtPredictionAccuracyStatus.setTextColor(
                primary
        );
    }



    private void renderMoisturePredictionLearning(
            MoisturePrediction prediction
    ) {

        txtMoisturePredictionStatus.setText(
                "ÖĞRENİYOR"
        );

        txtPredictionCurrentMoisture.setText(
                formatMoistureValue(
                        prediction.getCurrent_moisture()
                )
        );

        txtPredictionMoistureLimit.setText(
                formatMoistureValue(
                        prediction.getMoisture_limit()
                )
        );

        txtPredictionOneHour.setText("—");
        txtPredictionThreeHours.setText("—");
        txtPredictionSixHours.setText("—");

        txtPredictionTimeUntilLimit.setText(
                "Yeterli veri bekleniyor"
        );

        txtMoisturePredictionConfidence.setText(
                formatConfidence(
                        prediction.getConfidence_level(),
                        prediction.getConfidence()
                )
        );

        txtPredictionLimitReachedAt.setText(
                "Tahmini sınır zamanı: —"
        );

        txtMoisturePredictionUpdatedAt.setText(
                "Son güncelleme: "
                        + formatPredictionDateTime(
                        prediction.getGenerated_at()
                )
        );

        applyMoisturePredictionWaitingStyle();
    }

    private void renderMoisturePredictionEmpty() {

        txtMoisturePredictionStatus.setText(
                "BEKLENİYOR"
        );

        txtPredictionCurrentMoisture.setText(
                "—"
        );

        txtPredictionMoistureLimit.setText(
                "—"
        );

        txtPredictionOneHour.setText(
                "—"
        );

        txtPredictionThreeHours.setText(
                "—"
        );

        txtPredictionSixHours.setText(
                "—"
        );

        txtPredictionTimeUntilLimit.setText(
                "Hesaplanıyor"
        );

        txtMoisturePredictionConfidence.setText(
                "—"
        );

        txtPredictionLimitReachedAt.setText(
                "Tahmini sınır zamanı: —"
        );

        txtMoisturePredictionUpdatedAt.setText(
                "Son güncelleme: —"
        );

        applyMoisturePredictionWaitingStyle();
    }

    private String formatMoistureValue(
            double moisture
    ) {

        double safeMoisture =
                Math.max(
                        0.0,
                        Math.min(
                                moisture,
                                100.0
                        )
                );

        return String.format(
                Locale.getDefault(),
                "%%%.1f",
                safeMoisture
        );
    }

    private String formatMinutesUntilLimit(
            double estimatedMinutes
    ) {

        if (
                Double.isNaN(estimatedMinutes)
                        || Double.isInfinite(estimatedMinutes)
                        || estimatedMinutes <= 0
        ) {
            return "Hesaplanamıyor";
        }

        long totalMinutes =
                Math.round(
                        estimatedMinutes
                );

        long days =
                totalMinutes / 1440;

        long hours =
                (
                        totalMinutes % 1440
                ) / 60;

        long minutes =
                totalMinutes % 60;

        if (days > 0) {

            return String.format(
                    Locale.getDefault(),
                    "%d gün %d sa",
                    days,
                    hours
            );
        }

        if (hours > 0) {

            return String.format(
                    Locale.getDefault(),
                    "%d sa %d dk",
                    hours,
                    minutes
            );
        }

        return String.format(
                Locale.getDefault(),
                "%d dk",
                minutes
        );
    }

    private String formatPredictionDateTime(
            String isoDateTime
    ) {

        if (
                isoDateTime == null
                        || isoDateTime.trim().isEmpty()
        ) {
            return "—";
        }

        try {

            java.time.LocalDateTime dateTime =
                    java.time.LocalDateTime.parse(
                            isoDateTime.trim()
                    );

            return dateTime.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern(
                                    "dd-MM-yyyy HH:mm",
                                    Locale.getDefault()
                            )
            );

        } catch (Exception exception) {

            Log.w(
                    TAG,
                    "Moisture prediction date could not be formatted.",
                    exception
            );

            return "—";
        }
    }

    private void applyMoisturePredictionStatusStyle(
            String predictionStatus
    ) {

        if ("READY".equals(predictionStatus)) {

            txtMoisturePredictionStatus.setText(
                    "HAZIR"
            );

            applyMoisturePredictionReadyStyle();

        } else if (
                "INSUFFICIENT_DATA".equals(
                        predictionStatus
                )
        ) {

            txtMoisturePredictionStatus.setText(
                    "ÖĞRENİYOR"
            );

            applyMoisturePredictionWaitingStyle();

        } else {

            txtMoisturePredictionStatus.setText(
                    "BEKLENİYOR"
            );

            applyMoisturePredictionWaitingStyle();
        }
    }

    private void applyMoisturePredictionReadyStyle() {

        int online =
                ContextCompat.getColor(
                        this,
                        R.color.online
                );

        int onlineBackground =
                ContextCompat.getColor(
                        this,
                        R.color.onlineBackground
                );

        cardMoisturePredictionStatusBadge
                .setCardBackgroundColor(
                        onlineBackground
                );

        cardMoisturePredictionStatusBadge
                .setStrokeColor(
                        online
                );

        txtMoisturePredictionStatus.setTextColor(
                online
        );
    }

    private void applyMoisturePredictionWaitingStyle() {

        int primary =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int primaryLight =
                ContextCompat.getColor(
                        this,
                        R.color.primaryLight
                );

        cardMoisturePredictionStatusBadge
                .setCardBackgroundColor(
                        primaryLight
                );

        cardMoisturePredictionStatusBadge
                .setStrokeColor(
                        primary
                );

        txtMoisturePredictionStatus.setTextColor(
                primary
        );
    }
    private void renderPredictionValidationStatus(
            PredictionValidationStatus status
    ) {

        if (status == null) {
            renderPredictionValidationIdle();
            return;
        }

        String validationStatus =
                safeText(
                        status.getValidation_status(),
                        "IDLE"
                )
                        .trim()
                        .toUpperCase(Locale.ROOT);

        long pendingCount =
                Math.max(
                        0,
                        status.getPending_count()
                );

        long targetMinutes =
                Math.max(
                        0,
                        status.getTarget_minutes()
                );

        long remainingSeconds =
                Math.max(
                        0,
                        status.getRemaining_seconds()
                );

        boolean waiting =
                "WAITING".equals(validationStatus)
                        && pendingCount > 0;

        if (!waiting) {
            renderPredictionValidationIdle();

            txtPredictionValidationUpdatedAt.setText(
                    formatPredictionValidationDateTime(
                            status.getUpdated_at()
                    )
            );

            return;
        }

        txtPredictionValidationStatus.setText(
                "BEKLİYOR"
        );

        txtPredictionValidationRemaining.setText(
                formatRemainingTime(
                        remainingSeconds
                )
        );

        txtPredictionValidationTarget.setText(
                targetMinutes + " dk"
        );

        txtPredictionValidationPending.setText(
                String.valueOf(
                        pendingCount
                )
        );

        txtPredictionValidationNextTime.setText(
                formatPredictionValidationTime(
                        status.getNext_validation_at()
                )
        );

        txtPredictionValidationUpdatedAt.setText(
                formatPredictionValidationDateTime(
                        status.getUpdated_at()
                )
        );

        int progress =
                calculateValidationProgress(
                        targetMinutes,
                        remainingSeconds
                );

        progressPredictionValidation.setProgressCompat(
                progress,
                true
        );

        txtPredictionValidationPercent.setText(
                progress + "%"
        );

        applyPredictionValidationWaitingStyle();
    }

    private void renderPredictionValidationIdle() {

        txtPredictionValidationStatus.setText(
                "BOŞTA"
        );

        txtPredictionValidationRemaining.setText(
                "Bekleyen doğrulama yok"
        );

        txtPredictionValidationPercent.setText(
                "—"
        );

        txtPredictionValidationTarget.setText(
                "—"
        );

        txtPredictionValidationPending.setText(
                "0"
        );

        txtPredictionValidationNextTime.setText(
                "—"
        );

        txtPredictionValidationUpdatedAt.setText(
                "Bekleniyor"
        );

        progressPredictionValidation.setProgressCompat(
                0,
                false
        );

        applyPredictionValidationIdleStyle();
    }

    private int calculateValidationProgress(
            long targetMinutes,
            long remainingSeconds
    ) {

        if (targetMinutes <= 0) {
            return 0;
        }

        long totalSeconds =
                targetMinutes * 60L;

        long elapsedSeconds =
                totalSeconds - remainingSeconds;

        if (elapsedSeconds < 0) {
            elapsedSeconds = 0;
        }

        if (elapsedSeconds > totalSeconds) {
            elapsedSeconds = totalSeconds;
        }

        return (int) Math.round(
                (
                        elapsedSeconds
                                / (double) totalSeconds
                ) * 100.0
        );
    }

    private String formatRemainingTime(
            long remainingSeconds
    ) {

        long safeSeconds =
                Math.max(
                        0,
                        remainingSeconds
                );

        long hours =
                safeSeconds / 3600;

        long minutes =
                (
                        safeSeconds % 3600
                ) / 60;

        long seconds =
                safeSeconds % 60;

        if (hours > 0) {
            return String.format(
                    Locale.getDefault(),
                    "%d sa %02d dk %02d sn",
                    hours,
                    minutes,
                    seconds
            );
        }

        return String.format(
                Locale.getDefault(),
                "%d dk %02d sn",
                minutes,
                seconds
        );
    }

    private String formatPredictionValidationTime(
            String isoDateTime
    ) {

        if (
                isoDateTime == null
                        || isoDateTime.trim().isEmpty()
        ) {
            return "—";
        }

        try {

            java.time.LocalDateTime dateTime =
                    java.time.LocalDateTime.parse(
                            isoDateTime
                    );

            return dateTime.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern(
                                    "HH:mm",
                                    java.util.Locale.getDefault()
                            )
            );

        } catch (Exception exception) {

            Log.w(
                    TAG,
                    "Prediction validation time could not be formatted.",
                    exception
            );

            return "—";
        }
    }

    private String formatPredictionValidationDateTime(
            String isoDateTime
    ) {

        if (
                isoDateTime == null
                        || isoDateTime.trim().isEmpty()
        ) {
            return "Bekleniyor";
        }

        try {

            java.time.LocalDateTime dateTime =
                    java.time.LocalDateTime.parse(
                            isoDateTime
                    );

            return dateTime.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern(
                                    "dd-MM-yyyy HH:mm:ss",
                                    java.util.Locale.getDefault()
                            )
            );

        } catch (Exception exception) {

            Log.w(
                    TAG,
                    "Prediction validation update time "
                            + "could not be formatted.",
                    exception
            );

            return "Bekleniyor";
        }
    }

    private void applyPredictionValidationWaitingStyle() {

        int primary =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int primaryLight =
                ContextCompat.getColor(
                        this,
                        R.color.primaryLight
                );

        cardPredictionValidationStatusBadge
                .setCardBackgroundColor(
                        primaryLight
                );

        cardPredictionValidationStatusBadge
                .setStrokeColor(
                        primary
                );

        txtPredictionValidationStatus.setTextColor(
                primary
        );
    }

    private void applyPredictionValidationIdleStyle() {

        int textSecondary =
                ContextCompat.getColor(
                        this,
                        R.color.textSecondary
                );

        int surfaceSoft =
                ContextCompat.getColor(
                        this,
                        R.color.surfaceSoft
                );

        cardPredictionValidationStatusBadge
                .setCardBackgroundColor(
                        surfaceSoft
                );

        cardPredictionValidationStatusBadge
                .setStrokeColor(
                        textSecondary
                );

        txtPredictionValidationStatus.setTextColor(
                textSecondary
        );
    }

    @Override
    protected void onDestroy() {

        if (progressAnimator != null) {

            progressAnimator.cancel();
            progressAnimator = null;
        }

        super.onDestroy();
    }
}
