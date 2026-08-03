package com.ali.smartgarden.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenZone;
import com.ali.smartgarden.models.ZoneIrrigationStatus;

import java.util.ArrayList;
import java.util.List;

public class GardenZoneAdapter
        extends RecyclerView.Adapter<GardenZoneAdapter.ZoneViewHolder> {

    private static final long CONNECTED_SECONDS = 30L;
    private static final long WEAK_SECONDS = 90L;

    private final List<GardenZone> zones =
            new ArrayList<>();
    private OnZoneClickListener onZoneClickListener;

    public interface OnZoneClickListener {
        void onZoneClick(GardenZone zone);
    }

    public void setOnZoneClickListener(
            OnZoneClickListener listener
    ) {
        onZoneClickListener = listener;
    }

    public void submitZones(List<GardenZone> values) {
        zones.clear();
        if (values != null) {
            zones.addAll(values);
        }
        notifyDataSetChanged();
    }

    public int getConnectedCount() {
        int count = 0;
        for (GardenZone zone : zones) {
            if (!zone.isSensor_enabled()) {
                continue;
            }
            long age = getAgeSeconds(zone);
            if (age >= 0 && age <= WEAK_SECONDS) {
                count++;
            }
        }
        return count;
    }

    public int getEnabledSensorCount() {
        int count = 0;
        for (GardenZone zone : zones) {
            if (zone.isSensor_enabled()) {
                count++;
            }
        }
        return count;
    }

    public void refreshStatuses() {
        notifyItemRangeChanged(
                0,
                zones.size()
        );
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_garden_zone,
                        parent,
                        false
                );
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ZoneViewHolder holder,
            int position
    ) {
        holder.bind(zones.get(position));
        holder.itemView.setOnClickListener(
                view -> {
                    int currentPosition =
                            holder.getBindingAdapterPosition();
                    if (
                            onZoneClickListener != null
                                    && currentPosition
                                    != RecyclerView.NO_POSITION
                    ) {
                        onZoneClickListener.onZoneClick(
                                zones.get(currentPosition)
                        );
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    private static long getAgeSeconds(
            GardenZone zone
    ) {
        if (
                zone == null
                        || zone.getUpdated_at_epoch() <= 0L
        ) {
            return -1L;
        }

        long nowSeconds =
                System.currentTimeMillis() / 1000L;

        return Math.max(
                0L,
                nowSeconds - zone.getUpdated_at_epoch()
        );
    }

    static class ZoneViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView status;
        private final TextView sensorId;
        private final LinearLayout measurement;
        private final TextView moisture;
        private final TextView rssi;
        private final TextView wifiStatus;
        private final TextView irrigationStatus;
        private final TextView waiting;
        private final TextView lastUpdate;

        ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtZoneName);
            status = itemView.findViewById(R.id.txtZoneStatus);
            sensorId = itemView.findViewById(R.id.txtZoneSensorId);
            measurement = itemView.findViewById(
                    R.id.layoutZoneMeasurement
            );
            moisture = itemView.findViewById(
                    R.id.txtZoneMoisture
            );
            rssi = itemView.findViewById(R.id.txtZoneRssi);
            wifiStatus = itemView.findViewById(
                    R.id.txtZoneWifiStatus
            );
            irrigationStatus = itemView.findViewById(
                    R.id.txtZoneIrrigationStatus
            );
            waiting = itemView.findViewById(
                    R.id.txtZoneWaiting
            );
            lastUpdate = itemView.findViewById(
                    R.id.txtZoneLastUpdate
            );
        }

        void bind(GardenZone zone) {
            String emoji =
                    zone.getEmoji() == null
                            ? "🌱"
                            : zone.getEmoji();
            String zoneName =
                    zone.getName() == null
                            ? "İsimsiz bölge"
                            : zone.getName();
            String sensor =
                    zone.getSensor_id() == null
                            ? "Sensör atanmamış"
                            : zone.getSensor_id();

            name.setText(emoji + " " + zoneName);
            sensorId.setText(sensor);

            waiting.setText(R.string.sensor_waiting_description);

            if (!zone.isSensor_enabled()) {
                measurement.setVisibility(View.GONE);
                waiting.setVisibility(View.VISIBLE);
                lastUpdate.setVisibility(View.GONE);
                irrigationStatus.setVisibility(View.GONE);
                waiting.setText(R.string.sensor_disabled_description);
                setStatus(
                        R.string.sensor_status_paused,
                        R.color.textSecondary
                );
                return;
            }

            long age = getAgeSeconds(zone);
            boolean hasData = age >= 0L;

            measurement.setVisibility(
                    hasData ? View.VISIBLE : View.GONE
            );
            waiting.setVisibility(
                    hasData ? View.GONE : View.VISIBLE
            );
            lastUpdate.setVisibility(
                    hasData ? View.VISIBLE : View.GONE
            );
            irrigationStatus.setVisibility(
                    hasData ? View.VISIBLE : View.GONE
            );

            if (!hasData) {
                setStatus(
                        R.string.sensor_status_waiting,
                        R.color.textSecondary
                );
                return;
            }

            moisture.setText(
                    itemView.getContext().getString(
                            R.string.sensor_moisture_format,
                            zone.getMoisture()
                    )
            );
            rssi.setText(
                    itemView.getContext().getString(
                            R.string.sensor_rssi_format,
                            zone.getRssi()
                    )
            );
            lastUpdate.setText(
                    itemView.getContext().getString(
                            R.string.sensor_last_update_seconds,
                            age
                    )
            );

            if (age <= CONNECTED_SECONDS) {
                setStatus(
                        R.string.sensor_status_connected,
                        R.color.online
                );
            } else if (age <= WEAK_SECONDS) {
                setStatus(
                        R.string.sensor_status_delayed,
                        R.color.warning
                );
            } else {
                setStatus(
                        R.string.sensor_status_disconnected,
                        R.color.offline
                );
            }

            bindWifiStatus(
                    zone.getRssi(),
                    age
            );
            bindIrrigationStatus(zone);
        }

        private void bindIrrigationStatus(
                GardenZone zone
        ) {
            if (!zone.isIrrigation_enabled()) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_auto_disabled
                        ),
                        R.color.textSecondary
                );
                return;
            }

            ZoneIrrigationStatus value =
                    zone.getIrrigation_status();

            if (value != null && value.isWatering_active()) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_watering
                        ),
                        R.color.info
                );
                return;
            }

            if (value != null && value.isCooldown_active()) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_cooldown,
                                formatRemaining(
                                        value.getCooldown_remaining()
                                )
                        ),
                        R.color.warning
                );
                return;
            }

            if (
                    value != null
                            && value.getQueue_position() > 1
            ) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_queued,
                                value.getQueue_position()
                        ),
                        R.color.accentOrange
                );
                return;
            }

            setIrrigationStatus(
                    itemView.getContext().getString(
                            R.string.zone_card_ready
                    ),
                    R.color.online
            );
        }

        private String formatRemaining(int seconds) {
            int safeSeconds = Math.max(0, seconds);
            int totalMinutes = (safeSeconds + 59) / 60;

            if (totalMinutes < 60) {
                return itemView.getContext().getString(
                        R.string.zone_card_duration_minutes,
                        totalMinutes
                );
            }

            return itemView.getContext().getString(
                    R.string.zone_card_duration_hours_minutes,
                    totalMinutes / 60,
                    totalMinutes % 60
            );
        }

        private void setIrrigationStatus(
                String text,
                int colorResource
        ) {
            irrigationStatus.setText(text);
            irrigationStatus.setTextColor(
                    itemView.getContext().getColor(
                            colorResource
                    )
            );
        }

        private void bindWifiStatus(
                int value,
                long age
        ) {
            if (age > WEAK_SECONDS) {
                setWifiStatus(
                        R.string.sensor_wifi_status_disconnected,
                        R.color.offline
                );
            } else if (value == 0) {
                setWifiStatus(
                        R.string.sensor_wifi_status_unknown,
                        R.color.textSecondary
                );
            } else if (value >= -55) {
                setWifiStatus(
                        R.string.sensor_wifi_status_excellent,
                        R.color.online
                );
            } else if (value >= -70) {
                setWifiStatus(
                        R.string.sensor_wifi_status_good,
                        R.color.online
                );
            } else if (value >= -80) {
                setWifiStatus(
                        R.string.sensor_wifi_status_fair,
                        R.color.warning
                );
            } else {
                setWifiStatus(
                        R.string.sensor_wifi_status_weak,
                        R.color.offline
                );
            }
        }

        private void setStatus(
                int textResource,
                int colorResource
        ) {
            status.setText(textResource);
            status.setTextColor(
                    itemView.getContext().getColor(
                            colorResource
                    )
            );
        }

        private void setWifiStatus(
                int textResource,
                int colorResource
        ) {
            wifiStatus.setText(textResource);
            wifiStatus.setTextColor(
                    itemView.getContext().getColor(
                            colorResource
                    )
            );
        }
    }
}
