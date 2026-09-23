package com.civicai.ai;

import com.civicai.model.Complaint;

/**
 * Service interface for AI-driven complaint triage, severity estimation, and priority recommendation.
 */
public interface IAiAnalysisService {

    /**
     * Analyze a complaint and generate structured decision-support triage data.
     *
     * @param complaint The complaint entity containing title, description, category, and location.
     * @param callback Callback returning structured AiAnalysisResult or actionable error.
     */
    void analyzeComplaint(Complaint complaint, AiAnalysisCallback callback);

    /**
     * Cancel an ongoing analysis request if in-flight.
     */
    void cancelAnalysis();
}
