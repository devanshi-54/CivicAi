package com.civicai;

import android.app.Application;
import com.civicai.firebase.FirestoreHelper;

/**
 * Application entry point for CivicAI.
 * Initializes base logging, Firebase offline persistence, and global repositories.
 */
public class CivicAiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirestoreHelper.initialize();
    }
}
