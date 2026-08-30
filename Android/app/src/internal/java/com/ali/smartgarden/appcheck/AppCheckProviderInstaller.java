package com.ali.smartgarden.appcheck;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/** Installs the device-registered App Check provider for private internal APKs. */
public final class AppCheckProviderInstaller {
    private AppCheckProviderInstaller() { }

    public static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
