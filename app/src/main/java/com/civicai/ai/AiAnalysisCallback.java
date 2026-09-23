package com.civicai.ai;

import com.civicai.model.AiAnalysisResult;

/**
 * Callback for AI analysis operations.
 * Explicitly separates success, failure, and timeout states.
 */
public interface AiAnalysisCallback {
    void onAnalysisComplete(AiAnalysisResult result);
    void onAnalysisError(String errorMessage, boolean isRetryable);
}
