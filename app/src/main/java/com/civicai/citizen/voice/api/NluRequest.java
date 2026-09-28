package com.civicai.citizen.voice.api;

import com.civicai.citizen.CitizenComplaintStore;

public class NluRequest {
    public String sessionId;
    public String message;
    public String conversationState;
    public CitizenComplaintStore.Draft complaintDraft;

    public NluRequest(String sessionId, String message, String conversationState, CitizenComplaintStore.Draft complaintDraft) {
        this.sessionId = sessionId;
        this.message = message;
        this.conversationState = conversationState;
        this.complaintDraft = complaintDraft;
    }
}
