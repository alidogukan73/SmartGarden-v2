package com.ali.smartgarden.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.ali.smartgarden.plantdoctor.PlantDoctorAdvisor;
import com.ali.smartgarden.plantdoctor.PlantDoctorRecommendationStore;
import com.ali.smartgarden.plantdoctor.PlantDoctorResult;
import com.ali.smartgarden.plantdoctor.PlantVisionClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** AI Bitki Doktoru: fotoğraf, belirtiler, sensör ve hava bağlamıyla güvenli ön değerlendirme. */
public class PlantDoctorActivity extends AppCompatActivity {
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
    private boolean selectedPhotoArchived;

    private final ActivityResultLauncher<Void> camera =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) showPhoto(bitmap);
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) showPhoto(uri);
            });

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_plant_doctor);
        requestedZoneId = getIntent().getStringExtra("zone_id");
        photoStore = new LocalGardenPhotoStore(this);
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
        findViewById(R.id.btnDoctorTakePhoto).setOnClickListener(view -> camera.launch(null));
        findViewById(R.id.btnDoctorPickPhoto).setOnClickListener(view -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        findViewById(R.id.btnAnalyzePlant).setOnClickListener(view -> analyze());
        other.setOnCheckedChangeListener((button, checked) -> {
            otherNoteLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) otherNote.setText("");
        });
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
        PlantDoctorResult result = PlantDoctorAdvisor.assess(zone, symptoms, note, currentWeather, hasPhoto());
        renderHeuristicResult(result);
        if (hasPhoto()) requestVisionAnalysis(zone, symptoms, note);
        savePhotoToArchive(zone, symptoms, note);
    }

    private void renderHeuristicResult(PlantDoctorResult result) {
        title.setText(result.getTitle());
        meta.setText("Olasılık: " + result.getProbability() + " · Aciliyet: " + result.getUrgency());
        context.setText("Değerlendirilen bahçe verisi: " + result.getContext());
        advice.setText(result.getAdvice());
        PlantDoctorRecommendationStore.save(this, result.getTitle(), result.getAdvice());
        resultCard.setVisibility(View.VISIBLE);
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
                JSONObject visual = PlantVisionClient.analyze(image, payload);
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
            return;
        }
        String findings = visual.optString("visual_findings", "Görsel bulgu üretilemedi.");
        String causes = PlantVisionClient.list(visual.optJSONArray("possible_causes"));
        String steps = PlantVisionClient.list(visual.optJSONArray("next_steps"));
        String redFlags = PlantVisionClient.list(visual.optJSONArray("red_flags"));
        PlantDoctorRecommendationStore.save(
                this,
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
        soilData.setText("💧\nToprak nemi\n%" + zone.getMoisture());
        weatherTemperatureData.setText("🌡\nHava sıcaklığı\n" + number(currentWeather == null ? null : currentWeather.getCurrentTemperature(), "°C"));
        sunData.setText("☀\nGüneş ışığı\n" + sunLabel(currentWeather));
        windData.setText("≋\nRüzgar\n" + number(currentWeather == null ? null : currentWeather.getCurrentWind(), " km/sa"));
        humidityData.setText("💧\nHava nemi\n" + number(currentWeather == null ? null : currentWeather.getCurrentHumidity(), "%"));
    }

    private void savePhotoToArchive(GardenZone zone, List<String> symptoms, String note) {
        if (!hasPhoto() || selectedPhotoArchived) return;
        selectedPhotoArchived = true;
        String archiveNote = "Bitki Doktoru · " + String.join(", ", symptoms)
                + (note.isEmpty() ? "" : " · " + note);
        Uri uri = selectedPhotoUri;
        Bitmap bitmap = selectedPhotoBitmap;
        new Thread(() -> {
            try {
                if (uri != null) photoStore.save(uri, zone.getZone_id(), archiveNote, "plant_doctor");
                else if (bitmap != null) photoStore.save(bitmap, zone.getZone_id(), archiveNote, "plant_doctor");
                runOnUiThread(() -> toast("Fotoğraf Bitki Doktoru arşivine kaydedildi."));
            } catch (Exception ignored) {
                selectedPhotoArchived = false;
                runOnUiThread(() -> toast("Fotoğraf analize eklendi, ancak arşive kaydedilemedi."));
            }
        }).start();
    }

    private GardenZone selectedZone() { return zones.get(String.valueOf(zoneDropdown.getText())); }
    private boolean hasPhoto() { return selectedPhotoUri != null || selectedPhotoBitmap != null; }
    private String labelFor(GardenZone zone) { return (zone.getEmoji() == null ? "🌱" : zone.getEmoji()) + " " + zone.getName(); }
    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private String number(Double value, String suffix) { return value == null ? "—" : Math.round(value) + suffix; }
    private String sunLabel(WeatherForecast weather) {
        if (weather == null || weather.getCurrentWeatherCode() == null) return "—";
        return weather.getCurrentWeatherCode() <= 1 ? "Kuvvetli" : weather.getCurrentWeatherCode() <= 3 ? "Orta" : "Düşük";
    }

    private void showPhoto(Uri uri) {
        selectedPhotoArchived = false;
        selectedPhotoUri = uri;
        selectedPhotoBitmap = null;
        photoPreview.setImageURI(uri);
        photoPreview.setVisibility(View.VISIBLE);
        photoHintLayout.setVisibility(View.GONE);
    }

    private void showPhoto(Bitmap bitmap) {
        selectedPhotoArchived = false;
        selectedPhotoBitmap = bitmap;
        selectedPhotoUri = null;
        photoPreview.setImageBitmap(bitmap);
        photoPreview.setVisibility(View.VISIBLE);
        photoHintLayout.setVisibility(View.GONE);
    }
}
