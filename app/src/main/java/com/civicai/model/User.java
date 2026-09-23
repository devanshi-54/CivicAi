package com.civicai.model;

import java.io.Serializable;

/**
 * User model representing either a Citizen or a Government Official.
 */
public class User implements Serializable {
    private String userId;
    private UserRole role;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String department;
    private String ward;
    private long createdAt;

    public User() {
        this.role = UserRole.CITIZEN;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, UserRole role, String name, String email, String phone) {
        this.userId = userId;
        this.role = role;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
