package com.civicai.model;

/**
 * Lifecycle states of a civic complaint.
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    REJECTED("Rejected");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ComplaintStatus fromString(String status) {
        if (status == null) return SUBMITTED;
        try {
            return ComplaintStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SUBMITTED;
        }
    }
}
