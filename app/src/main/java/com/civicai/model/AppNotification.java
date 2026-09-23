package com.civicai.model;

import java.io.Serializable;

/**
 * In-app and push notification data model for Citizen and Government alerts.
 */
public class AppNotification implements Serializable {
    private String notificationId;
    private String userId;
    private UserRole targetRole;
    private String title;
    private String message;
    private String type; // e.g. STATUS_UPDATE, CLUSTER_ALERT, PRIORITY_ALERT
    private String complaintId;
    private boolean isRead;
    private long timestamp;

    public AppNotification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.targetRole = UserRole.CITIZEN;
    }

    public AppNotification(String notificationId, String userId, UserRole targetRole,
                           String title, String message, String type, String complaintId) {
        this();
        this.notificationId = notificationId;
        this.userId = userId;
        this.targetRole = targetRole;
        this.title = title;
        this.message = message;
        this.type = type;
        this.complaintId = complaintId;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public UserRole getTargetRole() { return targetRole; }
    public void setTargetRole(UserRole targetRole) { this.targetRole = targetRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
