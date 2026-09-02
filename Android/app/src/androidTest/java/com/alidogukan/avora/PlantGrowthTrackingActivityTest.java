package com.alidogukan.avora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alidogukan.avora.activities.PlantGrowthTrackingActivity;
import com.alidogukan.avora.models.GardenPhoto;
import com.alidogukan.avora.photos.LocalGardenPhotoStore;
import com.alidogukan.avora.plantassistant.PlantGrowthTrendPolicy;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PlantGrowthTrackingActivityTest {
    private static final String EMPTY_ZONE = "instrumented-empty-zone";
    private static final String POPULATED_ZONE = "instrumented-growth-zone";

    @Test
    public void emptyZoneShowsGuidanceAndHidesSummary() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        try (ActivityScenario<PlantGrowthTrackingActivity> ignored =
                     ActivityScenario.launch(intent(context, EMPTY_ZONE, "Boş test bölgesi"))) {
            onView(withId(R.id.txtGrowthZone))
                    .check(matches(withText("Boş test bölgesi")));
            onView(withId(R.id.txtGrowthEmpty))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(R.string.runtime_growth_tracking_empty)));
            onView(withId(R.id.cardGrowthSummary))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    @Test
    public void storedGrowthRecordShowsSummaryAndHistoryItem() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LocalGardenPhotoStore store = new LocalGardenPhotoStore(context);
        Bitmap bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        GardenPhoto photo = null;
        try {
            photo = store.save(bitmap, POPULATED_ZONE, "Arayüz testi", "plant_assistant");
            photo = store.updateAnalysis(
                    photo.getId(),
                    "Gelişim dengeli görünüyor",
                    "Görsel güveni %87",
                    "Yaprak yoğunluğu değerlendirildi.",
                    "Aynı açıdan yeni fotoğraf çekin.",
                    "growth_status",
                    87,
                    76,
                    "Vejetatif gelişim",
                    PlantGrowthTrendPolicy.FIRST_RECORD,
                    0,
                    "Yeni yaprak oluşumu",
                    0L);

            try (ActivityScenario<PlantGrowthTrackingActivity> ignored =
                         ActivityScenario.launch(intent(
                                 context, POPULATED_ZONE, "Dolu test bölgesi"))) {
                onView(withId(R.id.cardGrowthSummary))
                        .check(matches(isDisplayed()));
                onView(withId(R.id.txtGrowthEmpty))
                        .check(matches(withEffectiveVisibility(GONE)));
                onView(withId(R.id.txtGrowthSummaryScore))
                        .check(matches(withText(
                                context.getString(R.string.runtime_growth_score_format, 76))));
                onView(withId(R.id.txtGrowthSummaryTrend))
                        .check(matches(withText(R.string.runtime_growth_trend_first)));
                onView(withId(R.id.txtGrowthRecordScore))
                        .check(matches(withText(
                                context.getString(R.string.runtime_growth_score_format, 76))));
                onView(withId(R.id.txtGrowthRecordSignals))
                        .check(matches(withText(context.getString(
                                R.string.runtime_growth_signals_format,
                                "Yeni yaprak oluşumu"))));
            }
        } finally {
            bitmap.recycle();
            if (photo != null) store.delete(photo);
        }
    }

    private static Intent intent(Context context, String zoneId, String zoneLabel) {
        return new Intent(context, PlantGrowthTrackingActivity.class)
                .putExtra(PlantGrowthTrackingActivity.EXTRA_ZONE_ID, zoneId)
                .putExtra(PlantGrowthTrackingActivity.EXTRA_ZONE_LABEL, zoneLabel);
    }
}
