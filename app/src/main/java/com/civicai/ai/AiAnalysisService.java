package com.civicai.ai;

import android.os.Handler;
import android.os.Looper;

import com.civicai.model.AiAnalysisResult;
import com.civicai.model.Complaint;
import com.civicai.model.Priority;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.Executor;

/** Cloud complaint triage using Firebase AI Logic and Gemini Developer API. */
public class AiAnalysisService implements IAiAnalysisService {
    private static final String MODEL_NAME = "gemini-3.5-flash-lite";
    private static AiAnalysisService instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor mainExecutor = command -> mainHandler.post(command);
    private volatile int activeGeneration;
    private volatile ListenableFuture<?> activeRequest;

    private AiAnalysisService() { }

    public static synchronized AiAnalysisService getInstance() {
        if (instance == null) instance = new AiAnalysisService();
        return instance;
    }

    @Override public void analyzeComplaint(Complaint complaint, AiAnalysisCallback callback) {
        if (callback == null) return;
        if (complaint == null || (isBlank(complaint.getTitle()) && isBlank(complaint.getDescription()))) {
            callback.onAnalysisError("Complaint title or description is required.", false);
            return;
        }

        cancelAnalysis();
        int generation = ++activeGeneration;
        try {
            GenerativeModel model = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                    .generativeModel(MODEL_NAME);
            GenerativeModelFutures futuresModel = GenerativeModelFutures.from(model);
            String promptText = buildPrompt(complaint);
            Content prompt = new Content.Builder().addText(promptText).build();
            ListenableFuture<GenerateContentResponse> request = futuresModel.generateContent(prompt);
            activeRequest = request;
            Futures.addCallback(request, new FutureCallback<GenerateContentResponse>() {
                @Override public void onSuccess(GenerateContentResponse response) {
                    if (generation != activeGeneration) return;
                    try {
                        callback.onAnalysisComplete(parseResult(response.getText()));
                    } catch (Exception error) {
                        callback.onAnalysisError("AI returned an unreadable triage result. Please retry.", true);
                    }
                }

                @Override public void onFailure(Throwable error) {
                    if (generation == activeGeneration) callback.onAnalysisError(
                            "Gemini analysis failed. Check Firebase AI Logic setup or try again.", true);
                }
            }, mainExecutor);
        } catch (Exception error) {
            callback.onAnalysisError("Firebase AI Logic is not configured for this Firebase project.", false);
        }
    }

    private String buildPrompt(Complaint complaint) {
        String title = redactContactInfo(complaint.getTitle());
        String description = redactContactInfo(complaint.getDescription());
        String category = redactContactInfo(complaint.getCategory());
        return "You are a civic complaint triage assistant. Analyze the report and return only a JSON object "
                + "with keys summary, category, severity, urgency, safetyRisk, affectedPeople, priority, reason, "
                + "suggestedDepartment, suggestedAction, confidence. priority must be HIGH, MEDIUM, or LOW. "
                + "confidence must be a number from 0 to 1. Estimate affectedPeople conservatively; use 0 if unknown. "
                + "Recommend one responsible municipal department and an initial inspection/action. "
                + "This is advisory triage only; never claim an official assignment or decision. "
                + "Do not infer facts not stated in the report.\n"
                + "Title: " + safe(title) + "\nCategory: " + safe(category) + "\nDescription: " + safe(description);
    }

    private AiAnalysisResult parseResult(String responseText) throws Exception {
        if (isBlank(responseText)) throw new IllegalArgumentException("Empty model response");
        int start = responseText.indexOf('{');
        int end = responseText.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("Missing JSON object");
        JSONObject json = new JSONObject(responseText.substring(start, end + 1));
        AiAnalysisResult result = new AiAnalysisResult();
        result.setSummary(json.optString("summary", "Civic complaint triage"));
        result.setCategory(json.optString("category", "General Civic"));
        result.setSeverity(json.optString("severity", "Needs review"));
        result.setUrgency(json.optString("urgency", "Needs review"));
        result.setSafetyRisk(json.optString("safetyRisk", "Needs review"));
        result.setAffectedPeople(Math.max(0, json.optInt("affectedPeople", 0)));
        try {
            result.setPriority(Priority.valueOf(json.optString("priority", "MEDIUM").trim().toUpperCase(Locale.US)));
        } catch (IllegalArgumentException ignored) {
            result.setPriority(Priority.MEDIUM);
        }
        result.setReason(json.optString("reason", "Municipal review recommended."));
        result.setSuggestedDepartment(json.optString("suggestedDepartment", "Municipal Intake"));
        result.setSuggestedAction(json.optString("suggestedAction", "Review and inspect the report."));
        result.setConfidence(Math.max(0f, Math.min(1f, (float) json.optDouble("confidence", 0.5))));
        return result;
    }

    private String redactContactInfo(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[redacted email]")
                .replaceAll("(?<!\\d)(?:\\+?\\d[\\d ()-]{8,}\\d)(?!\\d)", "[redacted phone]");
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    @Override public void cancelAnalysis() {
        activeGeneration++;
        ListenableFuture<?> request = activeRequest;
        if (request != null) request.cancel(true);
        activeRequest = null;
    }
}
