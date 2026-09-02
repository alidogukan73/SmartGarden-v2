package com.alidogukan.avora.ui;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.GardenPhoto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Full-screen, swipeable viewer for photos belonging to one journal record. */
public final class GardenPhotoViewerDialog {
    private GardenPhotoViewerDialog() { }

    public static void show(Activity activity, List<GardenPhoto> source, String selectedId) {
        List<GardenPhoto> photos = availablePhotos(source);
        if (photos.isEmpty()) return;
        int initialPosition = initialPosition(photos, selectedId);

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_garden_photo_viewer);
        RecyclerView pager = dialog.findViewById(R.id.pagerGardenPhotos);
        TextView position = dialog.findViewById(R.id.txtGardenPhotoPagerPosition);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                activity, RecyclerView.HORIZONTAL, false);
        pager.setLayoutManager(layoutManager);
        pager.setAdapter(new PhotoPagerAdapter(activity, photos));
        pager.setItemAnimator(null);
        new PagerSnapHelper().attachToRecyclerView(pager);
        pager.scrollToPosition(initialPosition);
        updatePosition(activity, position, initialPosition, photos.size());
        pager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                int current = layoutManager.findFirstVisibleItemPosition();
                if (current >= 0) updatePosition(activity, position, current, photos.size());
            }
        });
        dialog.findViewById(R.id.btnGardenPhotoViewerClose)
                .setOnClickListener(view -> dialog.dismiss());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private static List<GardenPhoto> availablePhotos(List<GardenPhoto> source) {
        List<GardenPhoto> result = new ArrayList<>();
        if (source == null) return result;
        for (GardenPhoto photo : source) {
            if (photo == null || photo.getLocal_path() == null) continue;
            if (new File(photo.getLocal_path()).isFile()) result.add(photo);
        }
        return result;
    }

    private static int initialPosition(List<GardenPhoto> photos, String selectedId) {
        for (int index = 0; index < photos.size(); index++) {
            if (safe(selectedId).equals(safe(photos.get(index).getId()))) return index;
        }
        return 0;
    }

    private static void updatePosition(Activity activity, TextView target,
                                       int position, int total) {
        target.setText(activity.getString(
                R.string.runtime_photo_swipe_position, position + 1, total));
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static final class PhotoPagerAdapter
            extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoHolder> {
        private final Activity activity;
        private final List<GardenPhoto> photos;

        PhotoPagerAdapter(Activity activity, List<GardenPhoto> photos) {
            this.activity = activity;
            this.photos = photos;
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_garden_photo_page, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GardenPhoto photo = photos.get(position);
            holder.image.setImageDrawable(null);
            holder.image.setImageURI(Uri.fromFile(new File(photo.getLocal_path())));
            holder.image.setContentDescription(activity.getString(
                    R.string.runtime_photo_page_description, position + 1, photos.size()));
        }

        @Override public void onViewRecycled(@NonNull PhotoHolder holder) {
            holder.image.setImageDrawable(null);
        }

        @Override public long getItemId(int position) {
            return safe(photos.get(position).getId()).hashCode();
        }

        @Override public int getItemCount() { return photos.size(); }

        static final class PhotoHolder extends RecyclerView.ViewHolder {
            final ImageView image;

            PhotoHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.imgGardenPhotoPage);
            }
        }
    }
}
