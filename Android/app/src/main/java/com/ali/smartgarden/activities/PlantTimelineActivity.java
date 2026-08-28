package com.ali.smartgarden.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.models.GardenPhoto;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.GardenSeason;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.SeasonStatus;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.models.ZoneSeasonState;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.ali.smartgarden.season.SeasonRepository;
import com.ali.smartgarden.season.SeasonScope;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Per-plant season timeline. It combines manual notes and archived analysis photos. */
public class PlantTimelineActivity extends AppCompatActivity {
    public static final String EXTRA_ZONE_ID = "zone_id";
    public static final String EXTRA_SEASON_ID = "season_id";
    public static final String EXTRA_INITIAL_TAB = "initial_tab";
    public static final String TAB_COMPARE = "compare";

    private final FirebaseRepository repository = new FirebaseRepository();
    private final SeasonRepository seasonRepository = new SeasonRepository();
    private String zoneId = "";
    private GardenZone zone;
    private LinearLayout entries;
    private TextView title, season, status, emoji, month, empty, planting;
    private final List<TimelineItem> items = new ArrayList<>();
    private List<FertilizerApplication> fertilizerApplications = new ArrayList<>();
    private List<WateringHistory> wateringHistory = new ArrayList<>();
    private List<GardenSeason> seasons = new ArrayList<>();
    private List<GardenSeason> observedSeasons = new ArrayList<>();
    private WeatherForecast weatherForecast;
    private String activeFilter = "all";
    private String activeTab = "timeline";
    private String selectedSeasonId = "";
    private boolean seasonSelectionInitialized;
    private boolean zoneSnapshotLoaded;
    private TextView tabTimeline, tabPhotos, tabNotes, tabCompare;
    private int selectedYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
    private final int[] filterIds = {R.id.filterTimelineAll, R.id.filterTimelineWatering, R.id.filterTimelineFertilizer, R.id.filterTimelineAnalysis, R.id.filterTimelineEvent};
    private final String[] filterValues = {"all", "watering", "fertilizer", "analysis", "event"};

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_plant_timeline);
        zoneId = getIntent().getStringExtra(EXTRA_ZONE_ID); if (zoneId == null) zoneId = "";
        if (TAB_COMPARE.equals(getIntent().getStringExtra(EXTRA_INITIAL_TAB))) {
            activeTab = TAB_COMPARE;
        }
        title = findViewById(R.id.txtTimelineTitle); season = findViewById(R.id.txtTimelineSeason); planting = findViewById(R.id.txtTimelinePlanting); status = findViewById(R.id.txtTimelineStatus); emoji = findViewById(R.id.txtTimelineEmoji); month = findViewById(R.id.txtTimelineMonth); empty = findViewById(R.id.txtTimelineEmpty); entries = findViewById(R.id.layoutTimelineEvents);
        tabTimeline = findViewById(R.id.tabTimeline); tabPhotos = findViewById(R.id.tabPhotos); tabNotes = findViewById(R.id.tabNotes); tabCompare = findViewById(R.id.tabCompare);
        findViewById(R.id.btnTimelineBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnTimelineAdd).setOnClickListener(v -> showNewRecordTypes());
        season.setOnClickListener(v -> showSeasonPicker());
        tabTimeline.setOnClickListener(v -> { activeTab = "timeline"; render(); });
        tabPhotos.setOnClickListener(v -> { activeTab = "photos"; render(); });
        tabNotes.setOnClickListener(v -> { activeTab = "notes"; render(); });
        tabCompare.setOnClickListener(v -> { activeTab = "compare"; render(); });
        for (int i = 0; i < filterIds.length; i++) { final String value = filterValues[i]; findViewById(filterIds[i]).setOnClickListener(v -> { activeFilter = value; render(); }); }
        repository.observeGardenZones().observe(this, zones -> {
            zone = null;
            if (zones != null) {
                for (GardenZone value : zones) {
                    if (zoneId.equals(value.getZone_id())) { zone = value; break; }
                }
            }
            zoneSnapshotLoaded = true;
            refreshVisibleSeasons();
            selectInitialSeason();
            render();
        });
        repository.observeFertilizerHistory().observe(this, values -> { fertilizerApplications = values == null ? new ArrayList<>() : values; loadItems(); render(); });
        repository.observeWateringHistory().observe(this, values -> { wateringHistory = values == null ? new ArrayList<>() : values; loadItems(); render(); });
        repository.observeWeatherForecast().observe(this, value -> { weatherForecast = value; addAutomaticSignals(); loadItems(); render(); });
        seasonRepository.observeZoneSeasons(zoneId).observe(this, values -> {
            observedSeasons = values == null ? new ArrayList<>() : new ArrayList<>(values);
            refreshVisibleSeasons();
            if (zoneSnapshotLoaded) selectInitialSeason();
            render();
        });
    }

    @Override protected void onResume() { super.onResume(); loadItems(); render(); }

    private void loadItems() {
        items.clear();
        for (GardenEvent event : new LocalGardenEventStore(this).load()) if (zoneId.equals(event.getZone_id())) items.add(TimelineItem.event(event));
        Set<String> journalPhotoGroups = new HashSet<>();
        for (GardenPhoto photo : new LocalGardenPhotoStore(this).load()) {
            if (!zoneId.equals(photo.getZone_id())) continue;
            String groupId = photo.getRelated_application_id();
            if (groupId != null && groupId.startsWith("journal_record_") && !journalPhotoGroups.add(groupId)) continue;
            items.add(TimelineItem.photo(photo));
        }
        for (FertilizerApplication application : fertilizerApplications) if (zoneId.equals(application.getZone_id())) items.add(TimelineItem.fertilizer(application));
        for (WateringHistory watering : wateringHistory) if (zoneId.equals(watering.getZoneId()) && watering.isCompleted()) items.add(TimelineItem.watering(watering));
        items.sort(Comparator.comparingLong(TimelineItem::time).reversed());
    }

    private void render() {
        addAutomaticSignals();
        GardenSeason selected = selectedSeason();
        String name;
        if (selected != null && selected.getZone_name() != null
                && !selected.getZone_name().isBlank()) {
            name = selected.getZone_name().trim();
        } else {
            name = zone == null || zone.getName() == null || zone.getName().isBlank() ? getString(R.string.runtime_plant_default) : zone.getName();
        }
        title.setText(getString(R.string.runtime_timeline_named_title, name));
        String seasonHeader = seasonHeader(name, selected);
        season.setText(seasonHeader);
        season.setContentDescription(getString(
                R.string.runtime_timeline_season_selector_description, seasonHeader));
        planting.setText("● " + plantingDateText());
        status.setText("● " + liveSeasonStatus());
        String archiveEmoji = selected == null ? "" : selected.getEmoji();
        emoji.setText(archiveEmoji == null || archiveEmoji.isBlank()
                ? zone == null || zone.getEmoji() == null ? getString(R.string.symbol_plant) : zone.getEmoji() : archiveEmoji);
        month.setText(tabHeading());
        month.setVisibility("compare".equals(activeTab) ? View.VISIBLE : View.GONE);
        entries.removeAllViews(); int shown = 0; String lastMonthKey = "";
        if ("compare".equals(activeTab)) {
            shown = renderComparison();
        } else {
            for (TimelineItem item : items) if (recordBelongsToSelectedSeason(item) && visibleInTab(item)) {
                String monthKey = monthKey(item.time());
                if (!monthKey.equals(lastMonthKey)) {
                    TextView monthHeader = text(monthHeading(item.time()), 20, R.color.textPrimary);
                    monthHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(-1, -2);
                    monthParams.topMargin = shown == 0 ? dp(4) : dp(16);
                    monthParams.bottomMargin = dp(9);
                    monthHeader.setLayoutParams(monthParams);
                    entries.addView(monthHeader);
                    lastMonthKey = monthKey;
                }
                entries.addView(card(item));
                shown++;
            }
        }
        empty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        empty.setText("compare".equals(activeTab)
                ? R.string.runtime_compare_empty : R.string.runtime_view_empty);
        updateTabStyle(tabTimeline, "timeline"); updateTabStyle(tabPhotos, "photos"); updateTabStyle(tabNotes, "notes"); updateTabStyle(tabCompare, "compare");
        for (int i = 0; i < filterIds.length; i++) {
            MaterialCardView filter = findViewById(filterIds[i]); boolean filterSelected = filterValues[i].equals(activeFilter);
            filter.setCardBackgroundColor(getColor(filterSelected ? R.color.surfaceGreen : R.color.card));
            filter.setStrokeColor(getColor(filterSelected ? R.color.primary : R.color.border));
        }
    }

    private String monthKey(long epoch) {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date(epoch * 1000L));
    }

    private String monthHeading(long epoch) {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(new Date(epoch * 1000L));
    }
    private String tabHeading() {
        if ("photos".equals(activeTab)) return getString(R.string.runtime_growth_photos);
        if ("notes".equals(activeTab)) return getString(R.string.runtime_notes_observations);
        if ("compare".equals(activeTab)) return getString(R.string.runtime_photo_comparison);
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
    }

    private String plantingDateText() {
        GardenSeason selected = selectedSeason();
        String configuredDate = selected == null ? "" : selected.getPlanting_date();
        if ((configuredDate == null || configuredDate.isBlank())
                && selected != null && SeasonStatus.isActive(selected.getStatus())
                && zone != null && zone.getFertilization() != null) {
            configuredDate = zone.getFertilization().getPlanting_date();
        }
        if (configuredDate != null && !configuredDate.isBlank()) {
            return getString(R.string.runtime_planting_done, formatPlantingDate(configuredDate));
        }
        if (selected == null && zone != null && zone.getFertilization() != null) {
            configuredDate = zone.getFertilization().getPlanting_date();
            if (configuredDate != null && !configuredDate.isBlank()) {
                return getString(R.string.runtime_planting_done, formatPlantingDate(configuredDate));
            }
        }
        for (TimelineItem item : items) {
            if (recordBelongsToSelectedSeason(item)
                    && item.event != null && item.event.getType() != null
                    && item.event.getType().toLowerCase(Locale.ROOT).contains("dikim")) {
                return getString(R.string.runtime_planting_done,
                        new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                                .format(new Date(item.time() * 1000L)));
            }
        }
        return getString(R.string.runtime_planting_missing);
    }

    private String formatPlantingDate(String value) {
        try {
            LocalDate date = LocalDate.parse(value);
            return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault()));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String liveSeasonStatus() {
        GardenSeason selected = selectedSeason();
        if (selected != null) {
            if (SeasonStatus.isClosed(selected.getStatus())) return getString(R.string.season_closed_event_title);
            if (SeasonStatus.PLANNED.equals(selected.getStatus())) return getString(R.string.runtime_planned_season);
            if (zone != null && !zone.isEnabled()) return getString(R.string.runtime_zone_inactive);
            return getString(R.string.runtime_season_ongoing);
        }
        for (TimelineItem item : items) {
            if (recordBelongsToSelectedSeason(item) && item.event != null
                    && item.event.getType() != null
                    && item.event.getType().toLowerCase(Locale.ROOT).contains("hasat")) {
                return getString(R.string.season_closed_event_title);
            }
        }
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        if (selectedYear < currentYear) return getString(R.string.runtime_past_season);
        if (selectedYear > currentYear) return getString(R.string.runtime_planned_season);
        if (zone != null && !zone.isEnabled()) return getString(R.string.runtime_zone_inactive);
        return getString(R.string.runtime_season_ongoing);
    }
    private boolean visibleInTab(TimelineItem item) {
        if ("photos".equals(activeTab)) return item.photo != null;
        if ("notes".equals(activeTab)) return item.event != null && "MANUAL".equals(item.event.getSource());
        return item.matches(activeFilter);
    }

    private void updateTabStyle(TextView tab, String value) {
        boolean selected = value.equals(activeTab);
        tab.setTextColor(getColor(selected ? R.color.primary : R.color.textSecondary));
        tab.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private int renderComparison() {
        List<TimelineItem> photos = new ArrayList<>();
        for (TimelineItem item : items) if (item.photo != null && recordBelongsToSelectedSeason(item)) photos.add(item);
        if (photos.size() < 2) return 0;
        TimelineItem newest = photos.get(0), previous = photos.get(1);
        MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(16)); card.setCardBackgroundColor(getColor(R.color.card)); card.setStrokeColor(getColor(R.color.border)); card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(9); card.setLayoutParams(params);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView heading = text(getString(R.string.runtime_compare_heading), 15, R.color.textPrimary); heading.setTypeface(null, android.graphics.Typeface.BOLD); content.addView(heading);
        LinearLayout images = new LinearLayout(this); images.setOrientation(LinearLayout.HORIZONTAL); images.setPadding(0, dp(12), 0, 0);
        addComparisonImage(images, previous, getString(R.string.runtime_previous)); addComparisonImage(images, newest, getString(R.string.runtime_new)); content.addView(images);
        TextView detail = text(getString(R.string.runtime_compare_detail), 12, R.color.textSecondary); detail.setPadding(0, dp(12), 0, 0); content.addView(detail);
        card.addView(content); card.setOnClickListener(v -> openRecord(newest)); entries.addView(card); return 1;
    }

    private void addComparisonImage(LinearLayout parent, TimelineItem item, String label) {
        LinearLayout holder = new LinearLayout(this); holder.setOrientation(LinearLayout.VERTICAL); holder.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); if (parent.getChildCount() > 0) lp.setMarginStart(dp(10)); holder.setLayoutParams(lp);
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP); image.setImageURI(android.net.Uri.fromFile(new File(item.photo.getLocal_path()))); holder.addView(image, new LinearLayout.LayoutParams(-1, dp(132)));
        TextView caption = text(label + " · " + new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(item.time() * 1000L)), 11, R.color.textSecondary); caption.setGravity(Gravity.CENTER); caption.setPadding(0, dp(6), 0, 0); holder.addView(caption); parent.addView(holder);
    }
    private void addAutomaticSignals() {
        GardenSeason active = activeSeason();
        if (zone == null || active == null) return;
        LocalGardenEventStore store = new LocalGardenEventStore(this); GardenEvent created = null;
        if (zone.hasSensorData() && zone.getMoisture() <= zone.getMoisture_limit() - 10) {
            created = store.addAutomaticOncePerDay(zoneId, "Nem riski", "Toprak nemi %" + zone.getMoisture() + ". Bölge limiti %" + zone.getMoisture_limit() + " altında.", "moisture_risk");
        }
        if (created == null && weatherForecast != null) {
            Double temperature = weatherForecast.getTodayTemperatureMax(), rain = weatherForecast.getTodayRainProbability(), wind = weatherForecast.getTodayWindMax();
            if (temperature != null && temperature >= 34) created = store.addAutomaticOncePerDay(zoneId, "Sıcak hava uyarısı", "Bugün en yüksek sıcaklık " + Math.round(temperature) + "°C. Toprak nemini ve yapraklarda solmayı takip edin.", "hot_weather");
            else if (rain != null && rain >= 70) created = store.addAutomaticOncePerDay(zoneId, "Yağış uyarısı", "Yağış olasılığı %" + Math.round(rain) + ". Sulama öncesi toprak nemini yeniden kontrol edin.", "rain_weather");
            else if (wind != null && wind >= 35) created = store.addAutomaticOncePerDay(zoneId, "Kuvvetli rüzgar", "Rüzgar yaklaşık " + Math.round(wind) + " km/sa. Toprak nemi daha hızlı düşebilir.", "wind_weather");
        }
        if (created != null) {
        created.setSeason_id(active.getSeason_id());
        store.replaceSeasonId(created.getId(), active.getSeason_id());
        GardenEvent createdEvent = created;
        repository.saveGardenEvent(createdEvent).addOnSuccessListener(unused -> {
            loadItems(); render();
        });
    }
}
    private View card(TimelineItem item) {
        LinearLayout timelineRow = new LinearLayout(this);
        timelineRow.setOrientation(LinearLayout.HORIZONTAL);
        timelineRow.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(-1, -2);
        outer.bottomMargin = dp(7);
        timelineRow.setLayoutParams(outer);

        LinearLayout dateColumn = new LinearLayout(this);
        dateColumn.setOrientation(LinearLayout.VERTICAL);
        dateColumn.setGravity(Gravity.CENTER);
        TextView date = text(new SimpleDateFormat("dd", Locale.forLanguageTag("tr-TR"))
                .format(new Date(item.time() * 1000L)), 14, R.color.textPrimary);
        date.setGravity(Gravity.CENTER);
        date.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView time = text(new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))
                .format(new Date(item.time() * 1000L)), 9, R.color.textSecondary);
        time.setGravity(Gravity.CENTER);
        dateColumn.addView(date);
        dateColumn.addView(time);
        timelineRow.addView(dateColumn, new LinearLayout.LayoutParams(dp(46), dp(68)));

        FrameLayout rail = new FrameLayout(this);
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(dp(34), dp(68));
        rail.setLayoutParams(railParams);
        View connector = new View(this);
        connector.setBackgroundColor(getColor(R.color.accentLight));
        FrameLayout.LayoutParams connectorParams = new FrameLayout.LayoutParams(dp(2), -1, Gravity.CENTER_HORIZONTAL);
        rail.addView(connector, connectorParams);
        TextView type = text(item.icon(this), 17, R.color.primary);
        type.setGravity(Gravity.CENTER);
        type.setBackground(timelineDotBackground());
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        dotParams.topMargin = dp(18);
        rail.addView(type, dotParams);
        timelineRow.addView(rail);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardBackgroundColor(getColor(R.color.card));
        card.setStrokeColor(getColor(R.color.border));
        card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -2, 1);
        card.setLayoutParams(cardParams);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView heading = text(item.title(this), 14, R.color.textPrimary);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView detail = text(item.detail(this), 12, R.color.textSecondary);
        info.addView(heading);
        info.addView(detail);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        if (item.photo != null && item.photo.getLocal_path() != null && new File(item.photo.getLocal_path()).exists()) {
            ImageView picture = new ImageView(this);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            picture.setImageURI(android.net.Uri.fromFile(new File(item.photo.getLocal_path())));
            row.addView(picture, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        card.addView(row);
        card.setOnClickListener(v -> openRecord(item));
        timelineRow.addView(card);
        return timelineRow;
    }

    private GradientDrawable timelineDotBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(getColor(R.color.card));
        background.setStroke(dp(1), getColor(R.color.accentLight));
        return background;
    }
    private TextView text(String s, int size, int color) { TextView view = new TextView(this); view.setText(s); view.setTextSize(size); view.setTextColor(getColor(color)); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void openRecord(TimelineItem item) {
        Intent intent = new Intent(this, JournalRecordDetailActivity.class);
        GardenSeason selected = selectedSeason();
        intent.putExtra("title", item.title(this));
        intent.putExtra("detail", item.detail(this));
        intent.putExtra("icon", item.icon(this));
        intent.putExtra("time", item.time());
        intent.putExtra("zone_id", zoneId);
        intent.putExtra("season_id", item.seasonId());
        intent.putExtra("season_read_only",
                selected != null && SeasonStatus.isClosed(selected.getStatus()));
        if (item.event != null && "MANUAL".equals(item.event.getSource())) {
            intent.putExtra("manual_event_id", item.event.getId());
            intent.putExtra("manual_event_type", item.event.getType());
        }
        if (item.photo != null) {
            intent.putExtra("photo_path", item.photo.getLocal_path());
            intent.putExtra("photo_group_id", item.photo.getRelated_application_id());
            intent.putExtra("advice", item.photo.getAnalysis_advice());
        }
        startActivity(intent);
    }
    private int yearOf(long epoch) { java.util.Calendar calendar = java.util.Calendar.getInstance(); calendar.setTimeInMillis(epoch * 1000L); return calendar.get(java.util.Calendar.YEAR); }
    private void showSeasonPicker() {
        PopupMenu menu = new PopupMenu(this, season);
        if (!seasons.isEmpty()) {
            for (GardenSeason value : seasons) {
                String label = value.getLabel().isBlank()
                        ? yearOf(value.getStarted_at_epoch()) + " Sezonu"
                        : value.getLabel();
                menu.getMenu().add(label).setIntent(
                        new Intent().putExtra("season_id", value.getSeason_id())
                );
            }
            menu.setOnMenuItemClickListener(item -> {
                Intent metadata = item.getIntent();
                selectedSeasonId = metadata == null ? "" : metadata.getStringExtra("season_id");
                GardenSeason selected = selectedSeason();
                if (selected != null) selectedYear = yearOf(selected.getStarted_at_epoch());
                render();
                return true;
            });
        } else {
            java.util.TreeSet<Integer> years = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
            years.add(selectedYear);
            for (TimelineItem item : items) years.add(yearOf(item.time()));
            for (Integer year : years) menu.getMenu().add(String.valueOf(year));
            menu.setOnMenuItemClickListener(item -> {
                selectedYear = Integer.parseInt(String.valueOf(item.getTitle()));
                render();
                return true;
            });
        }
        menu.show();
    }


    private void refreshVisibleSeasons() {
        seasons = new ArrayList<>();
        ZoneSeasonState current = zone == null ? null : zone.getSeason();
        for (GardenSeason value : observedSeasons) {
            if (SeasonScope.isVisibleSeason(value, current)) {
                seasons.add(value);
            }
        }
    }

    private void selectInitialSeason() {
        if (seasonSelectionInitialized && selectedSeason() != null) return;
        String requested = getIntent().getStringExtra(EXTRA_SEASON_ID);
        GardenSeason choice = null;
        if (requested != null && !requested.isBlank()) {
            for (GardenSeason value : seasons) {
                if (requested.equals(value.getSeason_id())) { choice = value; break; }
            }
        }
        if (choice == null) {
            for (GardenSeason value : seasons) {
                if (SeasonStatus.isActive(value.getStatus())) { choice = value; break; }
            }
        }
        if (choice == null && !seasons.isEmpty()) choice = seasons.get(0);
        if (choice != null) {
            selectedSeasonId = choice.getSeason_id();
            selectedYear = yearOf(choice.getStarted_at_epoch());
            seasonSelectionInitialized = true;
        }
    }

    private GardenSeason selectedSeason() {
        for (GardenSeason value : seasons) {
            if (selectedSeasonId.equals(value.getSeason_id())) return value;
        }
        return null;
    }

    private GardenSeason activeSeason() {
        for (GardenSeason value : seasons) {
            if (SeasonStatus.isActive(value.getStatus())) return value;
        }
        return null;
    }

    private String seasonHeader(String zoneName, GardenSeason selected) {
        if (selected == null) {
            return getString(R.string.runtime_timeline_season_header, zoneName,
                    getString(R.string.runtime_timeline_season_year, selectedYear));
        }
        String label = selected.getLabel().isBlank()
                ? getString(R.string.runtime_timeline_season_year, selectedYear)
                : selected.getLabel().trim();
        String normalizedZone = zoneName == null ? "" : zoneName.trim();
        // Compatibility with the first season format: "2026 Domates".
        if (!normalizedZone.isBlank() && (label.endsWith(" " + normalizedZone)
                || label.equalsIgnoreCase(normalizedZone))) {
            label = getString(R.string.runtime_timeline_season_year, selectedYear);
        }
        return getString(R.string.runtime_timeline_season_header, zoneName, label);
    }

    private boolean recordBelongsToSelectedSeason(TimelineItem item) {
        GardenSeason selected = selectedSeason();
        if (selected == null) return yearOf(item.time()) == selectedYear;
        ZoneSeasonState scope = new ZoneSeasonState();
        scope.setActive_season_id(selected.getSeason_id());
        scope.setStatus(selected.getStatus());
        scope.setStarted_at_epoch(selected.getStarted_at_epoch());
        scope.setEnded_at_epoch(selected.getEnded_at_epoch());
        scope.setInclude_legacy_records(selected.isIncludes_legacy_records());
        return SeasonScope.belongsTo(item.seasonId(), item.time(), scope);
    }
    private void showNewRecordTypes() {
        GardenSeason active = activeSeason();
        if (active == null) {
            Toast.makeText(this, R.string.runtime_start_season_first, Toast.LENGTH_LONG).show();
            return;
        }
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.btnTimelineAdd));
        String[] types = {"Dikim yapıldı", "Gözlem / not", "Çiçeklenme dönemi başladı", "İlk ürün", "Hasat", "Özel olay", "Gelişim fotoğrafı ekle"};
        for (int index = 0; index < types.length; index++) {
            menu.getMenu().add(0, index, index, eventTypeLabel(types[index]));
        }
        menu.setOnMenuItemClickListener(choice -> {
            String type = types[choice.getItemId()];
            if ("Gelişim fotoğrafı ekle".equals(type)) {
                Intent i = new Intent(this, NewJournalRecordActivity.class);
                i.putExtra(NewJournalRecordActivity.EXTRA_ZONE_ID, zoneId);
                i.putExtra(NewJournalRecordActivity.EXTRA_SEASON_ID, active.getSeason_id());
                i.putExtra(NewJournalRecordActivity.EXTRA_INITIAL_TYPE, NewJournalRecordActivity.RECORD_TYPE_PHOTO);
                startActivity(i);
            } else showNewEventDialog(type);
            return true;
        });
        menu.show();
    }

    private String eventTypeLabel(String type) {
        if ("Dikim yapıldı".equals(type)) return getString(R.string.runtime_event_planting);
        if ("Gözlem / not".equals(type)) return getString(R.string.runtime_event_note);
        if ("Çiçeklenme dönemi başladı".equals(type)) return getString(R.string.runtime_event_flowering);
        if ("İlk ürün".equals(type)) return getString(R.string.runtime_event_first_product);
        if ("Hasat".equals(type)) return getString(R.string.runtime_event_harvest);
        if ("Özel olay".equals(type)) return getString(R.string.runtime_event_special);
        if ("Gelişim fotoğrafı ekle".equals(type)) return getString(R.string.runtime_event_growth_photo);
        return type;
    }

    private void showNewEventDialog(String type) {
        GardenSeason active = activeSeason();
        if (active == null) {
            Toast.makeText(this, R.string.runtime_start_season_first, Toast.LENGTH_LONG).show();
            return;
        }
        EditText input = new EditText(this);
        input.setHint(type.equals("Dikim yapıldı")
                ? R.string.runtime_planting_hint : R.string.runtime_short_note_hint);
        input.setMinLines(3);
        int pad = dp(20);
        input.setPadding(pad, dp(8), pad, dp(8));
        new MaterialAlertDialogBuilder(this)
                .setTitle(eventTypeLabel(type))
                .setMessage(R.string.runtime_active_season_record_note)
                .setView(input)
                .setNegativeButton(R.string.settings_quick_cancel, null)
                .setPositiveButton(R.string.settings_quick_save, (dialog, which) -> {
                    LocalGardenEventStore store = new LocalGardenEventStore(this);
                    GardenEvent saved = store.addForSeason(
                            zoneId, active.getSeason_id(), type, input.getText().toString());
                    repository.saveGardenEvent(saved)
                            .addOnSuccessListener(unused -> {
                                store.replaceSeasonId(saved.getId(), saved.getSeason_id());
                                loadItems();
                                render();
                            })
                            .addOnFailureListener(error -> Toast.makeText(
                                    this, error.getMessage(), Toast.LENGTH_LONG).show());
                }).show();
    }
    private static final class TimelineItem {
        final GardenEvent event; final GardenPhoto photo; final FertilizerApplication fertilizer; final WateringHistory watering;
        private TimelineItem(GardenEvent e, GardenPhoto p, FertilizerApplication f, WateringHistory w) { event = e; photo = p; fertilizer = f; watering = w; }
        static TimelineItem event(GardenEvent v) { return new TimelineItem(v, null, null, null); } static TimelineItem photo(GardenPhoto v) { return new TimelineItem(null, v, null, null); } static TimelineItem fertilizer(FertilizerApplication v) { return new TimelineItem(null, null, v, null); } static TimelineItem watering(WateringHistory v) { return new TimelineItem(null, null, null, v); }
        long time() { if (event != null) return event.getOccurred_at_epoch(); if (photo != null) return photo.getCaptured_at_epoch(); if (fertilizer != null) return fertilizer.getApplied_at_epoch(); return parseTime(watering.getFinishedAt()); }
        String seasonId() { if (event != null) return event.getSeason_id(); if (photo != null) return photo.getSeason_id(); if (fertilizer != null) return fertilizer.getSeason_id(); return watering.getSeasonId(); }
        String title(android.content.Context context) {
            if (fertilizer != null) return context.getString(R.string.notification_category_fertilization);
            if (watering != null) return context.getString(R.string.notification_category_irrigation);
            if (photo != null) return photo.getAnalysis_title() == null || photo.getAnalysis_title().isBlank()
                    ? context.getString(R.string.runtime_growth_photo) : photo.getAnalysis_title();
            String raw = event.getType();
            if (raw == null || raw.isBlank()) return context.getString(R.string.runtime_garden_record);
            if ("Dikim yapıldı".equals(raw)) return context.getString(R.string.runtime_event_planting);
            if ("Gözlem / not".equals(raw)) return context.getString(R.string.runtime_event_note);
            if ("Çiçeklenme dönemi başladı".equals(raw)) return context.getString(R.string.runtime_event_flowering);
            if ("İlk ürün".equals(raw)) return context.getString(R.string.runtime_event_first_product);
            if ("Hasat".equals(raw)) return context.getString(R.string.runtime_event_harvest);
            if ("Özel olay".equals(raw)) return context.getString(R.string.runtime_event_special);
            if ("Takip fotoğrafı önerisi".equals(raw)) return context.getString(R.string.runtime_follow_up_photo_title);
            if ("Takip değerlendirmesi".equals(raw)) return context.getString(R.string.runtime_follow_up_assessment_title);
            if ("Nem riski".equals(raw)) return context.getString(R.string.runtime_signal_moisture_title);
            if ("Sıcak hava uyarısı".equals(raw)) return context.getString(R.string.runtime_signal_hot_title);
            if ("Yağış uyarısı".equals(raw)) return context.getString(R.string.runtime_signal_rain_title);
            if ("Kuvvetli rüzgar".equals(raw)) return context.getString(R.string.runtime_signal_wind_title);
            return raw;
        }
        String detail(android.content.Context context) {
            if (fertilizer != null) return fertilizer.getProduct_name() + " · "
                    + fertilizer.getApplied_dose() + " " + fertilizer.getDose_unit();
            if (watering != null) return context.getString(
                    R.string.runtime_duration_seconds, watering.getDuration());
            if (photo != null) return photo.getNote() == null || photo.getNote().isBlank()
                    ? context.getString(R.string.runtime_growth_photo_added) : photo.getNote();
            String type = event.getType() == null ? "" : event.getType();
            if ("Takip fotoğrafı önerisi".equals(type)) return context.getString(R.string.runtime_follow_up_photo_note);
            if ("Takip değerlendirmesi".equals(type)) return context.getString(R.string.runtime_follow_up_assessment_note);
            if ("Nem riski".equals(type)) return context.getString(R.string.runtime_signal_moisture_note);
            if ("Sıcak hava uyarısı".equals(type)) return context.getString(R.string.runtime_signal_hot_note);
            if ("Yağış uyarısı".equals(type)) return context.getString(R.string.runtime_signal_rain_note);
            if ("Kuvvetli rüzgar".equals(type)) return context.getString(R.string.runtime_signal_wind_note);
            return event.getNote() == null || event.getNote().isBlank()
                    ? context.getString(R.string.runtime_journal_added) : event.getNote();
        }
        String icon(android.content.Context context) { if (fertilizer != null) return context.getString(R.string.symbol_plant); if (watering != null) return context.getString(R.string.symbol_water_drop); if (photo != null) return context.getString(R.string.symbol_leaf); String type = event.getType().toLowerCase(Locale.ROOT); return type.contains("gübre") ? context.getString(R.string.symbol_plant) : type.contains("sula") ? context.getString(R.string.symbol_water_drop) : type.contains("analiz") ? context.getString(R.string.symbol_sparkle) : context.getString(R.string.symbol_bullet); }
        boolean matches(String filter) { if ("all".equals(filter)) return true; if ("watering".equals(filter)) return watering != null || event != null && event.getType().toLowerCase(Locale.ROOT).contains("sula"); if ("fertilizer".equals(filter)) return fertilizer != null || event != null && event.getType().toLowerCase(Locale.ROOT).contains("gübre"); if ("analysis".equals(filter)) return photo != null || event != null && event.getType().toLowerCase(Locale.ROOT).contains("analiz"); return event != null && !event.getType().toLowerCase(Locale.ROOT).contains("sula") && !event.getType().toLowerCase(Locale.ROOT).contains("gübre") && !event.getType().toLowerCase(Locale.ROOT).contains("analiz"); }
        private static long parseTime(String value) { if (value == null) return 0L; String[] formats = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy HH:mm"}; for (String format : formats) try { return new SimpleDateFormat(format, Locale.US).parse(value).getTime() / 1000L; } catch (Exception ignored) { } return 0L; }
    }
}
