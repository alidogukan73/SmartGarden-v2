package com.ali.smartgarden.fertilization;

/** A conservative tank-mix assessment. It never replaces the product label. */
public final class FertilizerMixResult {

    private final String title;
    private final String message;
    private final boolean caution;

    public FertilizerMixResult(String title, String message, boolean caution) {
        this.title = title;
        this.message = message;
        this.caution = caution;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isCaution() { return caution; }
}
