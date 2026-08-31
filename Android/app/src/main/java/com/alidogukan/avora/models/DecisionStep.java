package com.alidogukan.avora.models;

/**
 * AI karar akışındaki tek bir adımı temsil eder.
 * <p>
 * Örnek adımlar:
 * 1 - Sensör verisi
 * 2 - Nem analizi
 * 3 - Toprak profili
 * 4 - Sulama geçmişi
 * 5 - AI kararı
 */
public class DecisionStep {

    public enum Status {
        COMPLETED,
        ANALYZING,
        LEARNING,
        WAITING,
        RESULT
    }

    private final int stepNumber;
    private final int iconResource;
    private final String title;
    private final String description;
    private final String badgeText;
    private final Status status;
    private final boolean showBottomLine;

    public DecisionStep(
            int stepNumber,
            int iconResource,
            String title,
            String description,
            String badgeText,
            Status status,
            boolean showBottomLine
    ) {
        this.stepNumber = stepNumber;
        this.iconResource = iconResource;
        this.title = title;
        this.description = description;
        this.badgeText = badgeText;
        this.status = status;
        this.showBottomLine = showBottomLine;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public int getIconResource() {
        return iconResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isShowBottomLine() {
        return showBottomLine;
    }
}
