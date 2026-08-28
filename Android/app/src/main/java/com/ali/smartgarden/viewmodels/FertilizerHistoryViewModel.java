package com.ali.smartgarden.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.ali.smartgarden.fertilization.FertilizationRepository;
import com.ali.smartgarden.fertilization.FertilizerOutcomeFollowUpPolicy;
import com.ali.smartgarden.models.FertilizerApplication;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class FertilizerHistoryViewModel extends AndroidViewModel {
    private final FertilizationRepository repository = new FertilizationRepository();
    private final LiveData<List<FertilizerApplication>> history = repository.observeHistory();
    private final LiveData<List<GardenZone>> zones = repository.observeZones();
    private final GardenNotificationManager notifications;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FertilizerHistoryViewModel(@NonNull Application application) {
        super(application);
        notifications = new GardenNotificationManager(application);
    }

    public LiveData<List<FertilizerApplication>> getHistory() { return history; }
    public LiveData<List<GardenZone>> getZones() { return zones; }

    public Task<Void> update(FertilizerApplication application) {
        return repository.updateApplication(application);
    }

    public Task<Void> delete(FertilizerApplication application) {
        return repository.deleteApplication(application);
    }

    public void markNotificationRead(String id) {
        notifications.markRead(id);
    }

    public void writeCsv(Uri uri, String contents, Consumer<Boolean> completed) {
        executor.execute(() -> {
            boolean successful = false;
            try (OutputStream output = getApplication().getContentResolver()
                    .openOutputStream(uri, "wt")) {
                if (output != null) {
                    output.write(contents.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    successful = true;
                }
            } catch (Exception ignored) {
                successful = false;
            }
            completed.accept(successful);
        });
    }

    public OutcomeSummary summarizeOutcomes(List<FertilizerApplication> values) {
        int observed = 0;
        int improved = 0;
        int unchanged = 0;
        int issue = 0;
        double vigorTotal = 0.0;
        int vigorCount = 0;
        Map<String, Integer> pairCounts = new HashMap<>();
        if (values != null) {
            for (FertilizerApplication value : values) {
                if (!FertilizerOutcomeFollowUpPolicy.isEvaluated(value)) continue;
                observed++;
                String pairKey = safe(value.getZone_id()) + "|" + safe(value.getProduct_id());
                pairCounts.put(pairKey, pairCounts.getOrDefault(pairKey, 0) + 1);
                String status = safe(value.getOutcome_status());
                if ("IMPROVED".equals(status)) improved++;
                else if ("ISSUE".equals(status)) issue++;
                else unchanged++;
                if (value.getOutcome_vigor_score() > 0) {
                    vigorTotal += value.getOutcome_vigor_score();
                    vigorCount++;
                }
            }
        }
        int target = FertilizerOutcomeFollowUpPolicy.RELIABLE_OBSERVATION_COUNT;
        int learnedPairs = 0;
        int strongestPair = 0;
        for (int pairCount : pairCounts.values()) {
            strongestPair = Math.max(strongestPair, pairCount);
            if (pairCount >= target) learnedPairs++;
        }
        double averageVigor = vigorCount == 0 ? 0.0 : vigorTotal / vigorCount;
        return new OutcomeSummary(observed, improved, unchanged, issue, vigorCount,
                averageVigor, target, learnedPairs, strongestPair);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class OutcomeSummary {
        public final int observed;
        public final int improved;
        public final int unchanged;
        public final int issue;
        public final int vigorCount;
        public final double averageVigor;
        public final int target;
        public final int learnedPairs;
        public final int strongestPair;

        OutcomeSummary(int observed, int improved, int unchanged, int issue,
                       int vigorCount, double averageVigor, int target,
                       int learnedPairs, int strongestPair) {
            this.observed = observed;
            this.improved = improved;
            this.unchanged = unchanged;
            this.issue = issue;
            this.vigorCount = vigorCount;
            this.averageVigor = averageVigor;
            this.target = target;
            this.learnedPairs = learnedPairs;
            this.strongestPair = strongestPair;
        }
    }

    @Override protected void onCleared() {
        executor.shutdown();
    }
}
