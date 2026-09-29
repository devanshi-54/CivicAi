package com.civicai.citizen.voice;

import android.text.TextUtils;
import android.util.Log;

import com.civicai.citizen.CitizenComplaintStore;
import com.civicai.citizen.voice.api.GeminiApiClient;
import com.civicai.citizen.voice.api.GeminiApiService;
import com.civicai.citizen.voice.api.NluRequest;
import com.civicai.citizen.voice.api.NluResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.UUID;

public class ConversationEngine {

    public interface ConversationCallback {
        void onSpeak(String text);
        void onStateChanged(ConversationState state, CitizenComplaintStore.Draft draft);
        void onComplaintAction(String action);
        void onStatus(String status);
    }

    private ConversationState currentState = ConversationState.IDLE;
    private final CitizenComplaintStore.Draft draft = new CitizenComplaintStore.Draft();
    private final ConversationCallback callback;
    private final GeminiApiService apiService;
    private final String sessionId;

    public ConversationEngine(ConversationCallback callback) {
        this.callback = callback;
        this.apiService = GeminiApiClient.getService();
        this.sessionId = UUID.randomUUID().toString();
    }

    public void startConversation() {
        currentState = ConversationState.COLLECTING_CATEGORY;
        callback.onStateChanged(currentState, draft);
        callback.onSpeak("What type of civic issue would you like to report?");
    }

    public void processInput(String input) {
        if (TextUtils.isEmpty(input)) {
            callback.onSpeak("I didn't catch that. Please repeat.");
            return;
        }

        String lowerInput = input.toLowerCase().trim();

        // Local failsafes for cancellation and direct action
        if (lowerInput.equals("cancel") || lowerInput.equals("stop") || lowerInput.equals("cancel complaint") || lowerInput.equals("i don't want to submit")) {
            currentState = ConversationState.IDLE;
            callback.onStateChanged(currentState, draft);
            callback.onSpeak("Complaint cancelled.");
            return;
        }
        
        if (currentState == ConversationState.FINAL_REVIEW) {
             if (lowerInput.equals("submit") || lowerInput.equals("confirm") || lowerInput.equals("yes submit it")) {
                 callback.onComplaintAction("SUBMIT_COMPLAINT");
                 currentState = ConversationState.SUBMITTED;
                 callback.onStateChanged(currentState, draft);
                 return;
             }
        }

        callback.onStatus("Thinking...");

        NluRequest request = new NluRequest(sessionId, input, currentState.name(), draft);
        
        apiService.processMessage(request).enqueue(new Callback<NluResponse>() {
            @Override
            public void onResponse(Call<NluResponse> call, Response<NluResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleNluResponse(response.body());
                } else {
                    Log.e("ConversationEngine", "NLU API Error: " + response.code());
                    callback.onSpeak("Sorry, I'm having trouble connecting to the NLU service.");
                }
            }

            @Override
            public void onFailure(Call<NluResponse> call, Throwable t) {
                Log.e("ConversationEngine", "Network failure", t);
                callback.onSpeak("Network error. Please try again.");
            }
        });
    }

    private void handleNluResponse(NluResponse response) {
        String intent = response.intent;
        
        if ("CANCEL_COMPLAINT".equals(intent)) {
            currentState = ConversationState.IDLE;
            callback.onStateChanged(currentState, draft);
            callback.onSpeak("Complaint cancelled.");
            return;
        }
        
        if ("RESTART_COMPLAINT".equals(intent) || Boolean.TRUE.equals(response.restart)) {
            draft.title = "";
            draft.category = "";
            draft.description = "";
            draft.locationAddress = "";
            startConversation();
            return;
        }

        if ("TRACK_COMPLAINT".equals(intent) || "CHECK_STATUS".equals(intent) || "LIST_COMPLAINTS".equals(intent) || "OPEN_GRIEVANCES".equals(intent)) {
            callback.onComplaintAction("TRACK_COMPLAINTS");
            callback.onSpeak("Here are your complaints.");
            return;
        }

        if ("OPEN_NOTIFICATIONS".equals(intent)) {
            callback.onComplaintAction("OPEN_NOTIFICATIONS");
            callback.onSpeak("Opening notifications.");
            return;
        }

        if ("OPEN_PROFILE".equals(intent)) {
            callback.onComplaintAction("OPEN_PROFILE");
            callback.onSpeak("Opening your profile.");
            return;
        }

        if ("GO_HOME".equals(intent)) {
            callback.onComplaintAction("GO_HOME");
            callback.onSpeak("Going to the home screen.");
            return;
        }

        if ("UNKNOWN".equals(intent)) {
            callback.onSpeak("I'm sorry, I didn't quite understand that. Could you clarify?");
            return;
        }

        if ("EDIT_FIELD".equals(intent) && response.editField != null) {
            String field = response.editField.toLowerCase();
            if (field.contains("category")) {
                currentState = ConversationState.COLLECTING_CATEGORY;
                callback.onStateChanged(currentState, draft);
                callback.onSpeak("Okay, please tell me the correct category.");
            } else if (field.contains("location")) {
                currentState = ConversationState.COLLECTING_LOCATION;
                callback.onStateChanged(currentState, draft);
                callback.onSpeak("Okay, please tell me the correct location.");
            } else if (field.contains("description")) {
                currentState = ConversationState.COLLECTING_DESCRIPTION;
                callback.onStateChanged(currentState, draft);
                callback.onSpeak("Okay, please tell me the correct description.");
            } else if (field.contains("title")) {
                currentState = ConversationState.COLLECTING_TITLE;
                callback.onStateChanged(currentState, draft);
                callback.onSpeak("Okay, please tell me the correct title.");
            }
            return;
        }

        switch (currentState) {
            case COLLECTING_CATEGORY:
                if (response.entities != null && response.entities.category != null) {
                    draft.category = response.entities.category;
                    currentState = ConversationState.CONFIRMING_CATEGORY;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("You said the issue is " + draft.category + ". Is that correct?");
                } else {
                    callback.onSpeak("I didn't understand the category. What type of civic issue is this?");
                }
                break;
            case CONFIRMING_CATEGORY:
                if (Boolean.TRUE.equals(response.confirmation)) {
                    if (!TextUtils.isEmpty(draft.locationAddress) && !TextUtils.isEmpty(draft.description) && !TextUtils.isEmpty(draft.title)) {
                        currentState = ConversationState.FINAL_REVIEW;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Category updated. Your complaint is ready for review. Would you like to submit it?");
                    } else {
                        currentState = ConversationState.COLLECTING_LOCATION;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Where is the " + draft.category + " located?");
                    }
                } else if (Boolean.FALSE.equals(response.confirmation)) {
                    currentState = ConversationState.COLLECTING_CATEGORY;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("Okay. Please tell me the correct category.");
                } else {
                    callback.onSpeak("Please say yes or no. Is the issue " + draft.category + "?");
                }
                break;
            case COLLECTING_LOCATION:
                if (response.entities != null && response.entities.location != null) {
                    draft.locationAddress = response.entities.location;
                    currentState = ConversationState.CONFIRMING_LOCATION;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("You said the location is " + draft.locationAddress + ". Is that correct?");
                } else {
                    callback.onSpeak("I didn't catch the location. Where is it?");
                }
                break;
            case CONFIRMING_LOCATION:
                if (Boolean.TRUE.equals(response.confirmation)) {
                    if (!TextUtils.isEmpty(draft.description) && !TextUtils.isEmpty(draft.title)) {
                        currentState = ConversationState.FINAL_REVIEW;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Location updated. Your complaint is ready for review. Would you like to submit it?");
                    } else {
                        currentState = ConversationState.COLLECTING_DESCRIPTION;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Please describe the issue in detail.");
                    }
                } else if (Boolean.FALSE.equals(response.confirmation)) {
                    currentState = ConversationState.COLLECTING_LOCATION;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("Okay. Please tell me the correct location.");
                } else {
                    callback.onSpeak("Please say yes or no. Is the location " + draft.locationAddress + "?");
                }
                break;
            case COLLECTING_DESCRIPTION:
                if (response.entities != null && response.entities.description != null) {
                    draft.description = response.entities.description;
                    currentState = ConversationState.CONFIRMING_DESCRIPTION;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("You described it as: " + draft.description + ". Is that correct?");
                } else {
                    callback.onSpeak("I didn't get the description. Please describe the issue.");
                }
                break;
            case CONFIRMING_DESCRIPTION:
                if (Boolean.TRUE.equals(response.confirmation)) {
                    if (!TextUtils.isEmpty(draft.title)) {
                        currentState = ConversationState.FINAL_REVIEW;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Description updated. Your complaint is ready for review. Would you like to submit it?");
                    } else {
                        currentState = ConversationState.COLLECTING_TITLE;
                        callback.onStateChanged(currentState, draft);
                        callback.onSpeak("Finally, please give this complaint a short title.");
                    }
                } else if (Boolean.FALSE.equals(response.confirmation)) {
                    currentState = ConversationState.COLLECTING_DESCRIPTION;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("Okay. Please tell me the correct description.");
                } else {
                    callback.onSpeak("Please say yes or no.");
                }
                break;
            case COLLECTING_TITLE:
                if (response.entities != null && response.entities.title != null) {
                    draft.title = response.entities.title;
                    currentState = ConversationState.CONFIRMING_TITLE;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("You titled it: " + draft.title + ". Is that correct?");
                } else {
                    callback.onSpeak("I missed that. What should the title be?");
                }
                break;
            case CONFIRMING_TITLE:
                if (Boolean.TRUE.equals(response.confirmation)) {
                    currentState = ConversationState.FINAL_REVIEW;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("Your complaint is ready for review. I have displayed all the details on the screen. Would you like to submit it?");
                } else if (Boolean.FALSE.equals(response.confirmation)) {
                    currentState = ConversationState.COLLECTING_TITLE;
                    callback.onStateChanged(currentState, draft);
                    callback.onSpeak("Okay. Please tell me the correct title.");
                } else {
                    callback.onSpeak("Please say yes or no.");
                }
                break;
            case FINAL_REVIEW:
                if ("SUBMIT_COMPLAINT".equals(intent) || Boolean.TRUE.equals(response.confirmation)) {
                    callback.onComplaintAction("SUBMIT_COMPLAINT");
                    currentState = ConversationState.SUBMITTED;
                    callback.onStateChanged(currentState, draft);
                } else if (Boolean.FALSE.equals(response.confirmation)) {
                    callback.onSpeak("Submission paused. You can edit any field or say cancel to abort.");
                } else {
                    callback.onSpeak("Please say submit to confirm or cancel to abort.");
                }
                break;
            case SUBMITTED:
                callback.onSpeak("This complaint has already been submitted. You can say track my complaints.");
                break;
            default:
                break;
        }
    }

    public CitizenComplaintStore.Draft getDraft() {
        return draft;
    }
}
