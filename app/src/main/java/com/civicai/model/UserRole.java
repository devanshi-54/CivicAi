package com.civicai.model;

/**
 * User roles in the CivicAI system.
 * Governs screen access and authorization boundaries.
 */
public enum UserRole {
    CITIZEN,
    GOVERNMENT_OFFICIAL;

    public static UserRole fromString(String role) {
        if (role == null) return CITIZEN;
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CITIZEN;
        }
    }
}
