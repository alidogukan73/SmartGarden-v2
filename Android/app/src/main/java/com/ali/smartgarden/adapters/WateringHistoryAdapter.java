package com.ali.smartgarden.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.WateringHistory;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class WateringHistoryAdapter extends ListAdapter<
        WateringHistory,
        WateringHistoryAdapter.HistoryViewHolder
        > {

    private static final SimpleDateFormat FIREBASE_DATE_FORMAT =
            new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.US
            );

    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat(
                    "dd MMMM yyyy",
                    new Locale("tr", "TR")
            );

    private static final SimpleDateFormat DISPLAY_TIME_FORMAT =
            new SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
            );


    public WateringHistoryAdapter() {

        super(DIFF_CALLBACK);
    }


    private static final DiffUtil.ItemCallback<WateringHistory>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull WateringHistory oldItem,
                        @NonNull WateringHistory newItem
                ) {

                    return Objects.equals(
                            oldItem.getRecordId(),
                            newItem.getRecordId()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WateringHistory oldItem,
                        @NonNull WateringHistory newItem
                ) {

                    return Objects.equals(
                            oldItem.getStartedAt(),
                            newItem.getStartedAt()
                    )
                            && Objects.equals(
                            oldItem.getFinishedAt(),
                            newItem.getFinishedAt()
                    )
                            && oldItem.getDuration()
                            == newItem.getDuration()

                            && oldItem.getMoistureBefore()
                            == newItem.getMoistureBefore()

                            && oldItem.getMoistureAfter()
                            == newItem.getMoistureAfter()

                            && oldItem.getMoistureDelta()
                            == newItem.getMoistureDelta()

                            && oldItem.isCompleted()
                            == newItem.isCompleted()

                            && Objects.equals(
                            oldItem.getStopReason(),
                            newItem.getStopReason()
                    )
                            && Objects.equals(
                            oldItem.getMode(),
                            newItem.getMode()
                    )

                            && Objects.equals(
                            oldItem.getFirmware(),
                            newItem.getFirmware()
                    );
                }
            };


    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_watering_history,
                        parent,
                        false
                );

        return new HistoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(
            @NonNull HistoryViewHolder holder,
            int position
    ) {

        holder.bind(
                getItem(position)
        );
    }


    static class HistoryViewHolder
            extends RecyclerView.ViewHolder {

        private final MaterialCardView cardHistoryItem;
        private final MaterialCardView cardHistoryStatus;
        private final MaterialCardView cardHistoryDelta;

        private final TextView txtHistoryDate;
        private final TextView txtHistoryTime;
        private final TextView txtHistoryStatus;

        private final TextView txtHistoryMode;
        private final TextView txtHistoryDuration;

        private final TextView txtHistoryMoistureBefore;
        private final TextView txtHistoryMoistureAfter;
        private final TextView txtHistoryMoistureDelta;

        private final TextView txtHistoryStopReason;


        public HistoryViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            cardHistoryItem =
                    itemView.findViewById(
                            R.id.cardHistoryItem
                    );

            cardHistoryStatus =
                    itemView.findViewById(
                            R.id.cardHistoryStatus
                    );

            cardHistoryDelta =
                    itemView.findViewById(
                            R.id.cardHistoryDelta
                    );

            txtHistoryDate =
                    itemView.findViewById(
                            R.id.txtHistoryDate
                    );

            txtHistoryTime =
                    itemView.findViewById(
                            R.id.txtHistoryTime
                    );

            txtHistoryStatus =
                    itemView.findViewById(
                            R.id.txtHistoryStatus
                    );

            txtHistoryMode =
                    itemView.findViewById(
                            R.id.txtHistoryMode
                    );

            txtHistoryDuration =
                    itemView.findViewById(
                            R.id.txtHistoryDuration
                    );

            txtHistoryMoistureBefore =
                    itemView.findViewById(
                            R.id.txtHistoryMoistureBefore
                    );

            txtHistoryMoistureAfter =
                    itemView.findViewById(
                            R.id.txtHistoryMoistureAfter
                    );

            txtHistoryMoistureDelta =
                    itemView.findViewById(
                            R.id.txtHistoryMoistureDelta
                    );

            txtHistoryStopReason =
                    itemView.findViewById(
                            R.id.txtHistoryStopReason
                    );
        }


        /**
         * Tek bir sulama kaydını kart görünümüne bağlar.
         */
        public void bind(
                WateringHistory history
        ) {

            Context context =
                    itemView.getContext();

            bindDate(
                    context,
                    history.getStartedAt()
            );

            txtHistoryMode.setText(
                    formatMode(
                            context,
                            history.getMode()
                    )
            );

            txtHistoryDuration.setText(
                    formatDuration(
                            context,
                            history.getDuration()
                    )
            );

            txtHistoryMoistureBefore.setText(
                    context.getString(
                            R.string.percentage_format,
                            history.getMoistureBefore()
                    )
            );

            txtHistoryMoistureAfter.setText(
                    context.getString(
                            R.string.percentage_format,
                            history.getMoistureAfter()
                    )
            );

            txtHistoryMoistureDelta.setText(
                    context.getString(
                            R.string.signed_percentage_format,
                            history.getMoistureDelta()
                    )
            );

            txtHistoryStopReason.setText(
                    formatStopReason(
                            context,
                            history.getStopReason()
                    )
            );

            updateCompletionUi(
                    context,
                    history
            );

            updateMoistureDeltaUi(
                    context,
                    history.getMoistureDelta()
            );
        }


        /**
         * Firebase ISO tarihini kullanıcı dostu tarih ve saate çevirir.
         */
        private void bindDate(
                Context context,
                String startedAt
        ) {

            if (
                    startedAt == null
                            || startedAt.isBlank()
            ) {

                txtHistoryDate.setText(
                        R.string.history_default_date
                );

                txtHistoryTime.setText(
                        R.string.history_default_time
                );

                return;
            }

            try {

                /*
                 * Backend mikro saniye gönderebildiği için
                 * ilk 19 karakteri alıyoruz:
                 *
                 * 2026-07-11T18:18:11
                 */
                String normalizedDate =
                        startedAt.length() >= 19
                                ? startedAt.substring(0, 19)
                                : startedAt;

                Date parsedDate =
                        FIREBASE_DATE_FORMAT.parse(
                                normalizedDate
                        );

                if (parsedDate == null) {
                    throw new ParseException(
                            "Date could not be parsed",
                            0
                    );
                }

                txtHistoryDate.setText(
                        DISPLAY_DATE_FORMAT.format(
                                parsedDate
                        )
                );

                txtHistoryTime.setText(
                        DISPLAY_TIME_FORMAT.format(
                                parsedDate
                        )
                );

            } catch (
                    ParseException
                    | IndexOutOfBoundsException exception
            ) {

                txtHistoryDate.setText(
                        R.string.history_default_date
                );

                txtHistoryTime.setText(
                        R.string.history_default_time
                );
            }
        }


        /**
         * Tamamlanma durumuna göre rozet ve kart rengini değiştirir.
         */
        private void updateCompletionUi(
                Context context,
                WateringHistory history
        ) {

            if (history.isCompleted()) {

                int statusColor =
                        color(
                                context,
                                R.color.online
                        );

                txtHistoryStatus.setText(
                        R.string.history_status_completed
                );

                txtHistoryStatus.setTextColor(
                        statusColor
                );

                cardHistoryStatus.setCardBackgroundColor(
                        color(
                                context,
                                R.color.onlineBackground
                        )
                );

                cardHistoryStatus.setStrokeColor(
                        statusColor
                );

                cardHistoryItem.setStrokeColor(
                        color(
                                context,
                                R.color.border
                        )
                );

                return;
            }

            boolean warning =
                    isWarningReason(
                            history.getStopReason()
                    );

            int statusColor =
                    warning
                            ? color(
                            context,
                            R.color.warning
                    )
                            : color(
                            context,
                            R.color.offline
                    );

            int backgroundColor =
                    warning
                            ? color(
                            context,
                            R.color.warningBackground
                    )
                            : color(
                            context,
                            R.color.offlineBackground
                    );

            txtHistoryStatus.setText(
                    warning
                            ? R.string.history_status_warning
                            : R.string.history_status_interrupted
            );

            txtHistoryStatus.setTextColor(
                    statusColor
            );

            cardHistoryStatus.setCardBackgroundColor(
                    backgroundColor
            );

            cardHistoryStatus.setStrokeColor(
                    statusColor
            );

            cardHistoryItem.setStrokeColor(
                    statusColor
            );
        }


        /**
         * Nem farkına göre değişim kartını renklendirir.
         */
        private void updateMoistureDeltaUi(
                Context context,
                long moistureDelta
        ) {

            int statusColor;
            int backgroundColor;

            if (moistureDelta > 0) {

                statusColor =
                        color(
                                context,
                                R.color.moistureIdeal
                        );

                backgroundColor =
                        color(
                                context,
                                R.color.moistureIdealBackground
                        );

            } else if (moistureDelta < 0) {

                statusColor =
                        color(
                                context,
                                R.color.moistureLow
                        );

                backgroundColor =
                        color(
                                context,
                                R.color.moistureLowBackground
                        );

            } else {

                statusColor =
                        color(
                                context,
                                R.color.textSecondary
                        );

                backgroundColor =
                        color(
                                context,
                                R.color.surfaceSoft
                        );
            }

            txtHistoryMoistureDelta.setTextColor(
                    statusColor
            );

            cardHistoryDelta.setCardBackgroundColor(
                    backgroundColor
            );

            cardHistoryDelta.setStrokeColor(
                    statusColor
            );
        }


        /**
         * AUTO ve MANUAL değerlerini kullanıcı dostu hale getirir.
         */
        private String formatMode(
                Context context,
                String mode
        ) {

            if (
                    mode == null
                            || mode.isBlank()
            ) {

                return context.getString(
                        R.string.history_mode_unknown
                );
            }

            switch (
                    mode.trim()
                            .toUpperCase(Locale.ROOT)
            ) {

                case "AUTO":
                case "AUTOMATIC":
                    return context.getString(
                            R.string.history_mode_auto
                    );

                case "MANUAL":
                    return context.getString(
                            R.string.history_mode_manual
                    );

                default:
                    return mode;
            }
        }


        /**
         * Saniye değerini okunabilir süreye dönüştürür.
         */
        private String formatDuration(
                Context context,
                long seconds
        ) {

            long safeSeconds =
                    Math.max(
                            0,
                            seconds
                    );

            if (safeSeconds < 60) {

                return context.getString(
                        R.string.duration_seconds_format,
                        safeSeconds
                );
            }

            long minutes =
                    safeSeconds / 60;

            long remainingSeconds =
                    safeSeconds % 60;

            return context.getString(
                    R.string.duration_minutes_seconds_format,
                    minutes,
                    remainingSeconds
            );
        }


        /**
         * Backend durdurma nedenini kullanıcı dostu metne çevirir.
         */
        private String formatStopReason(
                Context context,
                String stopReason
        ) {

            if (
                    stopReason == null
                            || stopReason.isBlank()
            ) {

                return context.getString(
                        R.string.history_default_stop_reason
                );
            }

            switch (
                    stopReason.trim()
                            .toUpperCase(Locale.ROOT)
            ) {

                case "COMPLETED":
                case "DURATION_COMPLETED":
                case "WATERING_COMPLETED":
                    return context.getString(
                            R.string.history_reason_completed
                    );

                case "MANUAL_STOP":
                case "MANUAL":
                case "USER_STOPPED":
                    return context.getString(
                            R.string.history_reason_manual_stop
                    );

                case "MOISTURE_REACHED":
                case "TARGET_REACHED":
                    return context.getString(
                            R.string.history_reason_target_reached
                    );

                case "SYSTEM_DISABLED":
                    return context.getString(
                            R.string.history_reason_system_disabled
                    );

                case "DEVICE_OFFLINE":
                    return context.getString(
                            R.string.history_reason_device_offline
                    );

                case "SAFETY_TIMEOUT":
                case "TIMEOUT":
                    return context.getString(
                            R.string.history_reason_timeout
                    );

                default:
                    return stopReason
                            .replace("_", " ");
            }
        }


        /**
         * Uyarı olarak gösterilmesi gereken nedenleri belirler.
         */
        private boolean isWarningReason(
                String stopReason
        ) {

            if (stopReason == null) {
                return false;
            }

            String normalizedReason =
                    stopReason.trim()
                            .toUpperCase(Locale.ROOT);

            return normalizedReason.equals(
                    "SAFETY_TIMEOUT"
            )
                    || normalizedReason.equals(
                    "TIMEOUT"
            )
                    || normalizedReason.equals(
                    "MOISTURE_REACHED"
            )
                    || normalizedReason.equals(
                    "TARGET_REACHED"
            );
        }


        private int color(
                Context context,
                int colorResource
        ) {

            return ContextCompat.getColor(
                    context,
                    colorResource
            );
        }
    }
}