package com.civicai.model;

/**
 * Priority levels recommended by AI or assigned by Government Officials.
 */
public enum Priority {
    HIGH,
    MEDIUM,
    LOW;

    public static Priority fromString(String priority) {
        if (priority == null) return MEDIUM;
        try {
            return Priority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
