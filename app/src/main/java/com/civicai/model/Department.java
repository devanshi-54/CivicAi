package com.civicai.model;

import java.io.Serializable;

/**
 * Civic department entity (e.g. Roads & Infrastructure, Water & Sewage, Sanitation, Electricity).
 */
public class Department implements Serializable {
    private String departmentId;
    private String name;
    private String code;
    private String contactEmail;
    private String contactPhone;
    private int activeOfficersCount;
    private int openComplaintsCount;

    public Department() {}

    public Department(String departmentId, String name, String code, String contactEmail,
                      String contactPhone, int activeOfficersCount, int openComplaintsCount) {
        this.departmentId = departmentId;
        this.name = name;
        this.code = code;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.activeOfficersCount = activeOfficersCount;
        this.openComplaintsCount = openComplaintsCount;
    }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public int getActiveOfficersCount() { return activeOfficersCount; }
    public void setActiveOfficersCount(int activeOfficersCount) { this.activeOfficersCount = activeOfficersCount; }

    public int getOpenComplaintsCount() { return openComplaintsCount; }
    public void setOpenComplaintsCount(int openComplaintsCount) { this.openComplaintsCount = openComplaintsCount; }
}
