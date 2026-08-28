package com.ali.smartgarden.firebase;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Firebase-backed LiveData that owns exactly one listener while it has active observers.
 * Reopening a screen therefore does not leave an orphaned Firebase listener behind.
 */
public final class FirebaseLiveData<T> extends MutableLiveData<T> {
    private final Query query;
    private ValueEventListener eventListener;
    private boolean listening;

    public FirebaseLiveData(@NonNull Query query) {
        this.query = query;
    }

    public FirebaseLiveData<T> setEventListener(@NonNull ValueEventListener listener) {
        if (eventListener != null) {
            throw new IllegalStateException("Firebase listener is already configured");
        }
        eventListener = listener;
        if (hasActiveObservers()) {
            startListening();
        }
        return this;
    }

    @Override
    protected void onActive() {
        super.onActive();
        startListening();
    }

    @Override
    protected void onInactive() {
        stopListening();
        super.onInactive();
    }

    private void startListening() {
        if (!listening && eventListener != null) {
            query.addValueEventListener(eventListener);
            listening = true;
        }
    }

    private void stopListening() {
        if (listening && eventListener != null) {
            query.removeEventListener(eventListener);
            listening = false;
        }
    }
}
