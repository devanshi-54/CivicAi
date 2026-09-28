package com.civicai;

import android.app.Application;
import com.civicai.BuildConfig;
import com.civicai.firebase.FirestoreHelper;
import com.civicai.repository.UserRepository;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/**
 * Application entry point for CivicAI.
 * Initializes base logging, Firebase offline persistence, and global repositories.
 */
public class CivicAiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
        }
        FirestoreHelper.initialize();
        UserRepository.initialize(this);
    }
}
