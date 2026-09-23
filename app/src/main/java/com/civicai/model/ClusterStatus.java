package com.civicai.model;

/**
 * Lifecycle states of an AI-assisted issue cluster.
 */
public enum ClusterStatus {
    AI_DETECTED,
    NEEDS_REVIEW,
    CONFIRMED,
    SEPARATE;

    public static ClusterStatus fromString(String status) {
        if (status == null) return AI_DETECTED;
        try {
            return ClusterStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AI_DETECTED;
        }
    }
}
