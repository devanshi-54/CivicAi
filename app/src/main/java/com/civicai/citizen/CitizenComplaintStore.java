package com.civicai.citizen;

import android.content.Context;
import android.content.SharedPreferences;

import com.civicai.model.Complaint;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Device-local storage for citizen drafts and submitted reports. */
public final class CitizenComplaintStore {
    private static final String PREFS = "citizen_complaints";
    private static final String COMPLAINTS_KEY = "complaints";
    private static final String DRAFT_KEY = "draft";
    private static final Gson GSON = new Gson();
    private static final Type COMPLAINT_LIST_TYPE = new TypeToken<ArrayList<Complaint>>() { }.getType();

    private CitizenComplaintStore() { }

    public static List<Complaint> getComplaints(Context context) {
        String json = preferences(context).getString(COMPLAINTS_KEY, "[]");
        try {
            List<Complaint> complaints = GSON.fromJson(json, COMPLAINT_LIST_TYPE);
            if (complaints == null) return new ArrayList<>();
            Collections.sort(complaints, (first, second) ->
                    Long.compare(second.getCreatedAt(), first.getCreatedAt()));
            return complaints;
        } catch (RuntimeException exception) {
            return new ArrayList<>();
        }
    }

    public static Complaint findComplaint(Context context, String complaintId) {
        if (complaintId == null) return null;
        for (Complaint complaint : getComplaints(context)) {
            if (complaintId.equals(complaint.getComplaintId())) return complaint;
        }
        return null;
    }

    public static void saveComplaint(Context context, Complaint complaint) {
        List<Complaint> complaints = getComplaints(context);
        for (int index = complaints.size() - 1; index >= 0; index--) {
            if (complaint.getComplaintId().equals(complaints.get(index).getComplaintId())) {
                complaints.remove(index);
            }
        }
        complaints.add(0, complaint);
        preferences(context).edit().putString(COMPLAINTS_KEY, GSON.toJson(complaints)).apply();
    }

    public static void saveDraft(Context context, Draft draft) {
        preferences(context).edit().putString(DRAFT_KEY, GSON.toJson(draft)).apply();
    }

    public static Draft getDraft(Context context) {
        String json = preferences(context).getString(DRAFT_KEY, null);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, Draft.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static void clearDraft(Context context) {
        preferences(context).edit().remove(DRAFT_KEY).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class Draft {
        public String title = "";
        public String description = "";
        public String category = "";
        public String locationAddress = "";
        public String imageUri = "";
        public Double latitude;
        public Double longitude;
    }
}
