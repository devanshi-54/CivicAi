package com.civicai.firebase;

import android.util.Log;
import com.civicai.model.Complaint;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Helper class for Cloud Firestore interactions.
 * Prepared with offline cache settings and graceful fallback if Firebase is not yet configured.
 */
public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (!initialized) {
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build();
                db.setFirestoreSettings(settings);
                initialized = true;
                Log.d(TAG, "Firestore initialized successfully with offline persistence.");
            } catch (Exception e) {
                Log.w(TAG, "Firestore initialization deferred (waiting for google-services config): " + e.getMessage());
            }
        }
    }

    public static void saveComplaintToFirestore(Complaint complaint) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaint.getComplaintId())
                    .set(complaint)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Complaint synced to Firestore: " + complaint.getComplaintId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to sync complaint to Firestore: " + e.getMessage()));
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
        }
    }
}
