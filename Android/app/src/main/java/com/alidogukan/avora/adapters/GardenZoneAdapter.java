package com.alidogukan.avora.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.GardenZone;
import com.alidogukan.avora.models.ZoneIrrigationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GardenZoneAdapter
        extends RecyclerView.Adapter<GardenZoneAdapter.ZoneViewHolder> {

    private static final long CONNECTED_SECONDS = 30L;
    private static final long WEAK_SECONDS = 90L;
    private static final Object PAYLOAD_CONTENT_CHANGE = new Object();
    private static final Object PAYLOAD_STATUS_REFRESH = new Object();

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
        List<GardenZone> previous = new ArrayList<>(zones);
        List<GardenZone> updated = values == null
                ? new ArrayList<>()
                : new ArrayList<>(values);

        DiffUtil.DiffResult difference = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return previous.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return updated.size();
                    }

                    @Override
                    public boolean areItemsTheSame(
                            int oldItemPosition,
                            int newItemPosition
                    ) {
                        return itemIdentity(
                                previous.get(oldItemPosition)
                        ).equals(itemIdentity(
                                updated.get(newItemPosition)
                        ));
                    }

                    @Override
                    public boolean areContentsTheSame(
                            int oldItemPosition,
                            int newItemPosition
                    ) {
                        return sameDisplayedContent(
                                previous.get(oldItemPosition),
                                updated.get(newItemPosition)
                        );
                    }

                    @Override
                    public Object getChangePayload(
                            int oldItemPosition,
                            int newItemPosition
                    ) {
                        return PAYLOAD_CONTENT_CHANGE;
                    }
                }
        );

        zones.clear();
        zones.addAll(updated);
        difference.dispatchUpdatesTo(this);
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
        if (!zones.isEmpty()) {
            notifyItemRangeChanged(
                    0,
                    zones.size(),
                    PAYLOAD_STATUS_REFRESH
            );
        }
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
        bindHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ZoneViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        bindHolder(holder, position);
    }

    private void bindHolder(
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

    private static String itemIdentity(GardenZone zone) {
        String zoneId = safe(zone == null ? null : zone.getZone_id());
        if (!zoneId.isEmpty()) {
            return "zone:" + zoneId;
        }
        return "sensor:" + safe(
                zone == null ? null : zone.getSensor_id()
        );
    }

    private static boolean sameDisplayedContent(
            GardenZone previous,
            GardenZone updated
    ) {
        if (previous == updated) {
            return true;
        }
        if (previous == null || updated == null) {
            return false;
        }
        return Objects.equals(previous.getName(), updated.getName())
                && Objects.equals(previous.getEmoji(), updated.getEmoji())
                && Objects.equals(previous.getSensor_id(), updated.getSensor_id())
                && previous.isSensor_enabled() == updated.isSensor_enabled()
                && previous.isIrrigation_enabled() == updated.isIrrigation_enabled()
                && previous.getMoisture_limit() == updated.getMoisture_limit()
                && previous.getMoisture() == updated.getMoisture()
                && previous.getRssi() == updated.getRssi()
                && previous.getRaw() == updated.getRaw()
                && Double.compare(previous.getVoltage(), updated.getVoltage()) == 0
                && previous.getUpdated_at_epoch() == updated.getUpdated_at_epoch()
                && sameIrrigationStatus(
                        previous.getIrrigation_status(),
                        updated.getIrrigation_status()
                );
    }

    private static boolean sameIrrigationStatus(
            ZoneIrrigationStatus previous,
            ZoneIrrigationStatus updated
    ) {
        if (previous == updated) {
            return true;
        }
        if (previous == null || updated == null) {
            return false;
        }
        return previous.isWatering_active() == updated.isWatering_active()
                && previous.isCooldown_active() == updated.isCooldown_active()
                && previous.getCooldown_remaining() == updated.getCooldown_remaining()
                && previous.getQueue_position() == updated.getQueue_position()
                && Objects.equals(
                        previous.getDecision_reason(),
                        updated.getDecision_reason()
                );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
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
        private final LinearLayout sensorMetrics;
        private final TextView voltage;
        private final TextView raw;
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
            sensorMetrics = itemView.findViewById(R.id.layoutZoneSensorMetrics);
            voltage = itemView.findViewById(R.id.txtZoneVoltage);
            raw = itemView.findViewById(R.id.txtZoneRaw);
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
                            ? itemView.getContext().getString(R.string.runtime_zone_unnamed)
                            : zone.getName();
            String sensor =
                    zone.getSensor_id() == null
                            ? itemView.getContext().getString(R.string.runtime_sensor_unassigned)
                            : zone.getSensor_id();

            name.setText(itemView.getContext().getString(
                    R.string.runtime_icon_label,
                    emoji,
                    zoneName));
            sensorId.setText(sensor);

            waiting.setText(R.string.sensor_waiting_description);

            if (!zone.isSensor_enabled()) {
                measurement.setVisibility(View.GONE);
                sensorMetrics.setVisibility(View.GONE);
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
            sensorMetrics.setVisibility(hasData ? View.VISIBLE : View.GONE);
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
            moisture.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            moistureColorResource(
                                    zone.getMoisture()
                            )
                    )
            );
            rssi.setText(
                    itemView.getContext().getString(
                            R.string.sensor_rssi_format,
                            zone.getRssi()
                    )
            );
            voltage.setText(String.format(Locale.getDefault(), "%.3f V", zone.getVoltage()));
            raw.setText(String.valueOf(zone.getRaw()));
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

        private int moistureColorResource(
                int moistureValue
        ) {
            if (moistureValue < 35) {
                return R.color.moistureLow;
            }

            if (moistureValue > 70) {
                return R.color.info;
            }

            return R.color.moistureIdeal;
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

            if (zone.getMoisture() >= zone.getMoisture_limit()) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_moisture_sufficient
                        ),
                        R.color.online
                );
                return;
            }

            if (
                    value != null
                            && "WAITING_FOR_MOISTURE_RECOVERY".equals(
                            value.getDecision_reason()
                    )
            ) {
                setIrrigationStatus(
                        itemView.getContext().getString(
                                R.string.zone_card_recovery_waiting
                        ),
                        R.color.warning
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
