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

/** Complaint access backed by Firestore, with a memory cache for local workflows. */
public class ComplaintRepository implements IComplaintRepository {
    private static ComplaintRepository instance;
    private final Map<String, Complaint> memoryCache = new HashMap<>();

    private ComplaintRepository() { }

    public static synchronized ComplaintRepository getInstance() {
        if (instance == null) instance = new ComplaintRepository();
        return instance;
    }

    @Override
    public void submitComplaint(Complaint complaint, RepositoryCallback<String> callback) {
        if (complaint == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Complaint cannot be null"));
            return;
        }
        if (complaint.getComplaintId() == null || complaint.getComplaintId().isEmpty()) {
            complaint.setComplaintId("CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (complaint.getCreatedAt() <= 0) complaint.setCreatedAt(System.currentTimeMillis());
        complaint.setUpdatedAt(System.currentTimeMillis());
        memoryCache.put(complaint.getComplaintId(), complaint);
        FirestoreHelper.saveComplaintToFirestore(complaint, new RepositoryCallback<String>() {
            @Override public void onSuccess(String id) { if (callback != null) callback.onSuccess(id); }
            @Override public void onError(Exception error) {
                Log.w("ComplaintRepository", "Firestore save failed", error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    @Override
    public void getComplaintById(String complaintId, RepositoryCallback<Complaint> callback) {
        if (complaintId == null || complaintId.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid complaintId"));
            return;
        }
        FirestoreHelper.getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override public void onSuccess(Complaint complaint) {
                memoryCache.put(complaint.getComplaintId(), complaint);
                if (callback != null) callback.onSuccess(complaint);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    @Override
    public void getAllComplaints(RepositoryCallback<List<Complaint>> callback) {
        FirestoreHelper.getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override public void onSuccess(List<Complaint> complaints) {
                List<Complaint> result = complaints == null ? new ArrayList<>() : new ArrayList<>(complaints);
                for (Complaint complaint : result) memoryCache.put(complaint.getComplaintId(), complaint);
                sortNewestFirst(result);
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    @Override
    public void getComplaintsByUser(String userId, RepositoryCallback<List<Complaint>> callback) {
        FirestoreHelper.getComplaintsByUser(userId, new RepositoryCallback<List<Complaint>>() {
            @Override public void onSuccess(List<Complaint> complaints) {
                List<Complaint> result = complaints == null ? new ArrayList<>() : new ArrayList<>(complaints);
                for (Complaint complaint : result) memoryCache.put(complaint.getComplaintId(), complaint);
                sortNewestFirst(result);
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    @Override
    public void getComplaintsByPriority(Priority priority, RepositoryCallback<List<Complaint>> callback) {
        filterComplaints(complaint -> priority != null && complaint.getEffectivePriority() == priority, callback);
    }

    @Override
    public void getComplaintsByDepartment(String department, RepositoryCallback<List<Complaint>> callback) {
        filterComplaints(complaint -> department != null && complaint.getAssignedDepartment() != null
                && department.equalsIgnoreCase(complaint.getAssignedDepartment()), callback);
    }

    private void filterComplaints(java.util.function.Predicate<Complaint> predicate,
                                  RepositoryCallback<List<Complaint>> callback) {
        getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override public void onSuccess(List<Complaint> all) {
                List<Complaint> filtered = new ArrayList<>();
                for (Complaint complaint : all) if (predicate.test(complaint)) filtered.add(complaint);
                if (callback != null) callback.onSuccess(filtered);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    @Override
    public void updateOfficialDecision(String complaintId, Priority officialPriority, String department,
                                       String officer, String decision, String internalNotes,
                                       ComplaintStatus newStatus, RepositoryCallback<Void> callback) {
        FirestoreHelper.updateOfficialDecision(complaintId, officialPriority, department, officer,
                decision, internalNotes, newStatus, new RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void ignored) {
                        Complaint cached = memoryCache.get(complaintId);
                        if (cached != null) {
                            if (officialPriority != null) cached.setOfficialPriority(officialPriority);
                            if (department != null) cached.setAssignedDepartment(department);
                            if (officer != null) cached.setAssignedOfficer(officer);
                            if (decision != null) cached.setOfficialDecision(decision);
                            if (internalNotes != null) cached.setInternalNotes(internalNotes);
                            if (newStatus != null) cached.setStatus(newStatus);
                            cached.setUpdatedAt(System.currentTimeMillis());
                        }
                        if (callback != null) callback.onSuccess(null);
                    }
                    @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
                });
    }

    @Override
    public void updateStatus(String complaintId, ComplaintStatus status, RepositoryCallback<Void> callback) {
        FirestoreHelper.updateStatus(complaintId, status, new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void ignored) {
                Complaint cached = memoryCache.get(complaintId);
                if (cached != null && status != null) {
                    cached.setStatus(status);
                    cached.setUpdatedAt(System.currentTimeMillis());
                }
                if (callback != null) callback.onSuccess(null);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    private static void sortNewestFirst(List<Complaint> complaints) {
        Collections.sort(complaints, (first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));
    }
}