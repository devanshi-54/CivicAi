package com.civicai.common;

import android.content.Context;
import com.civicai.repository.RepositoryCallback;

/**
 * Clean integration interface for the Voice Assistant / Voice Complaint module
 * managed by team member Maheswaran.
 */
public interface VoiceAssistantIntegrationPoint {

    /**
     * Start voice capture for speech-to-text complaint entry.
     */
    void startVoiceInput(Context context, VoiceCallback callback);

    /**
     * Stop active voice recording.
     */
    void stopVoiceInput();

    /**
     * Callback interface for voice transcription results.
     */
    interface VoiceCallback {
        void onSpeechRecognized(String transcribedText);
        void onVoiceError(String errorMessage);
    }
}
