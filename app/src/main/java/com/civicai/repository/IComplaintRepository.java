package com.civicai.repository;

import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;

import java.util.List;

/**
 * Interface defining complaint data access operations for Citizen and Government views.
 */
public interface IComplaintRepository {
    void submitComplaint(Complaint complaint, RepositoryCallback<String> callback);
    void getComplaintById(String complaintId, RepositoryCallback<Complaint> callback);
    void getComplaintsByUser(String userId, RepositoryCallback<List<Complaint>> callback);
    void getAllComplaints(RepositoryCallback<List<Complaint>> callback);
    void getComplaintsByPriority(Priority priority, RepositoryCallback<List<Complaint>> callback);
    void getComplaintsByDepartment(String department, RepositoryCallback<List<Complaint>> callback);
    void updateOfficialDecision(String complaintId, Priority officialPriority, String department,
                                String officer, String decision, String internalNotes,
                                ComplaintStatus newStatus, RepositoryCallback<Void> callback);
    void updateStatus(String complaintId, ComplaintStatus status, RepositoryCallback<Void> callback);
}
