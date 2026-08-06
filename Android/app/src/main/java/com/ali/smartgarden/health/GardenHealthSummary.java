package com.ali.smartgarden.health;

public class GardenHealthSummary {
    private final int score;
    private final String title;
    private final String detail;

    public GardenHealthSummary(int score, String title, String detail) {
        this.score = Math.max(0, Math.min(100, score));
        this.title = title;
        this.detail = detail;
    }

    public int getScore() { return score; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
}
