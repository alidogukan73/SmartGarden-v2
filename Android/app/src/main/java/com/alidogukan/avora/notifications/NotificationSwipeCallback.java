package com.alidogukan.avora.notifications;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationSwipeCallback
        extends ItemTouchHelper.SimpleCallback {

    private final NotificationCenterAdapter adapter;

    public NotificationSwipeCallback(
            NotificationCenterAdapter adapter
    ) {
        super(
                0,
                ItemTouchHelper.LEFT
        );

        this.adapter = adapter;
    }

    @Override
    public int getSwipeDirs(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        int position =
                viewHolder.getBindingAdapterPosition();

        // Bugün / Dün gibi başlık satırları kaydırılamaz.
        if (position == RecyclerView.NO_POSITION
                || !adapter.isNotificationRow(position)) {
            return 0;
        }

        return ItemTouchHelper.LEFT;
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target
    ) {
        return false;
    }

    /*
     * Biz satırı gerçekten swipe edip silmiyoruz.
     * Bu yüzden bu metodun normal şartlarda
     * devreye girmesini istemiyoruz.
     */
    @Override
    public void onSwiped(
            @NonNull RecyclerView.ViewHolder viewHolder,
            int direction
    ) {
        // Bilerek boş.
    }

    /*
     * ItemTouchHelper satırı "tam swipe edildi"
     * olarak kabul etmesin.
     */
    @Override
    public float getSwipeThreshold(
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        return 2f;
    }

    /*
     * Hızlı flick hareketi de tam swipe oluşturmasın.
     */
    @Override
    public float getSwipeEscapeVelocity(
            float defaultValue
    ) {
        return Float.MAX_VALUE;
    }

    @Override
    public float getSwipeVelocityThreshold(
            float defaultValue
    ) {
        return Float.MAX_VALUE;
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas canvas,
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {
        if (actionState
                != ItemTouchHelper.ACTION_STATE_SWIPE) {
            return;
        }

        View foreground =
                adapter.getSwipeForeground(
                        viewHolder
                );

        if (foreground == null) {
            return;
        }

        /*
         * KRİTİK:
         *
         * Sadece kullanıcının parmağı gerçekten
         * ekrandayken kartı dX ile hareket ettiriyoruz.
         *
         * Parmağı bıraktıktan sonra ItemTouchHelper
         * dX'i tekrar 0'a doğru animasyonla götürür.
         * O animasyonu foreground'a uygularsak
         * gerçek telefonda menü hemen kapanır.
         */
        if (!isCurrentlyActive) {
            return;
        }

        float maxTranslation =
                -adapter.getActionWidth();

        float translation =
                Math.max(
                        maxTranslation,
                        Math.min(
                                0f,
                                dX
                        )
                );

        foreground.setTranslationX(
                translation
        );
    }

    @Override
    public void clearView(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        /*
         * Önce ItemTouchHelper kendi temizliğini yapsın.
         */
        super.clearView(
                recyclerView,
                viewHolder
        );

        View foreground =
                adapter.getSwipeForeground(
                        viewHolder
                );

        if (foreground == null) {
            return;
        }

        int actionWidth =
                adapter.getActionWidth();

        float currentTranslation =
                foreground.getTranslationX();

        /*
         * Action alanının yaklaşık %30'una kadar
         * açıldıysa tamamen açık bırak.
         *
         * Daha az kaydırıldıysa tekrar kapat.
         */
        boolean shouldOpen =
                Math.abs(currentTranslation)
                        > actionWidth * 0.30f;

        foreground
                .animate()
                .translationX(
                        shouldOpen
                                ? -actionWidth
                                : 0f
                )
                .setDuration(160)
                .start();
    }
}
