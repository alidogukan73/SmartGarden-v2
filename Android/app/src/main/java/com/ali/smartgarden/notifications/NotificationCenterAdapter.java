package com.ali.smartgarden.notifications;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.models.GardenNotification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenterAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NOTIFICATION = 1;

    private static final int ACTION_WIDTH_DP = 184;

    public interface Listener {

        void onNotificationClick(
                GardenNotification value
        );

        void onSaveClick(
                GardenNotification value
        );

        void onDeleteClick(
                GardenNotification value
        );
    }

    private final Context context;
    private final Listener listener;

    private final List<Row> rows =
            new ArrayList<>();

    public NotificationCenterAdapter(
            Context context,
            Listener listener
    ) {
        this.context = context;
        this.listener = listener;
    }

    public void submitList(
            List<GardenNotification> notifications
    ) {

        rows.clear();

        String lastDay = "";

        if (notifications != null) {

            for (GardenNotification value : notifications) {

                if (value == null) {
                    continue;
                }

                String day =
                        dayLabel(
                                value.getCreated_at_epoch()
                        );

                if (!day.equals(lastDay)) {

                    rows.add(
                            Row.header(day)
                    );

                    lastDay = day;
                }

                rows.add(
                        Row.notification(value)
                );
            }
        }

        notifyDataSetChanged();
    }

    public GardenNotification getNotificationAt(
            int position
    ) {

        if (position < 0
                || position >= rows.size()) {

            return null;
        }

        return rows
                .get(position)
                .notification;
    }

    public boolean isNotificationRow(
            int position
    ) {
        return getNotificationAt(position) != null;
    }

    public View getSwipeForeground(
            RecyclerView.ViewHolder holder
    ) {

        if (!(holder instanceof NotificationHolder)) {
            return null;
        }

        return ((NotificationHolder) holder)
                .foreground;
    }

    public int getActionWidth() {
        return dp(ACTION_WIDTH_DP);
    }

    @Override
    public int getItemViewType(
            int position
    ) {

        return rows
                .get(position)
                .notification == null
                ? TYPE_HEADER
                : TYPE_NOTIFICATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        if (viewType == TYPE_HEADER) {

            TextView header =
                    new TextView(context);

            header.setTextSize(15);

            header.setTextColor(
                    context.getColor(
                            R.color.textPrimary
                    )
            );

            header.setTypeface(
                    null,
                    Typeface.BOLD
            );

            header.setPadding(
                    0,
                    dp(14),
                    0,
                    dp(7)
            );

            return new HeaderHolder(header);
        }

        /*
         * ROOT
         *
         * Arkada actionContainer,
         * önde foreground kartı bulunuyor.
         */
        FrameLayout root =
                new FrameLayout(context);

        RecyclerView.LayoutParams rootParams =
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        rootParams.bottomMargin = dp(8);

        root.setLayoutParams(rootParams);

        // -------------------------------
        // ARKADAKİ BUTONLAR
        // -------------------------------

        LinearLayout actions =
                new LinearLayout(context);

        actions.setOrientation(
                LinearLayout.HORIZONTAL
        );

        actions.setGravity(
                Gravity.CENTER_VERTICAL
        );

        FrameLayout.LayoutParams actionsParams =
                new FrameLayout.LayoutParams(
                        dp(ACTION_WIDTH_DP),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.END
                );

        actions.setLayoutParams(actionsParams);

        MaterialButton saveButton =
                new MaterialButton(context);

        saveButton.setTextSize(12);
        saveButton.setAllCaps(false);

        saveButton.setTextColor(
                context.getColor(
                        android.R.color.white
                )
        );

        saveButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        context.getColor(
                                R.color.primary
                        )
                )
        );

        MaterialButton deleteButton =
                new MaterialButton(context);

        deleteButton.setText("Sil");
        deleteButton.setTextSize(12);
        deleteButton.setAllCaps(false);

        deleteButton.setTextColor(
                context.getColor(
                        android.R.color.white
                )
        );

        deleteButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        context.getColor(
                                R.color.error
                        )
                )
        );

        actions.addView(
                saveButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        actions.addView(
                deleteButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        root.addView(actions);

        // -------------------------------
        // ÖNDEKİ BİLDİRİM KARTI
        // -------------------------------

        MaterialCardView card =
                new MaterialCardView(context);

        card.setRadius(dp(16));

        card.setStrokeColor(
                context.getColor(
                        R.color.border
                )
        );

        card.setStrokeWidth(dp(1));

        FrameLayout.LayoutParams cardParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        card.setLayoutParams(cardParams);

        LinearLayout row =
                new LinearLayout(context);

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dp(13),
                dp(12),
                dp(13),
                dp(12)
        );

        TextView icon =
                text(
                        "",
                        23,
                        R.color.primary
                );

        icon.setGravity(
                Gravity.CENTER
        );

        row.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(42),
                        dp(48)
                )
        );

        LinearLayout info =
                new LinearLayout(context);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView title =
                text(
                        "",
                        14,
                        R.color.textPrimary
                );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        TextView description =
                text(
                        "",
                        12,
                        R.color.textSecondary
                );

        description.setMaxLines(2);

        info.addView(title);
        info.addView(description);

        row.addView(
                info,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView time =
                text(
                        "",
                        11,
                        R.color.textSecondary
                );

        row.addView(time);

        card.addView(row);

        root.addView(card);

        return new NotificationHolder(
                root,
                card,
                icon,
                title,
                description,
                time,
                saveButton,
                deleteButton
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        Row row =
                rows.get(position);

        if (holder instanceof HeaderHolder) {

            ((HeaderHolder) holder)
                    .textView
                    .setText(row.header);

            return;
        }

        NotificationHolder h =
                (NotificationHolder) holder;

        GardenNotification value =
                row.notification;

        /*
         * Recycle edilen kart açık kalmasın.
         */
        h.foreground.setTranslationX(0f);

        h.foreground.setCardBackgroundColor(
                context.getColor(
                        value.isRead()
                                ? R.color.card
                                : R.color.surfaceGreen
                )
        );

        h.icon.setText(
                icon(value.getType())
        );

        h.title.setText(
                value.getTitle()
        );

        h.description.setText(
                value.getDescription()
        );

        h.time.setText(
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.forLanguageTag(
                                "tr-TR"
                        )
                ).format(
                        new Date(
                                value.getCreated_at_epoch()
                                        * 1000L
                        )
                )
        );

        h.saveButton.setText(
                value.isSaved()
                        ? "Kaydı kaldır"
                        : "Kaydet"
        );

        /*
         * Kart açıkken karta dokunulursa
         * önce kapatılır.
         */
        h.foreground.setOnClickListener(view -> {

            if (h.foreground.getTranslationX() != 0f) {

                h.foreground
                        .animate()
                        .translationX(0f)
                        .setDuration(160)
                        .start();

                return;
            }

            if (listener != null) {

                listener.onNotificationClick(
                        value
                );
            }
        });

        h.saveButton.setOnClickListener(view -> {

            h.foreground
                    .animate()
                    .translationX(0f)
                    .setDuration(160)
                    .start();

            if (listener != null) {

                listener.onSaveClick(
                        value
                );
            }
        });

        h.deleteButton.setOnClickListener(view -> {

            h.foreground
                    .animate()
                    .translationX(0f)
                    .setDuration(160)
                    .start();

            if (listener != null) {

                listener.onDeleteClick(
                        value
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private TextView text(
            String value,
            int size,
            int color
    ) {

        TextView view =
                new TextView(context);

        view.setText(value);
        view.setTextSize(size);

        view.setTextColor(
                context.getColor(color)
        );

        return view;
    }

    private String dayLabel(long epoch) {

        java.util.Calendar now =
                java.util.Calendar.getInstance();

        java.util.Calendar notification =
                java.util.Calendar.getInstance();

        notification.setTimeInMillis(
                epoch * 1000L
        );

        if (now.get(java.util.Calendar.YEAR)
                == notification.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR)
                == notification.get(java.util.Calendar.DAY_OF_YEAR)) {

            return "Bugün";
        }

        now.add(
                java.util.Calendar.DAY_OF_YEAR,
                -1
        );

        if (now.get(java.util.Calendar.YEAR)
                == notification.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR)
                == notification.get(java.util.Calendar.DAY_OF_YEAR)) {

            return "Dün";
        }

        return new SimpleDateFormat(
                "dd MMMM yyyy",
                Locale.forLanguageTag("tr-TR")
        ).format(
                new Date(epoch * 1000L)
        );
    }

    private String icon(String type) {

        if ("IRRIGATION".equals(type)) {
            return "💧";
        }

        if ("FERTILIZATION".equals(type)) {
            return "🌱";
        }

        if ("STOCK".equals(type)) {
            return "⚠";
        }

        if ("PHOTO_FOLLOW_UP".equals(type)) {
            return "📷";
        }

        if ("PLANT_ASSISTANT".equals(type)) {
            return "✦";
        }

        if ("WEATHER".equals(type)) {
            return "☀";
        }

        if ("DEVICE".equals(type)) {
            return "▣";
        }

        return "•";
    }

    private int dp(int value) {

        return Math.round(
                value
                        * context
                        .getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private static final class Row {

        final String header;

        final GardenNotification notification;

        private Row(
                String header,
                GardenNotification notification
        ) {

            this.header = header;
            this.notification = notification;
        }

        static Row header(
                String title
        ) {

            return new Row(
                    title,
                    null
            );
        }

        static Row notification(
                GardenNotification value
        ) {

            return new Row(
                    null,
                    value
            );
        }
    }

    private static final class HeaderHolder
            extends RecyclerView.ViewHolder {

        final TextView textView;

        HeaderHolder(
                TextView view
        ) {

            super(view);

            textView = view;
        }
    }

    private static final class NotificationHolder
            extends RecyclerView.ViewHolder {

        final MaterialCardView foreground;

        final TextView icon;
        final TextView title;
        final TextView description;
        final TextView time;

        final MaterialButton saveButton;
        final MaterialButton deleteButton;

        NotificationHolder(
                View root,
                MaterialCardView foreground,
                TextView icon,
                TextView title,
                TextView description,
                TextView time,
                MaterialButton saveButton,
                MaterialButton deleteButton
        ) {

            super(root);

            this.foreground = foreground;

            this.icon = icon;
            this.title = title;
            this.description = description;
            this.time = time;

            this.saveButton = saveButton;
            this.deleteButton = deleteButton;
        }
    }
}