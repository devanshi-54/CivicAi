package com.civicai.firebase;

import android.util.Log;
import android.net.Uri;

import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;
import com.civicai.model.User;
import com.civicai.repository.RepositoryCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Cloud Firestore interactions.
 * Prepared with offline cache settings and graceful fallback if Firebase is not yet configured.
 */
public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private static boolean initialized = false;

    public static void uploadComplaintImage(Uri imageUri, String complaintId, RepositoryCallback<String> callback) {
        if (imageUri == null || complaintId == null || complaintId.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Image or complaint ID is missing"));
            return;
        }
        try {
            FirebaseStorage.getInstance().getReference()
                    .child(FirebaseConfig.STORAGE_COMPLAINT_IMAGES + complaintId + ".jpg")
                    .putFile(imageUri)
                    .addOnSuccessListener(task -> task.getStorage().getDownloadUrl()
                            .addOnSuccessListener(uri -> { if (callback != null) callback.onSuccess(uri.toString()); })
                            .addOnFailureListener(error -> { if (callback != null) callback.onError(error); }))
                    .addOnFailureListener(error -> { if (callback != null) callback.onError(error); });
        } catch (Exception error) {
            if (callback != null) callback.onError(error);
        }
    }

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
        saveComplaintToFirestore(complaint, null);
    }

    public static void saveUserToFirestore(User user, RepositoryCallback<Void> callback) {
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("User or userId is invalid."));
            return;
        }
        try {
            initialize();
            FirebaseFirestore.getInstance().collection(FirebaseConfig.COLLECTION_USERS)
                    .document(user.getUserId()).set(user, SetOptions.merge())
                    .addOnSuccessListener(result -> { if (callback != null) callback.onSuccess(null); })
                    .addOnFailureListener(error -> { if (callback != null) callback.onError(error); });
        } catch (Exception error) {
            if (callback != null) callback.onError(error);
        }
    }

    public static void getUserById(String userId, RepositoryCallback<User> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid userId"));
            return;
        }
        try {
            initialize();
            FirebaseFirestore.getInstance().collection(FirebaseConfig.COLLECTION_USERS)
                    .document(userId).get()
                    .addOnSuccessListener(document -> {
                        User user = document.exists() ? document.toObject(User.class) : null;
                        if (user != null && (user.getUserId() == null || user.getUserId().isEmpty())) {
                            user.setUserId(document.getId());
                        }
                        if (callback != null) {
                            if (user == null) callback.onError(new Exception("User not found: " + userId));
                            else callback.onSuccess(user);
                        }
                    })
                    .addOnFailureListener(error -> { if (callback != null) callback.onError(error); });
        } catch (Exception error) {
            if (callback != null) callback.onError(error);
        }
    }

    public static void saveComplaintToFirestore(Complaint complaint, RepositoryCallback<String> callback) {
        if (complaint == null || complaint.getComplaintId() == null || complaint.getComplaintId().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Complaint or complaintId is invalid."));
            }
            return;
        }

        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaint.getComplaintId())
                    .set(complaint)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Complaint synced to Firestore: " + complaint.getComplaintId());
                        if (callback != null) {
                            callback.onSuccess(complaint.getComplaintId());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to sync complaint to Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void getComplaintById(String complaintId, RepositoryCallback<Complaint> callback) {
        if (complaintId == null || complaintId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Invalid complaintId"));
            }
            return;
        }

        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaintId)
                    .get(Source.SERVER)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            try {
                                Complaint complaint = documentSnapshot.toObject(Complaint.class);
                                if (complaint != null) {
                                    if (complaint.getComplaintId() == null || complaint.getComplaintId().isEmpty()) {
                                        complaint.setComplaintId(documentSnapshot.getId());
                                    }
                                    if (callback != null) {
                                        callback.onSuccess(complaint);
                                    }
                                    return;
                                }
                            } catch (Exception ex) {
                                Log.w(TAG, "Error deserializing complaint " + complaintId + ": " + ex.getMessage());
                            }
                        }
                        if (callback != null) {
                            callback.onError(new Exception("Complaint not found with ID: " + complaintId));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to get complaint from Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void getAllComplaints(RepositoryCallback<List<Complaint>> callback) {
        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Complaint> complaints = new ArrayList<>();
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                try {
                                    Complaint c = doc.toObject(Complaint.class);
                                    if (c != null) {
                                        if (c.getComplaintId() == null || c.getComplaintId().isEmpty()) {
                                            c.setComplaintId(doc.getId());
                                        }
                                        complaints.add(c);
                                    }
                                } catch (Exception ex) {
                                    Log.w(TAG, "Failed to deserialize complaint " + doc.getId() + ": " + ex.getMessage());
                                }
                            }
                        }
                        Collections.sort(complaints, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        if (callback != null) {
                            callback.onSuccess(complaints);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to get all complaints from Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void getComplaintsByUser(String userId, RepositoryCallback<List<Complaint>> callback) {
        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Query query = db.collection(FirebaseConfig.COLLECTION_COMPLAINTS);
            if (userId != null && !userId.isEmpty()) {
                query = query.whereEqualTo("userId", userId);
            }
            query.get(Source.SERVER)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Complaint> complaints = new ArrayList<>();
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                try {
                                    Complaint c = doc.toObject(Complaint.class);
                                    if (c != null) {
                                        if (c.getComplaintId() == null || c.getComplaintId().isEmpty()) {
                                            c.setComplaintId(doc.getId());
                                        }
                                        complaints.add(c);
                                    }
                                } catch (Exception ex) {
                                    Log.w(TAG, "Failed to deserialize user complaint " + doc.getId() + ": " + ex.getMessage());
                                }
                            }
                        }
                        Collections.sort(complaints, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        if (callback != null) {
                            callback.onSuccess(complaints);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to get complaints for user from Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void updateOfficialDecision(String complaintId, Priority officialPriority, String department,
                                              String officer, String decision, String internalNotes,
                                              ComplaintStatus newStatus, RepositoryCallback<Void> callback) {
        if (complaintId == null || complaintId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Invalid complaintId"));
            }
            return;
        }

        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> updates = new HashMap<>();
            if (officialPriority != null) {
                updates.put("officialPriority", officialPriority.name());
            }
            if (department != null) {
                updates.put("assignedDepartment", department);
            }
            if (officer != null) {
                updates.put("assignedOfficer", officer);
            }
            if (decision != null) {
                updates.put("officialDecision", decision);
            }
            if (internalNotes != null) {
                updates.put("internalNotes", internalNotes);
            }
            if (newStatus != null) {
                updates.put("status", newStatus.name());
            }
            updates.put("updatedAt", System.currentTimeMillis());

            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaintId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Official decision updated in Firestore for: " + complaintId);
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to update official decision in Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void updateStatus(String complaintId, ComplaintStatus status, RepositoryCallback<Void> callback) {
        if (complaintId == null || complaintId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Invalid complaintId"));
            }
            return;
        }

        try {
            initialize();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> updates = new HashMap<>();
            if (status != null) {
                updates.put("status", status.name());
            }
            updates.put("updatedAt", System.currentTimeMillis());

            db.collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaintId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Status updated in Firestore for: " + complaintId);
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to update status in Firestore: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static void submitComplaintFeedback(String complaintId, String userId, int rating, String message,
                                               RepositoryCallback<Void> callback) {
        if (complaintId == null || complaintId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Complaint and citizen IDs are required"));
            return;
        }
        if (rating < 1 || rating > 5) {
            if (callback != null) callback.onError(new IllegalArgumentException("Select a rating from 1 to 5"));
            return;
        }
        try {
            initialize();
            Map<String, Object> feedback = new HashMap<>();
            feedback.put("feedbackComplaintId", complaintId);
            feedback.put("feedbackUserId", userId);
            feedback.put("citizenFeedbackRating", rating);
            feedback.put("citizenFeedback", message == null ? "" : message.trim());
            feedback.put("feedbackSubmittedAt", System.currentTimeMillis());
            FirebaseFirestore.getInstance().collection(FirebaseConfig.COLLECTION_COMPLAINTS)
                    .document(complaintId).update(feedback)
                    .addOnSuccessListener(reference -> { if (callback != null) callback.onSuccess(null); })
                    .addOnFailureListener(error -> { if (callback != null) callback.onError(error); });
        } catch (Exception error) {
            if (callback != null) callback.onError(error);
        }
    }
}
