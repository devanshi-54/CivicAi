package com.civicai.model;

import java.io.Serializable;

/**
 * Structured AI triage output for a complaint.
 * AI recommendations are advisory and decision-support only.
 */
public class AiAnalysisResult implements Serializable {
    private String summary;
    private String category;
    private String severity;
    private String urgency;
    private String safetyRisk;
    private int affectedPeople;
    private Priority priority;
    private String reason;
    private String suggestedDepartment;
    private String suggestedAction;
    private float confidence;

    public AiAnalysisResult() {
        this.priority = Priority.MEDIUM;
        this.confidence = 0.0f;
    }

    public AiAnalysisResult(String summary, String category, String severity, String urgency,
                            String safetyRisk, int affectedPeople, Priority priority,
                            String reason, String suggestedDepartment, String suggestedAction,
                            float confidence) {
        this.summary = summary;
        this.category = category;
        this.severity = severity;
        this.urgency = urgency;
        this.safetyRisk = safetyRisk;
        this.affectedPeople = affectedPeople;
        this.priority = priority;
        this.reason = reason;
        this.suggestedDepartment = suggestedDepartment;
        this.suggestedAction = suggestedAction;
        this.confidence = confidence;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getSafetyRisk() { return safetyRisk; }
    public void setSafetyRisk(String safetyRisk) { this.safetyRisk = safetyRisk; }

    public int getAffectedPeople() { return affectedPeople; }
    public void setAffectedPeople(int affectedPeople) { this.affectedPeople = affectedPeople; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSuggestedDepartment() { return suggestedDepartment; }
    public void setSuggestedDepartment(String suggestedDepartment) { this.suggestedDepartment = suggestedDepartment; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
}
