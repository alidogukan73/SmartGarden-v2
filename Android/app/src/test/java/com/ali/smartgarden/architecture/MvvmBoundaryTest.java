package com.ali.smartgarden.architecture;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Prevents persistence and backend dependencies from leaking back into Activities. */
public final class MvvmBoundaryTest {
    private static final List<String> FORBIDDEN_ACTIVITY_TOKENS = Arrays.asList(
            "com.ali.smartgarden.firebase.",
            "com.google.firebase.",
            "android.content.SharedPreferences",
            "getSharedPreferences(",
            "com.ali.smartgarden.backup.AvoraBackupManager",
            "com.ali.smartgarden.journal.LocalGardenEventStore",
            "com.ali.smartgarden.photos.LocalGardenPhotoStore",
            "com.ali.smartgarden.season.SeasonRepository",
            "com.ali.smartgarden.notifications.GardenNotificationManager",
            "com.ali.smartgarden.notifications.NotificationSettingsStore",
            "com.ali.smartgarden.fertilization.FertilizationPreferenceStore",
            "com.ali.smartgarden.settings.GardenProfileStore",
            "com.ali.smartgarden.settings.UnitPreferences",
            "com.ali.smartgarden.language.AvoraLanguageManager",
            "com.ali.smartgarden.theme.AvoraThemeManager",
            "com.ali.smartgarden.sync.DataSyncRepository",
            "com.ali.smartgarden.calibration.SensorCalibrationSampler",
            "com.ali.smartgarden.crop.CropCatalog",
            "com.ali.smartgarden.zones.ZoneCapacityPolicy",
            "com.ali.smartgarden.season.SeasonScope",
            "com.ali.smartgarden.season.SeasonStartConfiguration",
            "com.ali.smartgarden.plantassistant.PlantFollowUpStore",
            "com.ali.smartgarden.plantassistant.PlantAssistantRecommendationStore",
            "NotificationSignalScheduler.schedule(",
            "FertilizerReminderScheduler.schedule("
    );

    private static final Set<String> VIEWMODEL_FREE_UI_ONLY = new HashSet<>(Arrays.asList(
            "HelpCenterActivity.java"
    ));

    @Test
    public void activitiesDoNotAccessPersistenceOrFirebaseDirectly() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path file : activityFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String token : FORBIDDEN_ACTIVITY_TOKENS) {
                if (source.contains(token)) {
                    violations.add(file.getFileName() + " -> " + token);
                }
            }
            for (String line : source.split("\\R")) {
                String value = line.trim();
                if (value.startsWith("import com.ali.smartgarden.")
                        && (value.endsWith("Repository;")
                        || value.endsWith("Store;")
                        || value.endsWith("Manager;")
                        || value.endsWith("Scheduler;")
                        || value.endsWith("Advisor;")
                        || value.endsWith("Policy;")
                        || value.endsWith("Engine;")
                        || value.endsWith("Coordinator;")
                        || value.endsWith("ApplicationSafety;"))) {
                    violations.add(file.getFileName() + " -> " + value);
                }
            }
            if (source.contains("new Thread(")) {
                violations.add(file.getFileName() + " -> direct background thread");
            }
            if (source.contains("getContentResolver().openOutputStream(")) {
                violations.add(file.getFileName() + " -> direct document write");
            }
            if (source.contains("android.location.LocationManager")) {
                violations.add(file.getFileName() + " -> direct location data access");
            }
        }
        assertTrue("MVVM data boundary violations: " + violations, violations.isEmpty());
    }

    @Test
    public void everyStatefulActivityUsesAViewModel() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path file : activityFiles()) {
            String name = file.getFileName().toString();
            if (VIEWMODEL_FREE_UI_ONLY.contains(name)) continue;
            String source = Files.readString(file, StandardCharsets.UTF_8);
            if (!source.contains("new ViewModelProvider(")) violations.add(name);
        }
        assertTrue("Stateful Activities without a ViewModel: " + violations,
                violations.isEmpty());
    }

    @Test
    public void viewModelsDoNotAccessFirebaseOrUiFrameworkDirectly() throws Exception {
        List<String> violations = new ArrayList<>();
        List<String> forbidden = Arrays.asList(
                "com.google.firebase.",
                "android.app.Activity",
                "android.view.",
                "android.widget.",
                "androidx.appcompat.",
                "androidx.fragment."
        );
        for (Path file : viewModelFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String token : forbidden) {
                if (source.contains(token)) {
                    violations.add(file.getFileName() + " -> " + token);
                }
            }
        }
        assertTrue("ViewModels with backend or UI framework access: " + violations,
                violations.isEmpty());
    }

    private static List<Path> activityFiles() throws IOException {
        Path root = Paths.get("src/main/java/com/ali/smartgarden/activities");
        if (!Files.isDirectory(root)) {
            root = Paths.get("app/src/main/java/com/ali/smartgarden/activities");
        }
        assertTrue("Activity source directory not found: " + root.toAbsolutePath(),
                Files.isDirectory(root));
        try (Stream<Path> paths = Files.list(root)) {
            List<Path> files = new ArrayList<>();
            paths.filter(path -> path.getFileName().toString().endsWith("Activity.java"))
                    .sorted()
                    .forEach(files::add);
            return files;
        }
    }

    private static List<Path> viewModelFiles() throws IOException {
        Path root = Paths.get("src/main/java/com/ali/smartgarden/viewmodels");
        if (!Files.isDirectory(root)) {
            root = Paths.get("app/src/main/java/com/ali/smartgarden/viewmodels");
        }
        assertTrue("ViewModel source directory not found: " + root.toAbsolutePath(),
                Files.isDirectory(root));
        try (Stream<Path> paths = Files.list(root)) {
            List<Path> files = new ArrayList<>();
            paths.filter(path -> path.getFileName().toString().endsWith("ViewModel.java"))
                    .sorted()
                    .forEach(files::add);
            return files;
        }
    }
}
