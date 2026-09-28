package com.civicai.repository;

import android.util.Log;

import com.civicai.firebase.FirestoreHelper;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.civicai.model.User;

/**
 * Concrete implementation of IComplaintRepository.
 * Backed by Firestore with in-memory cache and sample fallback data for development and test workflows.
 */
public class ComplaintRepository implements IComplaintRepository {

    private static ComplaintRepository instance;
    private final Map<String, Complaint> memoryCache = new HashMap<>();

    private ComplaintRepository() {
        initSampleData();
    }

    public static synchronized ComplaintRepository getInstance() {
        if (instance == null) {
            instance = new ComplaintRepository();
        }
        return instance;
    }

    private void initSampleData() {
        // Disabled for production. Actual data must come from Firestore.
    }

    @Override
    public void submitComplaint(Complaint complaint, RepositoryCallback<String> callback) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Please sign in to continue."));
            }
            return;
        }

        if (complaint == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Complaint cannot be null"));
            }
            return;
        }

        // Force ownership to authenticated user
        complaint.setUserId(fUser.getUid());

        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                complaint.setCitizenName(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Citizen");
                proceedSubmit(complaint, callback);
            }
            @Override
            public void onError(Exception e) {
                complaint.setCitizenName("Citizen");
                proceedSubmit(complaint, callback);
            }
        });
    }

    private void proceedSubmit(Complaint complaint, RepositoryCallback<String> callback) {
        if (complaint.getComplaintId() == null || complaint.getComplaintId().isEmpty()) {
            complaint.setComplaintId("CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (complaint.getCreatedAt() <= 0) {
            complaint.setCreatedAt(System.currentTimeMillis());
        }
        complaint.setUpdatedAt(System.currentTimeMillis());

        memoryCache.put(complaint.getComplaintId(), complaint);

        FirestoreHelper.saveComplaintToFirestore(complaint, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.w("ComplaintRepository", "Firestore save deferred/failed, preserved in memoryCache: " + exception.getMessage());
                if (callback != null) {
                    callback.onSuccess(complaint.getComplaintId());
                }
            }
        });
    }

    @Override
    public void getComplaintById(String complaintId, RepositoryCallback<Complaint> callback) {
        if (complaintId == null || complaintId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Invalid complaintId"));
            }
            return;
        }

        FirestoreHelper.getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint remoteComplaint) {
                if (remoteComplaint != null) {
                    memoryCache.put(remoteComplaint.getComplaintId(), remoteComplaint);
                    if (callback != null) {
                        callback.onSuccess(remoteComplaint);
                    }
                } else {
                    fallbackToCache();
                }
            }

            @Override
            public void onError(Exception exception) {
                fallbackToCache();
            }

            private void fallbackToCache() {
                Complaint cached = memoryCache.get(complaintId);
                if (cached != null) {
                    if (callback != null) {
                        callback.onSuccess(cached);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new Exception("Complaint not found with ID: " + complaintId));
                    }
                }
            }
        });
    }

    @Override
    public void getAllComplaints(RepositoryCallback<List<Complaint>> callback) {
        FirestoreHelper.getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> firestoreList) {
                Map<String, Complaint> combined = new HashMap<>(memoryCache);
                if (firestoreList != null) {
                    for (Complaint fc : firestoreList) {
                        combined.put(fc.getComplaintId(), fc);
                    }
                }
                memoryCache.putAll(combined);
                List<Complaint> list = new ArrayList<>(combined.values());
                Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                if (callback != null) {
                    callback.onSuccess(list);
                }
            }

            @Override
            public void onError(Exception exception) {
                List<Complaint> fallback = new ArrayList<>(memoryCache.values());
                Collections.sort(fallback, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                if (callback != null) {
                    callback.onSuccess(fallback);
                }
            }
        });
    }

    @Override
    public void getComplaintsByUser(String userId, RepositoryCallback<List<Complaint>> callback) {
        FirestoreHelper.getComplaintsByUser(userId, new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> firestoreList) {
                Map<String, Complaint> combined = new HashMap<>();
                for (Complaint c : memoryCache.values()) {
                    if (userId == null || userId.equals(c.getUserId())) {
                        combined.put(c.getComplaintId(), c);
                    }
                }
                if (firestoreList != null) {
                    for (Complaint c : firestoreList) {
                        combined.put(c.getComplaintId(), c);
                        memoryCache.put(c.getComplaintId(), c);
                    }
                }
                List<Complaint> list = new ArrayList<>(combined.values());
                Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                if (callback != null) {
                    callback.onSuccess(list);
                }
            }

            @Override
            public void onError(Exception exception) {
                List<Complaint> results = new ArrayList<>();
                for (Complaint c : memoryCache.values()) {
                    if (userId == null || userId.equals(c.getUserId())) {
                        results.add(c);
                    }
                }
                Collections.sort(results, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                if (callback != null) {
                    callback.onSuccess(results);
                }
            }
        });
    }

    @Override
    public void getComplaintsByPriority(Priority priority, RepositoryCallback<List<Complaint>> callback) {
        getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> all) {
                List<Complaint> results = new ArrayList<>();
                for (Complaint c : all) {
                    if (c.getEffectivePriority() == priority) {
                        results.add(c);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(results);
                }
            }

            @Override
            public void onError(Exception exception) {
                List<Complaint> results = new ArrayList<>();
                for (Complaint c : memoryCache.values()) {
                    if (c.getEffectivePriority() == priority) {
                        results.add(c);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(results);
                }
            }
        });
    }

    @Override
    public void getComplaintsByDepartment(String department, RepositoryCallback<List<Complaint>> callback) {
        getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> all) {
                List<Complaint> results = new ArrayList<>();
                for (Complaint c : all) {
                    if (department != null && department.equalsIgnoreCase(c.getAssignedDepartment())) {
                        results.add(c);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(results);
                }
            }

            @Override
            public void onError(Exception exception) {
                List<Complaint> results = new ArrayList<>();
                for (Complaint c : memoryCache.values()) {
                    if (department != null && department.equalsIgnoreCase(c.getAssignedDepartment())) {
                        results.add(c);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(results);
                }
            }
        });
    }

    @Override
    public void updateOfficialDecision(String complaintId, Priority officialPriority, String department,
                                       String officer, String decision, String internalNotes,
                                       ComplaintStatus newStatus, RepositoryCallback<Void> callback) {
        Complaint c = memoryCache.get(complaintId);
        if (c != null) {
            c.setOfficialPriority(officialPriority);
            c.setAssignedDepartment(department);
            c.setAssignedOfficer(officer);
            c.setOfficialDecision(decision);
            c.setInternalNotes(internalNotes);
            if (newStatus != null) {
                c.setStatus(newStatus);
            }
            c.setUpdatedAt(System.currentTimeMillis());
        }

        FirestoreHelper.updateOfficialDecision(complaintId, officialPriority, department,
                officer, decision, internalNotes, newStatus, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.w("ComplaintRepository", "Firestore update deferred/failed, preserved in memoryCache: " + exception.getMessage());
                        if (c != null) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(exception);
                            }
                        }
                    }
                });
    }

    @Override
    public void updateStatus(String complaintId, ComplaintStatus status, RepositoryCallback<Void> callback) {
        Complaint c = memoryCache.get(complaintId);
        if (c != null) {
            c.setStatus(status);
            c.setUpdatedAt(System.currentTimeMillis());
        }

        FirestoreHelper.updateStatus(complaintId, status, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.w("ComplaintRepository", "Firestore status update deferred/failed, preserved in memoryCache: " + exception.getMessage());
                if (c != null) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                }
            }
        });
    }
}
