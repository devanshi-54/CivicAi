package com.civicai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-assisted Issue Cluster representing geographically or contextually grouped complaints.
 * Requires official review and validation.
 */
public class IssueCluster implements Serializable {
    private String clusterId;
    private String title;
    private String category;
    private String locationArea;
    private double centerLatitude;
    private double centerLongitude;
    private List<String> complaintIds;
    private ClusterStatus status;
    private float aiConfidence;
    private String aiGroupingReason;
    private long createdAt;
    private long updatedAt;

    public IssueCluster() {
        this.complaintIds = new ArrayList<>();
        this.status = ClusterStatus.AI_DETECTED;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public IssueCluster(String clusterId, String title, String category, String locationArea,
                        double centerLatitude, double centerLongitude, float aiConfidence,
                        String aiGroupingReason) {
        this();
        this.clusterId = clusterId;
        this.title = title;
        this.category = category;
        this.locationArea = locationArea;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.aiConfidence = aiConfidence;
        this.aiGroupingReason = aiGroupingReason;
    }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocationArea() { return locationArea; }
    public void setLocationArea(String locationArea) { this.locationArea = locationArea; }

    public double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(double centerLatitude) { this.centerLatitude = centerLatitude; }

    public double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(double centerLongitude) { this.centerLongitude = centerLongitude; }

    public List<String> getComplaintIds() { return complaintIds; }
    public void setComplaintIds(List<String> complaintIds) { this.complaintIds = complaintIds; }

    public ClusterStatus getStatus() { return status; }
    public void setStatus(ClusterStatus status) { this.status = status; }

    public float getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(float aiConfidence) { this.aiConfidence = aiConfidence; }

    public String getAiGroupingReason() { return aiGroupingReason; }
    public void setAiGroupingReason(String aiGroupingReason) { this.aiGroupingReason = aiGroupingReason; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getComplaintCount() {
        return complaintIds != null ? complaintIds.size() : 0;
    }
}
