package com.ali.smartgarden.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.CropCatalogItem;
import com.ali.smartgarden.models.GardenSeason;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.SeasonOutcome;
import com.ali.smartgarden.models.ZoneSeasonState;
import com.ali.smartgarden.viewmodels.SeasonManagementViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Explicit, non-destructive lifecycle management for every garden zone season. */
public final class SeasonManagementActivity extends AppCompatActivity {
    private SeasonManagementViewModel viewModel;
    private LinearLayout inactiveZoneContainer;
    private TextView inactiveZonesTitle;
    private TextView inactiveZonesDescription;
    private final List<GardenZone> zones = new ArrayList<>();
    private final List<GardenSeason> seasons = new ArrayList<>();
    private final List<CropCatalogItem> cropCatalogItems = new ArrayList<>();

    private LinearLayout zoneContainer;
    private TextView emptyView;
    private String lastRenderSignature = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_season_management);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        zoneContainer = findViewById(R.id.layoutSeasonZones);
        emptyView = findViewById(R.id.txtSeasonEmpty);
        inactiveZoneContainer = findViewById(R.id.layoutInactiveSeasonZones);
        inactiveZonesTitle = findViewById(R.id.txtInactiveSeasonZonesTitle);
        inactiveZonesDescription = findViewById(R.id.txtInactiveSeasonZonesDescription);
        viewModel = new ViewModelProvider(this).get(SeasonManagementViewModel.class);

        viewModel.getZones().observe(this, values -> {
            zones.clear();
            if (values != null) zones.addAll(values);
            viewModel.synchronizeLegacySeasons(values);
            renderIfChanged();
        });
        viewModel.getSeasons().observe(this, values -> {
            seasons.clear();
            if (values != null) seasons.addAll(values);
            renderIfChanged();
        });
        viewModel.getCropCatalogItems().observe(this, values -> {
            cropCatalogItems.clear();
            cropCatalogItems.addAll(viewModel.mergedCrops(values));
        });
        viewModel.getBootstrapNotices().observe(this, event -> {
            if (event == null) return;
            SeasonManagementViewModel.BootstrapNotice notice = event.consume();
            if (notice == null) return;
            lastRenderSignature = "";
            if (notice.error == null) {
                Toast.makeText(this, notice.messageRes, Toast.LENGTH_LONG).show();
            } else {
                showError(notice.error, notice.messageRes);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!zones.isEmpty()) {
            lastRenderSignature = "";
            renderIfChanged();
        }
    }

    private void renderIfChanged() {
        String signature = renderSignature();
        if (signature.equals(lastRenderSignature)) return;
        lastRenderSignature = signature;
        render();
    }

    private void render() {
        zoneContainer.removeAllViews();
        inactiveZoneContainer.removeAllViews();

        List<GardenZone> activeZones = viewModel.activeZones(zones);
        List<GardenZone> inactiveZones = new ArrayList<>();
        for (GardenZone zone : zones) {
            if (viewModel.isInactiveArchiveZone(zone)
                    && hasCompletedSeasonArchive(zone.getZone_id())) {
                inactiveZones.add(zone);
            }
        }

        emptyView.setVisibility(activeZones.isEmpty() ? View.VISIBLE : View.GONE);
        emptyView.setText(zones.isEmpty()
                ? R.string.season_management_no_zones
                : R.string.season_management_no_active_zones);
        for (GardenZone zone : activeZones) {
            zoneContainer.addView(createZoneCard(zone));
        }

        int inactiveVisibility = inactiveZones.isEmpty() ? View.GONE : View.VISIBLE;
        inactiveZonesTitle.setVisibility(inactiveVisibility);
        inactiveZonesDescription.setVisibility(inactiveVisibility);
        inactiveZoneContainer.setVisibility(inactiveVisibility);
        if (!inactiveZones.isEmpty()) {
            inactiveZonesTitle.setText(getString(
                    R.string.season_management_inactive_title,
                    inactiveZones.size()
            ));
            for (GardenZone zone : inactiveZones) {
                inactiveZoneContainer.addView(createInactiveZoneCard(zone));
            }
        }
    }

    /**
     * The garden-zones listener also receives frequent sensor measurements. None of those
     * measurements changes this screen, so they must not recreate every MaterialButton.
     */
    private String renderSignature() {
        StringBuilder value = new StringBuilder(256);
        for (GardenZone zone : zones) {
            if (zone == null) continue;
            ZoneSeasonState state = zone.getSeason();
            value.append("Z|")
                    .append(safe(zone.getZone_id())).append('|')
                    .append(safe(zone.getName())).append('|')
                    .append(safe(zone.getEmoji())).append('|')
                    .append(zone.isEnabled()).append('|')
                    .append(safe(zone.getLifecycle_status())).append('|')
                    .append(zone.getFertilization() == null
                            ? ""
                            : safe(zone.getFertilization().getGrowth_stage()))
                    .append('|');
            if (state == null) {
                value.append("NO_SEASON");
            } else {
                value.append(safe(state.getStatus())).append('|')
                        .append(safe(state.getActive_season_id())).append('|')
                        .append(safe(state.getLabel())).append('|')
                        .append(state.getStarted_at_epoch()).append('|')
                        .append(state.getEnded_at_epoch());
            }
            value.append(';');
        }

        List<GardenSeason> stableSeasons = new ArrayList<>(seasons);
        stableSeasons.sort(Comparator
                .comparing((GardenSeason season) -> safe(season.getZone_id()))
                .thenComparing(season -> safe(season.getSeason_id())));
        for (GardenSeason season : stableSeasons) {
            if (season == null) continue;
            value.append("S|")
                    .append(safe(season.getZone_id())).append('|')
                    .append(safe(season.getSeason_id())).append('|')
                    .append(safe(season.getStatus())).append('|')
                    .append(safe(season.getResult())).append('|')
                    .append(season.getEnded_at_epoch()).append('|')
                    .append(season.getWatering_count()).append('|')
                    .append(season.getFertilizer_application_count()).append('|')
                    .append(season.getJournal_event_count()).append('|')
                    .append(season.getManual_journal_event_count()).append('|')
                    .append(season.getPhoto_count()).append('|')
                    .append(season.getPlant_assistant_analysis_count()).append(';');
        }
        return value.toString();
    }

    private View createZoneCard(GardenZone zone) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(20));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout content = vertical();
        content.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.addView(content);

        LinearLayout heading = horizontal();
        TextView name = text(zoneLabel(zone), 18, R.color.textPrimary, Typeface.BOLD);
        heading.addView(name, weighted());
        ZoneSeasonState state = zone.getSeason();
        boolean preparing = state == null || blank(state.getStatus());
        boolean notStarted = viewModel.isSeasonNotStarted(state);
        boolean active = !preparing && state.isActive();
        int badgeText = preparing
                ? R.string.season_status_preparing
                : (notStarted
                        ? R.string.season_status_not_started
                        : (active ? R.string.season_status_active : R.string.season_status_closed));
        int badgeColor = active ? R.color.online : R.color.textSecondary;
        TextView badge = text(
                getString(badgeText),
                10,
                badgeColor,
                Typeface.BOLD
        );
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        heading.addView(badge);
        content.addView(heading);

        TextView label = text(currentSeasonSummary(zone), 13, R.color.textSecondary, Typeface.NORMAL);
        label.setPadding(0, dp(7), 0, 0);
        content.addView(label);

        List<GardenSeason> history = seasonsFor(zone);
        TextView historySummary = text(historySummary(history), 12, R.color.textSecondary, Typeface.NORMAL);
        historySummary.setPadding(0, dp(8), 0, 0);
        content.addView(historySummary);

        MaterialButton action = new MaterialButton(
                this,
                null,
                active ? com.google.android.material.R.attr.materialButtonOutlinedStyle
                        : com.google.android.material.R.attr.materialButtonStyle
        );
        action.setText(preparing
                ? R.string.season_prepare_action
                : (active ? R.string.season_close_action : R.string.season_start_action));
        action.setEnabled(!preparing);
        action.setAlpha(preparing ? 0.65f : 1f);
        action.setAllCaps(false);
        action.setTextSize(14);
        action.setTypeface(action.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        actionParams.topMargin = dp(13);
        action.setLayoutParams(actionParams);
        action.setOnClickListener(view -> {
            if (preparing) return;
            if (active) showCloseDialog(zone, state, action);
            else showStartDialog(zone, action);
        });
        content.addView(action);

        if (active) {
            MaterialButton cancelNewSeason = new MaterialButton(
                    this,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
            );
            cancelNewSeason.setText(R.string.season_cancel_new_action);
            cancelNewSeason.setAllCaps(false);
            cancelNewSeason.setTextSize(14);
            cancelNewSeason.setTypeface(cancelNewSeason.getTypeface(), Typeface.BOLD);
            cancelNewSeason.setVisibility(View.GONE);
            LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            cancelParams.topMargin = dp(8);
            cancelNewSeason.setLayoutParams(cancelParams);
            cancelNewSeason.setOnClickListener(view -> showCancelNewSeasonDialog(
                    zone,
                    cancelNewSeason
            ));
            content.addView(cancelNewSeason);

            String expectedSeasonId = safe(state.getActive_season_id());
            viewModel.canCancelNewSeason(zone.getZone_id())
                    .addOnSuccessListener(canCancel -> {
                        ZoneSeasonState latest = zone.getSeason();
                        boolean sameSeason = latest != null
                                && latest.isActive()
                                && expectedSeasonId.equals(safe(latest.getActive_season_id()));
                        cancelNewSeason.setVisibility(
                                Boolean.TRUE.equals(canCancel) && sameSeason
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    })
                    .addOnFailureListener(error -> cancelNewSeason.setVisibility(View.GONE));
        }

        MaterialButton archive = new MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        archive.setText(R.string.season_archive_action);
        archive.setAllCaps(false);
        archive.setTextSize(14);
        archive.setTypeface(archive.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams archiveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        archiveParams.topMargin = dp(8);
        archive.setLayoutParams(archiveParams);
        GardenSeason latestCompleted = latestCompletedSeason(history);
        if (latestCompleted != null) {
            archive.setOnClickListener(view -> openSeasonArchive(zone, latestCompleted));
            content.addView(archive);
        }
        return card;
    }

    private View createInactiveZoneCard(GardenZone zone) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(20));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout content = vertical();
        content.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.addView(content);
        GardenSeason archiveIdentity = latestCompletedSeason(seasonsFor(zone), true);

        LinearLayout heading = horizontal();
        heading.addView(text(archiveZoneLabel(zone, archiveIdentity), 18, R.color.textPrimary, Typeface.BOLD), weighted());
        TextView badge = text(
                getString(R.string.season_status_zone_inactive),
                10,
                R.color.textSecondary,
                Typeface.BOLD
        );
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        heading.addView(badge);
        content.addView(heading);

        TextView label = text(currentSeasonSummary(zone), 13, R.color.textSecondary, Typeface.NORMAL);
        label.setPadding(0, dp(7), 0, 0);
        content.addView(label);

        List<GardenSeason> history = seasonsFor(zone);
        TextView historyText = text(historySummary(history, true), 12, R.color.textSecondary, Typeface.NORMAL);
        historyText.setPadding(0, dp(8), 0, 0);
        content.addView(historyText);

        MaterialButton archive = new MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        archive.setText(R.string.season_archive_action);
        archive.setAllCaps(false);
        archive.setTextSize(14);
        archive.setTypeface(archive.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams archiveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        archiveParams.topMargin = dp(13);
        archive.setLayoutParams(archiveParams);
        GardenSeason latestCompleted = latestCompletedSeason(history, true);
        if (latestCompleted != null) {
            archive.setOnClickListener(view -> openSeasonArchive(zone, latestCompleted));
            content.addView(archive);
        }

        MaterialButton reactivate = new MaterialButton(this);
        reactivate.setText(R.string.season_reactivate_zone_action);
        reactivate.setAllCaps(false);
        reactivate.setTextSize(14);
        reactivate.setTypeface(reactivate.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams reactivateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        reactivateParams.topMargin = dp(8);
        reactivate.setLayoutParams(reactivateParams);
        reactivate.setOnClickListener(view -> startActivity(
                new Intent(this, ZoneManagementActivity.class)
        ));
        content.addView(reactivate);
        return card;
    }

    private void openSeasonArchive(GardenZone zone, GardenSeason latest) {
        Intent intent = new Intent(this, PlantTimelineActivity.class);
        intent.putExtra(PlantTimelineActivity.EXTRA_ZONE_ID, zone.getZone_id());
        intent.putExtra(PlantTimelineActivity.EXTRA_INITIAL_TAB, PlantTimelineActivity.TAB_COMPARE);
        if (!blank(latest.getSeason_id())) {
            intent.putExtra(PlantTimelineActivity.EXTRA_SEASON_ID, latest.getSeason_id());
        }
        startActivity(intent);
    }

    private String currentSeasonSummary(GardenZone zone) {
        ZoneSeasonState state = zone.getSeason();
        if (state == null || blank(state.getStatus())) {
            return getString(R.string.season_management_preparing);
        }
        if (viewModel.isSeasonNotStarted(state)) {
            return getString(R.string.season_not_started_summary);
        }
        String label = blank(state.getLabel()) ? getString(R.string.season_label_fallback) : state.getLabel();
        if (state.isActive()) {
            return getString(
                    R.string.season_active_summary,
                    label,
                    formatEpoch(state.getStarted_at_epoch())
            );
        }
        return getString(
                R.string.season_closed_summary,
                label,
                formatEpoch(state.getEnded_at_epoch())
        );
    }

    private String historySummary(List<GardenSeason> history) {
        return historySummary(history, false);
    }

    private String historySummary(List<GardenSeason> history, boolean requireRecordedActivity) {
        List<GardenSeason> completed =
                viewModel.completedArchives(history, requireRecordedActivity);
        int closed = completed.size();
        GardenSeason latestClosed = null;
        for (GardenSeason season : completed) {
            if (latestClosed == null || season.getEnded_at_epoch() > latestClosed.getEnded_at_epoch()) {
                latestClosed = season;
            }
        }
        if (closed == 0) return getString(R.string.season_history_empty);
        String latest = latestClosed == null || blank(latestClosed.getResult())
                ? getString(R.string.season_result_not_entered)
                : latestClosed.getResult();
        return getResources().getQuantityString(R.plurals.season_history_count, closed, closed, latest);
    }

    private GardenSeason latestCompletedSeason(List<GardenSeason> history) {
        return latestCompletedSeason(history, false);
    }

    private GardenSeason latestCompletedSeason(
            List<GardenSeason> history,
            boolean requireRecordedActivity) {
        GardenSeason latest = null;
        for (GardenSeason season :
                viewModel.completedArchives(history, requireRecordedActivity)) {
            if (latest == null || season.getEnded_at_epoch() > latest.getEnded_at_epoch()) {
                latest = season;
            }
        }
        return latest;
    }

    private List<GardenSeason> seasonsFor(GardenZone zone) {
        return viewModel.visibleSeasonsFor(zone, seasons);
    }

    private boolean hasCompletedSeasonArchive(String zoneId) {
        return viewModel.hasRecordedArchive(zoneId, seasons);
    }

    private void showStartDialog(GardenZone zone, MaterialButton action) {
        LinearLayout form = dialogForm();

        Spinner crop = spinnerWithLabel(form, R.string.season_start_crop_label);
        List<CropCatalogItem> cropChoices = new ArrayList<>(cropCatalogItems.isEmpty()
                ? viewModel.mergedCrops(null)
                : cropCatalogItems);
        List<String> cropLabels = new ArrayList<>();
        for (CropCatalogItem item : cropChoices) cropLabels.add(item.toString());
        crop.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                cropLabels
        ));
        int initialCropPosition = cropPosition(
                cropChoices,
                zone.getPlant_type(),
                zone.getName()
        );
        crop.setSelection(initialCropPosition);
        EditText date = field(R.string.season_start_planting_date_hint, InputType.TYPE_CLASS_DATETIME);
        date.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
        date.setFocusable(false);
        date.setClickable(true);
        date.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0);
        date.setCompoundDrawablePadding(dp(8));
        date.setOnClickListener(view -> showDatePicker(date));
        EditText label = field(R.string.season_start_label_hint, InputType.TYPE_CLASS_TEXT);
        label.setText(getString(
                R.string.season_default_label,
                new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date())
        ));
        form.addView(date);
        form.addView(label);

        Spinner stage = spinnerWithLabel(form, R.string.season_start_stage_label);
        stage.setAdapter(ArrayAdapter.createFromResource(
                this,
                R.array.season_growth_stage_labels,
                android.R.layout.simple_spinner_dropdown_item
        ));

        String valve = blank(zone.getValve_id())
                ? getString(R.string.season_start_no_valve)
                : zone.getValve_id();
        TextView valveNote = text(
                getString(R.string.season_start_valve_note, valve),
                12,
                R.color.textSecondary,
                Typeface.NORMAL
        );
        valveNote.setPadding(0, dp(2), 0, dp(8));
        form.addView(valveNote);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.season_start_dialog_title, safe(zone.getName())))
                .setMessage(R.string.season_start_dialog_message)
                .setView(scroll)
                .setNegativeButton(R.string.season_cancel, null)
                .setPositiveButton(R.string.season_start_action, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (!validDate(value(date))) {
                        Toast.makeText(this, R.string.season_invalid_date, Toast.LENGTH_LONG).show();
                        return;
                    }
                    int cropIndex = Math.min(crop.getSelectedItemPosition(), cropChoices.size() - 1);
                    CropCatalogItem selectedCrop = cropChoices.get(cropIndex);
                    String cropName = selectedCrop.getName();
                    if (blank(cropName)) {
                        Toast.makeText(this, R.string.season_crop_required, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String plantType = selectedCrop.getPlant_type();
                    String emoji = selectedCrop.getEmoji();

                    String[] codes = getResources().getStringArray(R.array.season_growth_stage_codes);
                    String code = codes[Math.min(stage.getSelectedItemPosition(), codes.length - 1)];
                    setBusy(action, true);
                    dialog.dismiss();
                    viewModel.startSeason(
                                    zone,
                                    value(date),
                                    code,
                                    value(label),
                                    cropName,
                                    plantType,
                                    emoji
                            )
                            .addOnSuccessListener(result -> {
                                setBusy(action, false);
                                Toast.makeText(this, R.string.season_started_success, Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(error -> {
                                setBusy(action, false);
                                showError(error, R.string.season_start_failed);
                            });
                }));
        dialog.show();
    }

    private Spinner spinnerWithLabel(LinearLayout form, int labelRes) {
        TextView label = text(getString(labelRes), 13, R.color.textSecondary, Typeface.BOLD);
        label.setPadding(0, dp(5), 0, dp(3));
        form.addView(label);
        Spinner spinner = new Spinner(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        params.bottomMargin = dp(10);
        spinner.setLayoutParams(params);
        form.addView(spinner);
        return spinner;
    }

    private static int cropPosition(
            List<CropCatalogItem> choices,
            String currentCode,
            String currentName
    ) {
        String expected = safe(currentCode).trim();
        String expectedName = safe(currentName).trim();
        for (int i = 0; i < choices.size(); i++) {
            CropCatalogItem item = choices.get(i);
            if (safe(item.getPlant_type()).equalsIgnoreCase(expected)
                    || safe(item.getCrop_id()).equalsIgnoreCase(expected)
                    || safe(item.getName()).equalsIgnoreCase(expectedName)) {
                return i;
            }
        }
        return 0;
    }

    private void showCancelNewSeasonDialog(GardenZone zone, MaterialButton action) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.season_cancel_new_dialog_title, safe(zone.getName())))
                .setMessage(R.string.season_cancel_new_dialog_message)
                .setNegativeButton(R.string.season_cancel, null)
                .setPositiveButton(R.string.season_cancel_new_confirm, (dialog, which) -> {
                    setBusy(action, true);
                    viewModel.cancelNewSeason(zone.getZone_id())
                            .addOnSuccessListener(ignored -> {
                                setBusy(action, false);
                                lastRenderSignature = "";
                                renderIfChanged();
                                Toast.makeText(
                                        this,
                                        R.string.season_cancel_new_success,
                                        Toast.LENGTH_LONG
                                ).show();
                            })
                            .addOnFailureListener(error -> {
                                setBusy(action, false);
                                showError(error, R.string.season_cancel_new_failed);
                            });
                })
                .show();
    }

    private void showCloseDialog(GardenZone zone, ZoneSeasonState state, MaterialButton action) {
        LinearLayout form = dialogForm();
        Spinner result = new Spinner(this);
        ArrayAdapter<CharSequence> resultAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.season_result_labels,
                android.R.layout.simple_spinner_dropdown_item
        );
        result.setAdapter(resultAdapter);
        EditText harvest = field(R.string.season_harvest_amount_hint, InputType.TYPE_CLASS_TEXT);
        EditText yield = field(R.string.season_yield_note_hint, multiline());
        EditText issues = field(R.string.season_issues_hint, multiline());
        EditText practices = field(R.string.season_practices_hint, multiline());
        EditText next = field(R.string.season_next_note_hint, multiline());
        form.addView(result);
        form.addView(harvest);
        form.addView(yield);
        form.addView(issues);
        form.addView(practices);
        form.addView(next);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.season_close_dialog_title, safe(zone.getName())))
                .setMessage(R.string.season_close_dialog_message)
                .setView(form)
                .setNegativeButton(R.string.season_cancel, null)
                .setPositiveButton(R.string.season_close_action, (dialog, which) -> {
                    SeasonOutcome outcome = new SeasonOutcome();
                    outcome.setId(state.getActive_season_id());
                    outcome.setSeason_id(state.getActive_season_id());
                    outcome.setZone_id(zone.getZone_id());
                    outcome.setResult(String.valueOf(result.getSelectedItem()));
                    outcome.setHarvest_amount(value(harvest));
                    outcome.setYield_note(value(yield));
                    outcome.setIssues_note(value(issues));
                    outcome.setSuccessful_practices(value(practices));
                    outcome.setNext_season_note(value(next));
                    outcome.setRecorded_at_epoch(System.currentTimeMillis() / 1000L);
                    setBusy(action, true);
                    viewModel.closeSeason(zone, state, outcome)
                            .addOnSuccessListener(ignored -> {
                                setBusy(action, false);
                                Toast.makeText(this, R.string.season_closed_success, Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(error -> {
                                setBusy(action, false);
                                showError(error, R.string.season_close_failed);
                            });
                })
                .show();
    }

    private void setBusy(MaterialButton action, boolean busy) {
        if (busy) {
            if (action.getTag() == null) {
                action.setTag(action.getText());
            }
            action.setEnabled(false);
            action.setText(R.string.season_operation_wait);
            return;
        }

        action.setEnabled(true);
        Object originalText = action.getTag();
        if (originalText instanceof CharSequence) {
            action.setText((CharSequence) originalText);
        }
        action.setTag(null);
    }

    private LinearLayout dialogForm() {
        LinearLayout form = vertical();
        int padding = dp(20);
        form.setPadding(padding, dp(4), padding, 0);
        return form;
    }

    private EditText field(int hintRes, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hintRes);
        input.setInputType(inputType);
        input.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(10);
        input.setLayoutParams(params);
        return input;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return layout;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(ContextCompat.getColor(this, color));
        view.setTypeface(view.getTypeface(), style);
        return view;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private String zoneLabel(GardenZone zone) {
        String emoji = safe(zone.getEmoji()).trim();
        return (emoji.isEmpty() ? "" : emoji + " ") + safe(zone.getName());
    }

    private String archiveZoneLabel(GardenZone zone, GardenSeason season) {
        if (season == null) return zoneLabel(zone);
        String emoji = blank(season.getEmoji()) ? safe(zone.getEmoji()).trim() : season.getEmoji().trim();
        String name = blank(season.getZone_name()) ? safe(zone.getName()) : season.getZone_name().trim();
        return (emoji.isEmpty() ? "" : emoji + " ") + name;
    }

    private String formatEpoch(long epoch) {
        if (epoch <= 0L) return getString(R.string.season_date_unknown);
        return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(epoch * 1000L));
    }

    private void showDatePicker(EditText target) {
        Calendar selected = Calendar.getInstance();
        Date parsed = parseSupportedDate(value(target));
        if (parsed != null) selected.setTime(parsed);
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> target.setText(String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%04d",
                        day,
                        month + 1,
                        year
                )),
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private Date parseSupportedDate(String rawDate) {
        if (blank(rawDate)) return null;
        String[] patterns = {"dd-MM-yyyy", "yyyy-MM-dd", "dd.MM.yyyy"};
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
            format.setLenient(false);
            try {
                return format.parse(rawDate.trim());
            } catch (Exception ignored) {
                // Try the next supported legacy format.
            }
        }
        return null;
    }

    private boolean validDate(String date) {
        return parseSupportedDate(date) != null;
    }

    private void showError(Exception error, int fallback) {
        String message = error == null || blank(error.getMessage()) ? getString(fallback) : error.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int multiline() {
        return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
    }

    private static String value(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
