package com.civicai.citizen.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceManager implements TextToSpeech.OnInitListener, RecognitionListener {

    public interface VoiceCallback {
        void onSpeechRecognized(String text);
        void onSpeechError(String error);
        void onSpeechStatus(String status);
    }

    private final Context context;
    private final VoiceCallback callback;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean ttsReady = false;

    public VoiceManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        
        tts = new TextToSpeech(context, this);
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(this);
        } else {
            callback.onSpeechError("Speech recognition not available on this device.");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true;
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}
                    
                    @Override
                    public void onDone(String utteranceId) {
                        callback.onSpeechStatus("Tap to speak");
                    }
                    
                    @Override
                    public void onError(String utteranceId) {}
                });
            } else {
                Log.e("VoiceManager", "TTS Language not supported");
            }
        } else {
            Log.e("VoiceManager", "TTS Init failed");
        }
    }

    public void speak(String text) {
        if (ttsReady && tts != null) {
            // Stop listening when speaking to avoid feedback loop
            stopListening();
            callback.onSpeechStatus("Speaking...");
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_msg");
        }
    }

    public void startListening() {
        if (speechRecognizer != null) {
            if (ttsReady && tts.isSpeaking()) {
                tts.stop();
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizer.startListening(intent);
            isListening = true;
            callback.onSpeechStatus("Listening...");
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public boolean isCurrentlyListening() {
        return isListening;
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    // RecognitionListener Methods
    @Override
    public void onReadyForSpeech(Bundle params) {
        callback.onSpeechStatus("Listening...");
    }

    @Override
    public void onBeginningOfSpeech() {
        callback.onSpeechStatus("Hearing you...");
    }

    @Override
    public void onRmsChanged(float rmsdB) {}

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {
        callback.onSpeechStatus("Processing...");
        isListening = false;
    }

    @Override
    public void onError(int error) {
        isListening = false;
        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error"; break;
            case SpeechRecognizer.ERROR_CLIENT: message = "Client side error"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Insufficient permissions"; break;
            case SpeechRecognizer.ERROR_NETWORK: message = "Network error"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Network timeout"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: message = "No match"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "RecognitionService busy"; break;
            case SpeechRecognizer.ERROR_SERVER: message = "Error from server"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "No speech input"; break;
            default: message = "Didn't understand, please try again."; break;
        }
        callback.onSpeechError(message);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            callback.onSpeechRecognized(matches.get(0));
        } else {
            callback.onSpeechError("I couldn't hear that clearly. Please try again.");
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}
}
