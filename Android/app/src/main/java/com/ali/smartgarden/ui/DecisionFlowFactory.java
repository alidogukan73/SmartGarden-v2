package com.ali.smartgarden.ui;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.AIExplanation;
import com.ali.smartgarden.models.DecisionStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DecisionFlowFactory {

    private DecisionFlowFactory() {
        // Utility class
    }

    public static List<DecisionStep> create(
            AIExplanation explanation
    ) {

        List<DecisionStep> steps =
                new ArrayList<>();

        AIExplanation.AIDecisionFlow flow =
                explanation != null
                        ? explanation.getDecisionFlow()
                        : null;

        steps.add(
                createStep(
                        1,
                        R.drawable.ic_ai_sensor_24,
                        "Sensör",
                        flow != null
                                ? flow.getSensor()
                                : null,
                        flow != null
                                ? flow.getSensorStatus()
                                : null,
                        "Sensör verisi bekleniyor.",
                        true
                )
        );

        steps.add(
                createStep(
                        2,
                        R.drawable.ic_ai_moisture_24,
                        "Nem Analizi",
                        flow != null
                                ? flow.getMoisture()
                                : null,
                        flow != null
                                ? flow.getMoistureStatus()
                                : null,
                        "Nem verisi analiz edilmeyi bekliyor.",
                        true
                )
        );

        steps.add(
                createStep(
                        3,
                        R.drawable.ic_ai_soil_24,
                        "Toprak",
                        flow != null
                                ? flow.getSoil()
                                : null,
                        flow != null
                                ? flow.getSoilStatus()
                                : null,
                        "Toprak davranışı değerlendirilmeyi bekliyor.",
                        true
                )
        );

        steps.add(
                createStep(
                        4,
                        R.drawable.ic_ai_history_24,
                        "Sulama Geçmişi",
                        flow != null
                                ? flow.getHistory()
                                : null,
                        flow != null
                                ? flow.getHistoryStatus()
                                : null,
                        "Sulama geçmişi değerlendirilmeyi bekliyor.",
                        true
                )
        );

        steps.add(
                createStep(
                        5,
                        R.drawable.ic_ai_brain_24,
                        "AI Kararı",
                        flow != null
                                ? flow.getResult()
                                : null,
                        flow != null
                                ? flow.getResultStatus()
                                : null,
                        "AI kararı hazırlanmayı bekliyor.",
                        false
                )
        );

        return steps;
    }

    private static DecisionStep createStep(
            int stepNumber,
            int iconResource,
            String title,
            String description,
            String rawStatus,
            String fallbackDescription,
            boolean showBottomLine
    ) {

        DecisionStep.Status status =
                parseStatus(
                        rawStatus
                );

        return new DecisionStep(
                stepNumber,
                iconResource,
                title,
                safeText(
                        description,
                        fallbackDescription
                ),
                badgeText(
                        status
                ),
                status,
                showBottomLine
        );
    }

    private static DecisionStep.Status parseStatus(
            String rawStatus
    ) {

        if (
                rawStatus == null
                        || rawStatus.trim().isEmpty()
        ) {
            return DecisionStep.Status.WAITING;
        }

        String normalized =
                rawStatus
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        switch (normalized) {

            case "completed":
            case "complete":
            case "done":
            case "success":
                return DecisionStep.Status.COMPLETED;

            case "analyzing":
            case "analysing":
            case "running":
            case "processing":
                return DecisionStep.Status.ANALYZING;

            case "learning":
            case "training":
                return DecisionStep.Status.LEARNING;

            case "result":
            case "ready":
            case "finished":
                return DecisionStep.Status.RESULT;

            case "waiting":
            case "pending":
            default:
                return DecisionStep.Status.WAITING;
        }
    }

    private static String badgeText(
            DecisionStep.Status status
    ) {

        switch (status) {

            case COMPLETED:
                return "TAMAMLANDI";

            case ANALYZING:
                return "ANALİZ EDİLİYOR";

            case LEARNING:
                return "ÖĞRENİYOR";

            case RESULT:
                return "SONUÇ HAZIR";

            case WAITING:
            default:
                return "BEKLİYOR";
        }
    }

    private static String safeText(
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