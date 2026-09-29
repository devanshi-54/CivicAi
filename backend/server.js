const express = require('express');
const cors = require('cors');
require('dotenv').config();
const { GoogleGenAI, Type } = require('@google/genai');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-2.5-flash';

if (!GEMINI_API_KEY) {
    console.error("CRITICAL: GEMINI_API_KEY is missing from environment variables.");
    process.exit(1);
}

const ai = new GoogleGenAI({ apiKey: GEMINI_API_KEY });

const schema = {
    type: Type.OBJECT,
    properties: {
        intent: {
            type: Type.STRING,
            description: "One of: CREATE_COMPLAINT, CONTINUE_COMPLAINT, CONFIRM_FIELD, REJECT_FIELD, EDIT_FIELD, CANCEL_COMPLAINT, RESTART_COMPLAINT, FINAL_CONFIRMATION, SUBMIT_COMPLAINT, TRACK_COMPLAINT, LIST_COMPLAINTS, CHECK_STATUS, OPEN_GRIEVANCES, OPEN_NOTIFICATIONS, OPEN_PROFILE, GO_HOME, UNKNOWN",
        },
        confidence: { type: Type.NUMBER },
        entities: {
            type: Type.OBJECT,
            properties: {
                category: { type: Type.STRING, nullable: true },
                title: { type: Type.STRING, nullable: true },
                description: { type: Type.STRING, nullable: true },
                location: { type: Type.STRING, nullable: true }
            },
            nullable: true
        },
        confirmation: { type: Type.BOOLEAN, nullable: true, description: "True if the user confirmed a field, false if they rejected it." },
        editField: { type: Type.STRING, nullable: true, description: "The field the user wants to edit, e.g. 'location', 'category'." },
        cancel: { type: Type.BOOLEAN },
        restart: { type: Type.BOOLEAN }
    },
    required: ["intent", "confidence", "cancel", "restart"]
};

app.post('/api/assistant/message', async (req, res) => {
    try {
        const { sessionId, message, conversationState, complaintDraft } = req.body;

        if (!message) {
            return res.status(400).json({ error: "Missing message" });
        }

        const systemInstruction = `You are the Natural Language Understanding (NLU) layer for the CivicAI Android app.
You parse user input to extract structured data for a civic complaint system.
Current State: ${conversationState}
Current Draft: ${JSON.stringify(complaintDraft)}

Rules:
1. Extract the intent based on the user's message.
2. If the user is confirming a field we asked about, intent is CONFIRM_FIELD and confirmation=true.
3. If the user rejects a field we asked about, intent is REJECT_FIELD and confirmation=false.
4. If the user provides info for a complaint, intent is CONTINUE_COMPLAINT or CREATE_COMPLAINT, and populate 'entities' with the info.
5. If the user says "change the location" or similar, intent is EDIT_FIELD and editField="location".
6. If the user wants to track complaints, intent is TRACK_COMPLAINT.
7. Return only the requested JSON structure.`;

        let response;
        let retries = 5;
        while (retries > 0) {
            try {
                response = await ai.models.generateContent({
                    model: GEMINI_MODEL,
                    contents: message,
                    config: {
                        systemInstruction: systemInstruction,
                        responseMimeType: "application/json",
                        responseSchema: schema,
                        temperature: 0.1
                    }
                });
                break;
            } catch (error) {
                if (error.status === 503 && retries > 1) {
                    console.log("Gemini API 503 Unavailable. Retrying...");
                    retries--;
                    await new Promise(resolve => setTimeout(resolve, 3000));
                } else {
                    throw error;
                }
            }
        }

        if (response.text) {
            const parsed = JSON.parse(response.text);
            res.json(parsed);
        } else {
            res.status(500).json({ error: "Empty response from Gemini" });
        }

    } catch (error) {
        console.error("Backend Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.listen(PORT, () => {
    console.log(`CivicAI NLU Backend listening on port ${PORT}`);
    console.log(`Using model: ${GEMINI_MODEL}`);
});
