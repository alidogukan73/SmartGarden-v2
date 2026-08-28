package com.ali.smartgarden.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseLiveDataTest {

    @Test
    public void activeObserversOwnExactlyOneFirebaseListener() {
        AtomicInteger additions = new AtomicInteger();
        AtomicInteger removals = new AtomicInteger();
        FirebaseLiveData<String> liveData = new FirebaseLiveData<>(
                new FirebaseLiveData.ListenerRegistration() {
                    @Override
                    public void add(ValueEventListener listener) {
                        additions.incrementAndGet();
                    }

                    @Override
                    public void remove(ValueEventListener listener) {
                        removals.incrementAndGet();
                    }
                });
        liveData.setEventListener(listener());

        liveData.onActive();
        liveData.onActive();
        assertEquals(1, additions.get());

        liveData.onInactive();
        liveData.onInactive();
        assertEquals(1, removals.get());

        liveData.onActive();
        assertEquals(2, additions.get());
    }

    @Test
    public void listenerCanOnlyBeConfiguredOnce() {
        FirebaseLiveData<String> liveData = new FirebaseLiveData<>(
                new FirebaseLiveData.ListenerRegistration() {
                    @Override public void add(ValueEventListener listener) { }
                    @Override public void remove(ValueEventListener listener) { }
                });
        liveData.setEventListener(listener());
        assertThrows(IllegalStateException.class,
                () -> liveData.setEventListener(listener()));
    }

    private static ValueEventListener listener() {
        return new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
    }
}
