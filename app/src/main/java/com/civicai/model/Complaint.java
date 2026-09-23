package com.civicai.model;

import java.io.Serializable;

/**
 * Primary CivicAI Complaint data model.
 * Holds core complaint details, advisory AI recommendations, official decisions, and cluster associations.
 */
public class Complaint implements Serializable {
    // Core Complaint Fields
    private String complaintId;
    private String userId;
    private String citizenName;
    private String title;
    private String description;
    private String category;
    private String locationAddress;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private long createdAt;
    private long updatedAt;
    private ComplaintStatus status;

    // AI Recommendation Fields (Decision Support Only)
    private String aiCategory;
    private String aiSummary;
    private String aiSeverity;
    private String aiUrgency;
    private String aiSafetyRisk;
    private int aiAffectedPeople;
    private Priority aiPriority;
    private String aiReason;
    private String suggestedDepartment;
    private String suggestedAction;
    private float aiConfidence;

    // Government Official Fields (Authoritative Decisions)
    private Priority officialPriority;
    private String assignedDepartment;
    private String assignedOfficer;
    private String officialDecision;
    private String internalNotes;
    private String resolutionDetails;

    // Cluster Fields
    private String clusterId;
    private ClusterStatus clusterStatus;

    public Complaint() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.status = ComplaintStatus.SUBMITTED;
        this.aiPriority = Priority.MEDIUM;
    }

    // Core Getters & Setters
    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    // AI Recommendation Getters & Setters
    public String getAiCategory() { return aiCategory; }
    public void setAiCategory(String aiCategory) { this.aiCategory = aiCategory; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiSeverity() { return aiSeverity; }
    public void setAiSeverity(String aiSeverity) { this.aiSeverity = aiSeverity; }

    public String getAiUrgency() { return aiUrgency; }
    public void setAiUrgency(String aiUrgency) { this.aiUrgency = aiUrgency; }

    public String getAiSafetyRisk() { return aiSafetyRisk; }
    public void setAiSafetyRisk(String aiSafetyRisk) { this.aiSafetyRisk = aiSafetyRisk; }

    public int getAiAffectedPeople() { return aiAffectedPeople; }
    public void setAiAffectedPeople(int aiAffectedPeople) { this.aiAffectedPeople = aiAffectedPeople; }

    public Priority getAiPriority() { return aiPriority; }
    public void setAiPriority(Priority aiPriority) { this.aiPriority = aiPriority; }

    public String getAiReason() { return aiReason; }
    public void setAiReason(String aiReason) { this.aiReason = aiReason; }

    public String getSuggestedDepartment() { return suggestedDepartment; }
    public void setSuggestedDepartment(String suggestedDepartment) { this.suggestedDepartment = suggestedDepartment; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public float getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(float aiConfidence) { this.aiConfidence = aiConfidence; }

    // Government Decision Getters & Setters
    public Priority getOfficialPriority() { return officialPriority; }
    public void setOfficialPriority(Priority officialPriority) { this.officialPriority = officialPriority; }

    public String getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(String assignedDepartment) { this.assignedDepartment = assignedDepartment; }

    public String getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(String assignedOfficer) { this.assignedOfficer = assignedOfficer; }

    public String getOfficialDecision() { return officialDecision; }
    public void setOfficialDecision(String officialDecision) { this.officialDecision = officialDecision; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public String getResolutionDetails() { return resolutionDetails; }
    public void setResolutionDetails(String resolutionDetails) { this.resolutionDetails = resolutionDetails; }

    // Cluster Getters & Setters
    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public ClusterStatus getClusterStatus() { return clusterStatus; }
    public void setClusterStatus(ClusterStatus clusterStatus) { this.clusterStatus = clusterStatus; }

    public Priority getEffectivePriority() {
        if (officialPriority != null) {
            return officialPriority;
        }
        return aiPriority != null ? aiPriority : Priority.MEDIUM;
    }
}
