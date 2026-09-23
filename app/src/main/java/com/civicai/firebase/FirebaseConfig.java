package com.civicai.firebase;

/**
 * Constants and collection paths for Firebase Firestore and Storage.
 */
public final class FirebaseConfig {
    private FirebaseConfig() {}

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_COMPLAINTS = "complaints";
    public static final String COLLECTION_CLUSTERS = "issue_clusters";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_DEPARTMENTS = "departments";
    public static final String COLLECTION_ANALYTICS = "analytics_snapshots";

    // Storage Paths
    public static final String STORAGE_COMPLAINT_IMAGES = "complaint_images/";
    public static final String STORAGE_USER_PROFILES = "user_profiles/";
}
