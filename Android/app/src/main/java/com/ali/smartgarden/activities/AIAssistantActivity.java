package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.animation.ValueAnimator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.AIDecision;
import com.ali.smartgarden.models.AIExplanation;
import com.ali.smartgarden.viewmodels.MainViewModel;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class AIAssistantActivity extends AppCompatActivity {

    private MaterialCardView cardAIDecision;
    private MaterialCardView cardAIReasons;
    private MaterialCardView cardAIProgress;
    private MaterialCardView cardAINextStep;
    private MaterialCardView cardAITechnicalSummary;
    private MaterialCardView cardAISeverityBadge;
    private MaterialButton btnBack;
    private TextView txtAIDecisionTitle;
    private TextView txtAIDecisionSummary;
    private TextView txtAISeverityBadge;

    private TextView txtAIReasonOne;
    private TextView txtAIReasonTwo;
    private TextView txtAIReasonThree;

    private TextView txtAIProgressPercent;
    private LinearProgressIndicator progressAI;

    private TextView txtAINextStep;

    private TextView txtAIConfidence;
    private TextView txtAISoilClassification;
    private TextView txtAITrendClassification;
    private TextView txtAIUpdatedAt;

    private MainViewModel viewModel;


    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_ai_assistant
        );

        initializeViews();
        initializeViewModel();
        initializeListeners();
        observeViewModel();

        startEntranceAnimations();
    }


    private void initializeViews() {

        cardAIDecision =
                findViewById(
                        R.id.cardAIDecision
                );

        cardAIReasons =
                findViewById(
                        R.id.cardAIReasons
                );

        cardAIProgress =
                findViewById(
                        R.id.cardAIProgress
                );

        cardAINextStep =
                findViewById(
                        R.id.cardAINextStep
                );

        cardAITechnicalSummary =
                findViewById(
                        R.id.cardAITechnicalSummary
                );

        btnBack =
                findViewById(
                        R.id.btnBack
                );

        cardAISeverityBadge =
                findViewById(
                        R.id.cardAISeverityBadge
                );

        txtAIDecisionTitle =
                findViewById(
                        R.id.txtAIDecisionTitle
                );

        txtAIDecisionSummary =
                findViewById(
                        R.id.txtAIDecisionSummary
                );

        txtAISeverityBadge =
                findViewById(
                        R.id.txtAISeverityBadge
                );

        txtAIReasonOne =
                findViewById(
                        R.id.txtAIReasonOne
                );

        txtAIReasonTwo =
                findViewById(
                        R.id.txtAIReasonTwo
                );

        txtAIReasonThree =
                findViewById(
                        R.id.txtAIReasonThree
                );

        txtAIProgressPercent =
                findViewById(
                        R.id.txtAIProgressPercent
                );

        progressAI =
                findViewById(
                        R.id.progressAI
                );

        txtAINextStep =
                findViewById(
                        R.id.txtAINextStep
                );

        txtAIConfidence =
                findViewById(
                        R.id.txtAIConfidence
                );

        txtAISoilClassification =
                findViewById(
                        R.id.txtAISoilClassification
                );

        txtAITrendClassification =
                findViewById(
                        R.id.txtAITrendClassification
                );

        txtAIUpdatedAt =
                findViewById(
                        R.id.txtAIUpdatedAt
                );
    }


    private void initializeViewModel() {

        viewModel =
                new ViewModelProvider(
                        this
                ).get(
                        MainViewModel.class
                );
    }


    private void initializeListeners() {

        btnBack.setOnClickListener(
                view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
        );
    }


    private void observeViewModel() {

        viewModel.getAIDecision().observe(
                this,
                this::renderAIDecision
        );

        viewModel.getAIExplanation().observe(
                this,
                this::renderAIExplanation
        );
    }


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
                (int) Math.max(
                        0,
                        Math.min(
                                explanation.getProgressPercent(),
                                100
                        )
                );

        animateProgress(
                targetProgress
        );

        txtAINextStep.setText(
                safeText(
                        explanation.getNextStep(),
                        "Yeni veriler bekleniyor."
                )
        );
    }


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

            textView.setVisibility(
                    View.GONE
            );

            return;
        }

        String value =
                reasonLines.get(
                        index
                );

        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            textView.setVisibility(
                    View.GONE
            );

            return;
        }

        textView.setVisibility(
                View.VISIBLE
        );

        textView.setText(
                value.trim()
        );
    }

    private void animateProgress(
            int targetProgress
    ) {

        int currentProgress =
                progressAI.getProgress();

        if (currentProgress == targetProgress) {

            txtAIProgressPercent.setText(
                    "%" + targetProgress
            );

            return;
        }

        ValueAnimator animator =
                ValueAnimator.ofInt(
                        currentProgress,
                        targetProgress
                );

        animator.setDuration(
                500
        );

        animator.addUpdateListener(
                animation -> {

                    int value =
                            (int) animation.getAnimatedValue();

                    progressAI.setProgress(
                            value
                    );

                    txtAIProgressPercent.setText(
                            "%" + value
                    );
                }
        );

        animator.start();
    }

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

        return level
                + " · %"
                + percent;
    }


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
                    updatedAt.indexOf(
                            "T"
                    );

            if (
                    separatorIndex < 0
                            || updatedAt.length()
                            < separatorIndex + 6
            ) {

                return "Bekleniyor";
            }

            String time =
                    updatedAt.substring(
                            separatorIndex + 1,
                            separatorIndex + 6
                    );

            return time;

        } catch (RuntimeException exception) {

            return "Son güncelleme bekleniyor";
        }
    }


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

        progressAI.setIndicatorColor(
                textColor
        );

        progressAI.setTrackColor(
                ContextCompat.getColor(
                        this,
                        R.color.primaryLight
                )
        );
    }

    private void startEntranceAnimations() {

        View[] cards = {
                cardAIDecision,
                cardAIReasons,
                cardAIProgress,
                cardAINextStep,
                cardAITechnicalSummary
        };

        for (int index = 0; index < cards.length; index++) {

            View card = cards[index];

            card.setAlpha(
                    0f
            );

            card.setTranslationY(
                    28f
            );

            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(
                            index * 90L
                    )
                    .setDuration(
                            320L
                    )
                    .start();
        }
    }

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
}