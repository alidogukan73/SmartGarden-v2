package com.ali.smartgarden.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.smartgarden.R;
import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.WeatherForecast;
import com.ali.smartgarden.photos.LocalGardenPhotoStore;
import com.ali.smartgarden.plantassistant.PlantAssistantAdvisor;
import com.ali.smartgarden.plantassistant.PlantAssistantRecommendationStore;
import com.ali.smartgarden.plantassistant.PlantAssistantResult;
import com.ali.smartgarden.plantassistant.PlantAssistantVisionClient;
import com.ali.smartgarden.plantassistant.PlantFollowUpStore;
import com.ali.smartgarden.journal.LocalGardenEventStore;
import com.ali.smartgarden.models.GardenEvent;
import com.ali.smartgarden.notifications.GardenNotificationManager;
import com.ali.smartgarden.ui.PrimaryBottomNavigation;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** AI Bitki Asistanı: fotoğraf, belirtiler, sensör ve hava bağlamıyla güvenli ön değerlendirme. */
public class PlantAssistantActivity extends AppCompatActivity {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final Map<String, GardenZone> zones = new HashMap<>();

    private MaterialAutoCompleteTextView zoneDropdown;
    private MaterialCardView resultCard;
    private TextView title, meta, context, advice;
    private TextView soilData, weatherTemperatureData, sunData, windData, humidityData;
    private ImageView photoPreview;
    private View photoHintLayout, otherNoteLayout;
    private TextInputEditText generalNote, otherNote;
    private CheckBox yellowing, drying, spot, wilt, pest, flowerDrop, other;
    private String requestedZoneId = "";
    private Uri selectedPhotoUri;
    private Bitmap selectedPhotoBitmap;
    private WeatherForecast currentWeather;
    private LocalGardenPhotoStore photoStore;
    private PlantFollowUpStore followUpStore;
    private LocalGardenEventStore gardenEventStore;
    private boolean selectedPhotoArchived;
    private String archivedPhotoId = "";
    private AnalysisSnapshot pendingAnalysis;

    private final ActivityResultLauncher<Void> camera =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) showPhoto(bitmap);
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) showPhoto(uri);
            });
    private final ActivityResultLauncher<Intent> journalPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                String path = result.getData().getStringExtra(GardenPhotoGalleryActivity.EXTRA_SELECTED_PHOTO_PATH);
                String photoId = result.getData().getStringExtra(GardenPhotoGalleryActivity.EXTRA_SELECTED_PHOTO_ID);
                if (path != null && !path.isBlank()) {
                    showPhoto(Uri.fromFile(new java.io.File(path)));
                    archivedPhotoId = photoId == null ? "" : photoId;
                    selectedPhotoArchived = !archivedPhotoId.isBlank();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_plant_assistant);
        PrimaryBottomNavigation.bind(this, PrimaryBottomNavigation.ASSISTANT);
        requestedZoneId = getIntent().getStringExtra("zone_id");
        photoStore = new LocalGardenPhotoStore(this);
        followUpStore = new PlantFollowUpStore(this);
        gardenEventStore = new LocalGardenEventStore(this);
        bindViews();
        bindActions();
        repository.observeGardenZones().observe(this, this::renderZones);
        repository.observeWeatherForecast().observe(this, weather -> {
            currentWeather = weather;
            renderLiveData(selectedZone());
        });
    }

    private void bindViews() {
        zoneDropdown = findViewById(R.id.dropdownDoctorZone);
        resultCard = findViewById(R.id.cardDoctorResult);
        title = findViewById(R.id.txtDoctorTitle);
        meta = findViewById(R.id.txtDoctorMeta);
        context = findViewById(R.id.txtDoctorContext);
        advice = findViewById(R.id.txtDoctorAdvice);
        photoHintLayout = findViewById(R.id.layoutDoctorPhotoHint);
        photoPreview = findViewById(R.id.imgDoctorPhoto);
        generalNote = findViewById(R.id.inputDoctorNote);
        otherNoteLayout = findViewById(R.id.layoutDoctorOtherNote);
        otherNote = findViewById(R.id.inputDoctorOtherNote);
        soilData = findViewById(R.id.txtDoctorSoil);
        weatherTemperatureData = findViewById(R.id.txtDoctorWeatherTemp);
        sunData = findViewById(R.id.txtDoctorSun);
        windData = findViewById(R.id.txtDoctorWind);
        humidityData = findViewById(R.id.txtDoctorHumidity);
        yellowing = findViewById(R.id.checkDoctorYellowing);
        drying = findViewById(R.id.checkDoctorDrying);
        spot = findViewById(R.id.checkDoctorSpot);
        wilt = findViewById(R.id.checkDoctorWilt);
        pest = findViewById(R.id.checkDoctorPest);
        flowerDrop = findViewById(R.id.checkDoctorFlowerDrop);
        other = findViewById(R.id.checkDoctorOther);
    }

    private void bindActions() {
        findViewById(R.id.btnDoctorBack).setOnClickListener(view -> finish());
        findViewById(R.id.cardDoctorPhotoPicker).setOnClickListener(view -> showPhotoSourceDialog());
        findViewById(R.id.btnAnalyzePlant).setOnClickListener(view -> analyze());
        other.setOnCheckedChangeListener((button, checked) -> {
            otherNoteLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) otherNote.setText("");
        });
    }

    private void showPhotoSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Fotoğraf ekle")
                .setItems(new String[]{"Fotoğraf çek", "Galeriden seç", "Bitki Günlüğünden seç"}, (dialog, which) -> {
                    if (which == 0) {
                        camera.launch(null);
                    } else if (which == 1) {
                        photoPicker.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                    } else {
                        Intent intent = new Intent(this, GardenPhotoGalleryActivity.class);
                        intent.putExtra(GardenPhotoGalleryActivity.EXTRA_PICK_MODE, true);
                        journalPhotoPicker.launch(intent);
                    }
                })
                .show();
    }

    private void renderZones(List<GardenZone> items) {
        zones.clear();
        List<String> labels = new ArrayList<>();
        GardenZone requested = null;
        if (items != null) for (GardenZone zone : items) {
            String label = labelFor(zone);
            labels.add(label);
            zones.put(label, zone);
            if (zone.getZone_id().equals(requestedZoneId)) requested = zone;
        }
        zoneDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        zoneDropdown.setOnItemClickListener((parent, view, position, id) -> {
            resultCard.setVisibility(View.GONE);
            renderLiveData(selectedZone());
        });
        if (requested != null) {
            zoneDropdown.setText(labelFor(requested), false);
            renderLiveData(requested);
        } else if (!labels.isEmpty() && selectedZone() == null) {
            zoneDropdown.setText(labels.get(0), false);
            renderLiveData(zones.get(labels.get(0)));
        }
    }

    private void analyze() {
        GardenZone zone = selectedZone();
        List<String> symptoms = selectedSymptoms();
        if (zone == null) {
            toast("Önce bir bölge seçin.");
            return;
        }
        if (symptoms.isEmpty()) {
            toast("En az bir belirti seçin.");
            return;
        }
        String note = text(generalNote);
        PlantAssistantResult result = PlantAssistantAdvisor.assess(zone, symptoms, note, currentWeather, hasPhoto());
        renderHeuristicResult(result);
        if (hasPhoto()) requestVisionAnalysis(zone, symptoms, note);
        savePhotoToArchive(zone, symptoms, note);
    }

    private void renderHeuristicResult(PlantAssistantResult result) {
        title.setText(result.getTitle());
        meta.setText("Olasılık: " + result.getProbability() + " · Aciliyet: " + result.getUrgency());
        context.setText("Değerlendirilen bahçe verisi: " + result.getContext());
        advice.setText(result.getAdvice());
        GardenZone zone = selectedZone();
        PlantAssistantRecommendationStore.save(
                this,
                zone == null ? "" : zone.getZone_id(),
                result.getUrgency(),
                result.getTitle(),
                result.getAdvice()
        );
        resultCard.setVisibility(View.VISIBLE);
        archiveAnalysis(
                result.getTitle(),
                "Olasılık: " + result.getProbability() + " · Aciliyet: " + result.getUrgency(),
                result.getContext(),
                result.getAdvice()
        );
    }

    private void requestVisionAnalysis(GardenZone zone, List<String> symptoms, String note) {
        Bitmap bitmap = selectedPhotoBitmap;
        if (bitmap == null && photoPreview.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) photoPreview.getDrawable()).getBitmap();
        }
        if (bitmap == null) return;
        Bitmap image = bitmap;
        toast("Görsel AI analizi hazırlanıyor...");
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("plant", zone.getName());
                payload.put("zone", zone.getZone_id());
                payload.put("moisture", zone.getMoisture());
                payload.put("moisture_limit", zone.getMoisture_limit());
                JSONArray symptomValues = new JSONArray();
                for (String symptom : symptoms) symptomValues.put(symptom);
                payload.put("symptoms", symptomValues);
                payload.put("note", note);
                if (currentWeather != null) {
                    payload.put("temperature", currentWeather.getCurrentTemperature());
                    payload.put("humidity", currentWeather.getCurrentHumidity());
                    payload.put("rain_probability", currentWeather.getTodayRainProbability());
                }
                JSONObject visual = PlantAssistantVisionClient.analyze(image, payload);
                runOnUiThread(() -> renderVisionResult(visual));
            } catch (Exception error) {
                runOnUiThread(() -> toast("Görsel AI servisine ulaşılamadı: " + error.getMessage()));
            }
        }).start();
    }

    private void renderVisionResult(JSONObject visual) {
        if (!visual.optBoolean("is_plant_photo", true)) {
            title.setText("Fotoğraf bitkiyi yeterince göstermiyor");
            meta.setText("Görsel güven: %" + visual.optInt("confidence", 0) + " · Aciliyet: Düşük");
            context.setText("Yaprak veya belirtili bölge net seçilemedi. Yakın plan, gün ışığında ve tek yaprağa odaklı yeni bir fotoğraf ekleyin.");
            advice.setText("Bu fotoğraf için hastalık değerlendirmesi yapılmadı. Bitkiyi tekrar fotoğraflayıp analizi yenileyin.");
            resultCard.setVisibility(View.VISIBLE);
            archiveAnalysis(String.valueOf(title.getText()), String.valueOf(meta.getText()),
                    String.valueOf(context.getText()), String.valueOf(advice.getText()));
            return;
        }
        String findings = visual.optString("visual_findings", "Görsel bulgu üretilemedi.");
        String causes = PlantAssistantVisionClient.list(visual.optJSONArray("possible_causes"));
        String steps = PlantAssistantVisionClient.list(visual.optJSONArray("next_steps"));
        String redFlags = PlantAssistantVisionClient.list(visual.optJSONArray("red_flags"));
        PlantAssistantRecommendationStore.save(
                this,
                selectedZone() == null ? "" : selectedZone().getZone_id(),
                visual.optString("urgency", "Düşük"),
                visual.optString("title", "Görsel ön değerlendirme"),
                steps.isEmpty() ? findings : steps
        );
        title.setText(visual.optString("title", "Görsel ön değerlendirme"));
        meta.setText("Görsel güven: %" + visual.optInt("confidence", 0)
                + " · Aciliyet: " + visual.optString("urgency", "Düşük"));
        context.setText(findings + (causes.isEmpty() ? "" : "\n\nOlası nedenler\n" + causes));
        advice.setText((steps.isEmpty() ? "" : "Önerilen gözlem\n" + steps + "\n\n")
                + (redFlags.isEmpty() ? "" : "Dikkat edilmesi gerekenler\n" + redFlags + "\n\n")
                + visual.optString("disclaimer", "Bu sonuç kesin teşhis değildir."));
        resultCard.setVisibility(View.VISIBLE);
        archiveAnalysis(String.valueOf(title.getText()), String.valueOf(meta.getText()),
                String.valueOf(context.getText()), String.valueOf(advice.getText()));
    }

    private List<String> selectedSymptoms() {
        List<String> items = new ArrayList<>();
        if (yellowing.isChecked()) items.add("Alt yapraklarda sararma");
        if (drying.isChecked()) items.add("Yaprak kuruması");
        if (spot.isChecked()) items.add("Yaprakta leke / yanıklık");
        if (wilt.isChecked()) items.add("Solma");
        if (pest.isChecked()) items.add("Meyve çatlaması");
        if (flowerDrop.isChecked()) items.add("Çiçek dökümü");
        if (other.isChecked()) items.add(text(otherNote).isEmpty() ? "Diğer gözlem" : text(otherNote));
        return items;
    }

    private void renderLiveData(GardenZone zone) {
        if (zone == null) return;
        soilData.setText(getString(R.string.format_assistant_soil_data, "%" + zone.getMoisture()));
        weatherTemperatureData.setText("🌡\nHava sıcaklığı\n" + number(currentWeather == null ? null : currentWeather.getCurrentTemperature(), "°C"));
        sunData.setText(getString(R.string.format_assistant_sun_data, sunLabel(currentWeather)));
        windData.setText(getString(R.string.symbol_wind) + "\nRüzgar\n" + number(currentWeather == null ? null : currentWeather.getCurrentWind(), " km/sa"));
        humidityData.setText(getString(R.string.format_assistant_humidity_data, number(currentWeather == null ? null : currentWeather.getCurrentHumidity(), "%")));
    }

    private void savePhotoToArchive(GardenZone zone, List<String> symptoms, String note) {
        if (!hasPhoto() || selectedPhotoArchived) return;
        selectedPhotoArchived = true;
        String archiveNote = "Bitki Asistanı · " + String.join(", ", symptoms)
                + (note.isEmpty() ? "" : " · " + note);
        Uri uri = selectedPhotoUri;
        Bitmap bitmap = selectedPhotoBitmap;
        new Thread(() -> {
            try {
                com.ali.smartgarden.models.GardenPhoto archived = null;
                if (uri != null) archived = photoStore.save(uri, zone.getZone_id(), archiveNote, "plant_assistant");
                else if (bitmap != null) archived = photoStore.save(bitmap, zone.getZone_id(), archiveNote, "plant_assistant");
                com.ali.smartgarden.models.GardenPhoto saved = archived;
                runOnUiThread(() -> {
                    if (saved != null) {
                        archivedPhotoId = saved.getId();
                        applyPendingAnalysis();
                    }
                    toast("Fotoğraf ve analiz kaydı Bitki Günlüğü'ne eklendi.");
                });
            } catch (Exception ignored) {
                selectedPhotoArchived = false;
                runOnUiThread(() -> toast("Fotoğraf analize eklendi, ancak arşive kaydedilemedi."));
            }
        }).start();
    }

    private void archiveAnalysis(String analysisTitle, String analysisMeta,
                                 String analysisContext, String analysisAdvice) {
        if (!hasPhoto()) return;
        GardenZone zone = selectedZone();
        pendingAnalysis = new AnalysisSnapshot(analysisTitle, analysisMeta, analysisContext, analysisAdvice,
                zone == null ? "" : zone.getZone_id());
        applyPendingAnalysis();
    }

    private void applyPendingAnalysis() {
        if (pendingAnalysis == null || archivedPhotoId.isBlank()) return;
        AnalysisSnapshot snapshot = pendingAnalysis;
        pendingAnalysis = null;
        new Thread(() -> {
            PlantFollowUpStore.Result followUp = followUpStore.registerAnalysis(snapshot.zoneId, archivedPhotoId, snapshot.title);
            String contextText = snapshot.context;
            if ("SCHEDULED".equals(followUp.type) || "SCHEDULED_EXISTING".equals(followUp.type)) contextText += "\n\nTakip görevi: 3 gün sonra aynı bölgeden yeni fotoğraf ekleyin.";
            else if ("COMPLETED".equals(followUp.type)) contextText += "\n\nTakip değerlendirmesi: Önceki analiz ('" + followUp.previousTitle + "') ile yeni fotoğraf karşılaştırılmak üzere kaydedildi.";
            com.ali.smartgarden.models.GardenPhoto updated = photoStore.updateAnalysis(archivedPhotoId,
                    snapshot.title, snapshot.meta, contextText, snapshot.advice);
            if (updated != null) {
                repository.saveGardenPhotoMetadata(updated);
                GardenNotificationManager notifications = new GardenNotificationManager(this);
                notifications.publishOnce("PLANT_ASSISTANT", "HIGH", snapshot.zoneId, snapshot.title,
                        "Bitki Asistanı analizi kaydedildi. Önerileri inceleyin.", "plant_analysis:" + archivedPhotoId);
                GardenEvent event;
                if ("SCHEDULED".equals(followUp.type)) {
                    event = gardenEventStore.addAutomaticOncePerDay(snapshot.zoneId, "Takip fotoğrafı önerisi", "Bitki Asistanı analizinden 3 gün sonra aynı bölgeden yeni fotoğraf ekleyin.", "follow_up_" + archivedPhotoId);
                    if (event != null) repository.saveGardenEvent(event);
                } else if ("COMPLETED".equals(followUp.type)) {
                    event = gardenEventStore.addAutomaticOncePerDay(snapshot.zoneId, "Takip değerlendirmesi", "Yeni analiz, önceki Bitki Asistanı analiziyle karşılaştırılmak üzere kaydedildi.", "follow_up_" + archivedPhotoId);
                    if (event != null) repository.saveGardenEvent(event);
                    notifications.publishOnce("PLANT_ASSISTANT", "NORMAL", snapshot.zoneId, "Takip değerlendirmesi hazır",
                            "Yeni analiz önceki Bitki Asistanı analiziyle ilişkilendirildi.", "follow_up_complete:" + archivedPhotoId);
                }
            }
        }).start();
    }

    private static final class AnalysisSnapshot {
        final String title, meta, context, advice, zoneId;

        AnalysisSnapshot(String title, String meta, String context, String advice, String zoneId) {
            this.title = title;
            this.meta = meta;
            this.context = context;
            this.advice = advice;
            this.zoneId = zoneId;
        }
    }

    private GardenZone selectedZone() { return zones.get(String.valueOf(zoneDropdown.getText())); }
    private boolean hasPhoto() { return selectedPhotoUri != null || selectedPhotoBitmap != null; }
    private String labelFor(GardenZone zone) { return (zone.getEmoji() == null ? getString(R.string.symbol_plant) : zone.getEmoji()) + " " + zone.getName(); }
    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private String number(Double value, String suffix) { return value == null ? "—" : Math.round(value) + suffix; }
    private String sunLabel(WeatherForecast weather) {
        if (weather == null || weather.getCurrentWeatherCode() == null) return "—";
        return weather.getCurrentWeatherCode() <= 1 ? "Kuvvetli" : weather.getCurrentWeatherCode() <= 3 ? "Orta" : "Düşük";
    }

    private void showPhoto(Uri uri) {
        selectedPhotoArchived = false;
        archivedPhotoId = "";
        pendingAnalysis = null;
        selectedPhotoUri = uri;
        selectedPhotoBitmap = null;
        photoPreview.setImageURI(uri);
        photoPreview.setVisibility(View.VISIBLE);
        photoHintLayout.setVisibility(View.GONE);
    }

    private void showPhoto(Bitmap bitmap) {
        selectedPhotoArchived = false;
        archivedPhotoId = "";
        pendingAnalysis = null;
        selectedPhotoBitmap = bitmap;
        selectedPhotoUri = null;
        photoPreview.setImageBitmap(bitmap);
        photoPreview.setVisibility(View.VISIBLE);
        photoHintLayout.setVisibility(View.GONE);
    }
}
