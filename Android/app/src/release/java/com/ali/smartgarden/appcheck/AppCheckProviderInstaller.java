package com.ali.smartgarden.appcheck;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** Installs Play Integrity attestation for production APKs. */
public final class AppCheckProviderInstaller {
    private AppCheckProviderInstaller() { }

    public static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
