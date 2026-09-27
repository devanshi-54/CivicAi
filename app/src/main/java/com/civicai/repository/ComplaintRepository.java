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
        // Initialize representative sample records for UI verification
        Complaint c1 = new Complaint();
        c1.setComplaintId("CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c1.setUserId("user_citizen_1");
        c1.setCitizenName("Rajesh Kumar");
        c1.setTitle("Dangerous open pothole on Main High Street");
        c1.setDescription("Deep pothole near school crossing causing traffic hazards and two-wheeler accidents.");
        c1.setCategory("Roads & Infrastructure");
        c1.setLocationAddress("42 MG Road, Ward 12");
        c1.setLatitude(12.9716);
        c1.setLongitude(77.5946);
        c1.setCreatedAt(System.currentTimeMillis() - 7200000);
        c1.setStatus(ComplaintStatus.SUBMITTED);
        c1.setAiPriority(Priority.HIGH);
        c1.setAiSeverity("Critical");
        c1.setAiUrgency("Immediate (School Zone)");
        c1.setAiSafetyRisk("High accident probability");
        c1.setAiAffectedPeople(450);
        c1.setAiConfidence(0.94f);
        c1.setSuggestedDepartment("Roads & Highway Maintenance");
        c1.setSuggestedAction("Emergency patch and barricading");
        c1.setAiReason("High traffic density and proximity to elementary school entrance increases risk severity.");
        memoryCache.put(c1.getComplaintId(), c1);

        Complaint c2 = new Complaint();
        c2.setComplaintId("CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c2.setUserId("user_citizen_2");
        c2.setCitizenName("Priya Sharma");
        c2.setTitle("Overflowing drainage pipe near Market Square");
        c2.setDescription("Sewage water overflowing onto pedestrian sidewalk for the past 24 hours.");
        c2.setCategory("Water & Sewage");
        c2.setLocationAddress("Sector 4 Market Square");
        c2.setLatitude(12.9780);
        c2.setLongitude(77.6010);
        c2.setCreatedAt(System.currentTimeMillis() - 18000000);
        c2.setStatus(ComplaintStatus.UNDER_REVIEW);
        c2.setAiPriority(Priority.MEDIUM);
        c2.setAiSeverity("Moderate");
        c2.setAiUrgency("Within 24-48 Hours");
        c2.setAiSafetyRisk("Sanitation hazard");
        c2.setAiAffectedPeople(200);
        c2.setAiConfidence(0.88f);
        c2.setSuggestedDepartment("Water Supply & Sewerage Board");
        c2.setSuggestedAction("Deploy suction vehicle and inspect blockage");
        c2.setAiReason("Public health risk due to stagnant wastewater in high-footfall commercial area.");
        memoryCache.put(c2.getComplaintId(), c2);
    }

    @Override
    public void submitComplaint(Complaint complaint, RepositoryCallback<String> callback) {
        if (complaint == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Complaint cannot be null"));
            }
            return;
        }

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
