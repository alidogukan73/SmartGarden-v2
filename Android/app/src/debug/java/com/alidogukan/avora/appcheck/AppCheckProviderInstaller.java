package com.alidogukan.avora.appcheck;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/** Installs the emulator/development-only App Check provider for debug APKs. */
public final class AppCheckProviderInstaller {
    private AppCheckProviderInstaller() { }

    public static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
