package com.ali.smartgarden.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeZonePagerAdapter
        extends RecyclerView.Adapter<HomeZonePagerAdapter.ZoneViewHolder> {

    public interface OnZoneClickListener {
        void onZoneClick(GardenZone zone);
    }

    private final List<GardenZone> zones =
            new ArrayList<>();
    private final OnZoneClickListener listener;

    public HomeZonePagerAdapter(
            OnZoneClickListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(List<GardenZone> items) {
        zones.clear();
        if (items != null) {
            zones.addAll(items);
        }
        notifyDataSetChanged();
    }

    public int getZoneCount() {
        return zones.size();
    }

    public int toZonePosition(int adapterPosition) {
        if (zones.isEmpty()) {
            return 0;
        }
        return Math.floorMod(
                adapterPosition,
                zones.size()
        );
    }

    public int initialAdapterPosition() {
        if (zones.isEmpty()) {
            return 0;
        }
        int middle = Integer.MAX_VALUE / 2;
        return middle - Math.floorMod(
                middle,
                zones.size()
        );
    }

    public int nearestAdapterPosition(
            int currentAdapterPosition,
            int requestedZonePosition
    ) {
        if (zones.isEmpty()) {
            return 0;
        }

        int count = zones.size();
        int currentZonePosition =
                toZonePosition(currentAdapterPosition);
        int forward = Math.floorMod(
                requestedZonePosition - currentZonePosition,
                count
        );
        int backward = forward - count;
        int delta = Math.abs(backward) < Math.abs(forward)
                ? backward
                : forward;
        return currentAdapterPosition + delta;
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_home_zone_page,
                        parent,
                        false
                );

        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            int fallbackPadding = Math.round(
                    32f * parent.getResources()
                            .getDisplayMetrics().density
            );
            parentWidth = parent.getResources()
                    .getDisplayMetrics().widthPixels
                    - fallbackPadding;
        }
        view.getLayoutParams().width =
                Math.max(1, parentWidth);

        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ZoneViewHolder holder,
            int position
    ) {
        holder.bind(
                zones.get(toZonePosition(position))
        );
    }

    @Override
    public int getItemCount() {
        return zones.isEmpty()
                ? 0
                : Integer.MAX_VALUE;
    }

    class ZoneViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView sensor;
        private final TextView badge;
        private final TextView moisture;
        private final TextView moistureState;
        private final TextView idealState;
        private final TextView idealRange;
        private final TextView lastWaterValue;
        private final TextView lastWaterDetail;
        private final TextView voltage;
        private final TextView raw;
        private final CircularProgressIndicator progress;

        ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtPageZoneName);
            sensor = itemView.findViewById(R.id.txtPageZoneSensor);
            badge = itemView.findViewById(R.id.txtPageZoneBadge);
            moisture = itemView.findViewById(R.id.txtPageZoneMoisture);
            moistureState = itemView.findViewById(
                    R.id.txtPageZoneMoistureState
            );
            idealState = itemView.findViewById(R.id.txtPageZoneIdealState);
            idealRange = itemView.findViewById(R.id.txtPageZoneIdealRange);
            lastWaterValue = itemView.findViewById(R.id.txtPageZoneLastWaterValue);
            lastWaterDetail = itemView.findViewById(R.id.txtPageZoneLastWaterDetail);
            voltage = itemView.findViewById(
                    R.id.txtPageZoneVoltage
            );
            raw = itemView.findViewById(R.id.txtPageZoneRaw);
            progress = itemView.findViewById(
                    R.id.progressPageZoneMoisture
            );
        }

        void bind(GardenZone zone) {
            Context context = itemView.getContext();
            String emoji = zone.getEmoji() == null
                    || zone.getEmoji().isBlank()
                    ? "🌱"
                    : zone.getEmoji();
            String zoneName = zone.getName() == null
                    ? "Bölge"
                    : zone.getName();
            String sensorId = zone.getSensor_id() == null
                    ? "—"
                    : zone.getSensor_id();

            name.setText(emoji + " " + zoneName);
            sensor.setText(
                    context.getString(
                            R.string.home_zone_sensor_subtitle,
                            sensorId
                    )
            );

            if (!zone.isSensor_enabled()) {
                bindPaused(context);
            } else if (isConnected(zone)) {
                bindConnected(context, zone);
            } else {
                bindWaiting(context);
            }

            itemView.setOnClickListener(
                    view -> listener.onZoneClick(zone)
            );
        }

        private void bindConnected(
                Context context,
                GardenZone zone
        ) {
            int value = Math.max(
                    0,
                    Math.min(100, zone.getMoisture())
            );
            badge.setText("CANLI");
            badge.setTextColor(color(context, R.color.primary));
            moisture.setText(
                    context.getString(
                            R.string.sensor_moisture_format,
                            value
                    )
            );
            progress.setProgress(value);
            voltage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.3f V",
                            zone.getVoltage()
                    )
            );
            raw.setText(String.valueOf(zone.getRaw()));

            int statusColor;
            String statusText;
            if (value < 35) {
                statusColor = color(context, R.color.moistureLow);
                statusText = "Düşük nem seviyesi";
            } else if (value > 70) {
                statusColor = color(context, R.color.info);
                statusText = "Yüksek nem seviyesi";
            } else {
                statusColor = color(context, R.color.moistureIdeal);
                statusText = "İdeal nem seviyesi";
            }

            moisture.setTextColor(statusColor);
            moistureState.setText(statusText);
            moistureState.setTextColor(statusColor);
            progress.setIndicatorColor(statusColor);
            progress.setTrackColor(color(context, R.color.divider));
            bindIdealRange(context, zone, value, statusColor);
            bindWateringState(context, zone);
        }

        private void bindWaiting(Context context) {
            badge.setText("BEKLİYOR");
            badge.setTextColor(
                    color(context, R.color.textSecondary)
            );
            moisture.setText("—");
            moisture.setTextColor(
                    color(context, R.color.textSecondary)
            );
            moistureState.setText("Sensör bağlantısı bekleniyor");
            moistureState.setTextColor(
                    color(context, R.color.textSecondary)
            );
            progress.setProgress(0);
            progress.setIndicatorColor(color(context, R.color.textSecondary));
            progress.setTrackColor(color(context, R.color.divider));
            idealState.setText("Veri bekleniyor");
            idealRange.setText("—");
            lastWaterValue.setText("Veri yok");
            lastWaterDetail.setText("Sensör bekleniyor");
            voltage.setText("—");
            raw.setText("—");
        }

        private void bindPaused(Context context) {
            badge.setText(
                    R.string.home_zone_sensor_paused_badge
            );
            badge.setTextColor(
                    color(context, R.color.textSecondary)
            );
            moisture.setText("-");
            moisture.setTextColor(
                    color(context, R.color.textSecondary)
            );
            moistureState.setText(
                    R.string.home_zone_sensor_paused
            );
            moistureState.setTextColor(
                    color(context, R.color.textSecondary)
            );
            progress.setProgress(0);
            progress.setIndicatorColor(color(context, R.color.textSecondary));
            progress.setTrackColor(color(context, R.color.divider));
            idealState.setText("Sensör kapalı");
            idealRange.setText("—");
            lastWaterValue.setText("Veri yok");
            lastWaterDetail.setText("Sensör kapalı");
            voltage.setText("-");
            raw.setText("-");
        }

        private boolean isConnected(GardenZone zone) {
            if (zone.getUpdated_at_epoch() <= 0L) {
                return false;
            }
            long age = Math.max(
                    0L,
                    System.currentTimeMillis() / 1000L
                            - zone.getUpdated_at_epoch()
            );
            return age <= 90L;
        }

        private void bindIdealRange(
                Context context,
                GardenZone zone,
                int value,
                int statusColor
        ) {
            int lower = Math.max(0, zone.getMoisture_limit());
            int upper = Math.min(100, lower + 20);
            String label = value < lower
                    ? "Nem düşük"
                    : value > upper ? "Nem yüksek" : "Nem ideal aralıkta";
            idealState.setText(label);
            idealState.setTextColor(statusColor);
            idealRange.setText("%" + lower + " - %" + upper);
            idealRange.setTextColor(color(context, R.color.textSecondary));
        }

        private void bindWateringState(Context context, GardenZone zone) {
            ZoneIrrigationStatus irrigation = zone.getIrrigation_status();
            if (irrigation != null && irrigation.isWatering_active()) {
                lastWaterValue.setText("Şimdi");
                lastWaterDetail.setText("Sulama sürüyor");
                return;
            }
            if (irrigation != null && irrigation.isCooldown_active()) {
                int minutes = Math.max(1,
                        (irrigation.getCooldown_remaining() + 59) / 60);
                lastWaterValue.setText(minutes + " dk kaldı");
                lastWaterDetail.setText("Bekleme süresi");
                return;
            }
            lastWaterValue.setText("Hazır");
            lastWaterDetail.setText(zone.isIrrigation_enabled()
                    ? "Otomatik izleniyor" : "Otomatik kapalı");
            lastWaterValue.setTextColor(color(context,
                    zone.isIrrigation_enabled() ? R.color.primary : R.color.textSecondary));
        }

        private int color(Context context, int resource) {
            return ContextCompat.getColor(context, resource);
        }
    }
}
