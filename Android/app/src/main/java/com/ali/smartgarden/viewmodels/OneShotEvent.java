package com.ali.smartgarden.viewmodels;

/** Delivers a ViewModel result once across Activity recreation. */
public final class OneShotEvent<T> {
    private final T value;
    private boolean handled;

    public OneShotEvent(T value) {
        this.value = value;
    }

    public synchronized T consume() {
        if (handled) return null;
        handled = true;
        return value;
    }
}
