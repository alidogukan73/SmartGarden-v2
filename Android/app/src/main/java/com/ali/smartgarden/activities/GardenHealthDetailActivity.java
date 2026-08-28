package com.ali.smartgarden.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.health.GardenHealthSummary;
import com.ali.smartgarden.health.GardenHealthZoneResult;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.viewmodels.MainViewModel;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/** Explains the garden health score. It is advisory only and never controls hardware. */
public class GardenHealthDetailActivity extends AppCompatActivity {
    private TextView summaryScore;
    private TextView summaryTitle;
    private TextView summaryDetail;
    private LinearLayout zoneList;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_garden_health_detail);

        View root = findViewById(R.id.gardenHealthDetailRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.btnGardenHealthBack).setOnClickListener(view -> finish());
        summaryScore = findViewById(R.id.txtGardenHealthDetailScore);
        summaryTitle = findViewById(R.id.txtGardenHealthDetailTitle);
        summaryDetail = findViewById(R.id.txtGardenHealthDetailSummary);
        zoneList = findViewById(R.id.layoutGardenHealthZones);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getGardenZones().observe(this, this::render);
    }

    private void render(List<GardenZone> zones) {
        long now = System.currentTimeMillis() / 1000L;
        GardenHealthSummary summary = viewModel.gardenHealth(zones, now);
        summaryScore.setText(getString(R.string.runtime_health_score_format, summary.getScore()));
        summaryTitle.setText(summary.getTitle());
        summaryDetail.setText(summary.getDetail());
        summaryScore.setTextColor(ContextCompat.getColor(this, colorFor(summary.getScore())));

        zoneList.removeAllViews();
        if (zones == null || zones.isEmpty()) {
            return;
        }
        for (GardenZone zone : zones) {
            if (zone == null || !zone.isEnabled()) {
                continue;
            }
            addZoneCard(zone, now);
        }
    }

    private void addZoneCard(GardenZone zone, long now) {
        GardenHealthZoneResult result = viewModel.gardenHealthForZone(zone, now);
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(18));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = new TextView(this);
        title.setText(getString(
                R.string.runtime_icon_label,
                zone.getEmoji() == null
                        ? getString(R.string.symbol_plant)
                        : zone.getEmoji(),
                zone.getName() == null
                        ? getString(R.string.zone_fallback_name)
                        : zone.getName()));
        title.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView detail = new TextView(this);
        detail.setText(result.getReason());
        detail.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        detail.setTextSize(13);
        detail.setPadding(0, dp(4), 0, 0);
        text.addView(title);
        text.addView(detail);

        TextView score = new TextView(this);
        score.setText(getString(R.string.runtime_health_score_format, result.getScore()));
        score.setTextColor(ContextCompat.getColor(this, colorFor(result.getScore())));
        score.setTextSize(19);
        score.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(text);
        row.addView(score);
        card.addView(row);
        card.setOnClickListener(view -> {
            Intent intent = new Intent(this, FertilizationZoneDetailActivity.class);
            intent.putExtra(FertilizationZoneDetailActivity.EXTRA_ZONE_ID, zone.getZone_id());
            startActivity(intent);
        });
        zoneList.addView(card);
    }

    private int colorFor(int score) {
        return score >= 85 ? R.color.primary
                : score >= 65 ? R.color.warning : R.color.moistureLow;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
