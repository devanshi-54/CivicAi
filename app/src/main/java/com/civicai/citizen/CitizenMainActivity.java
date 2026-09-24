package com.civicai.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.civicai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/** Citizen-facing UI and navigation for the screens in the CivicAI citizen suite. */
public class CitizenMainActivity extends AppCompatActivity {
    private final ArrayDeque<Integer> screenHistory = new ArrayDeque<>();
    private int currentScreen = R.layout.activity_citizen_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBack(); }
        });
        showScreen(currentScreen, false);
    }

    private void showScreen(int layout, boolean rememberCurrent) {
        if (rememberCurrent) screenHistory.push(currentScreen);
        currentScreen = layout;
        setContentView(layout);
        wireToolbar();
        wireBottomNavigation();
        wireActions();
        populateLists();
    }

    private void wireToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> goBack());
    }

    private void wireBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav == null) return;
        int selected = currentScreen == R.layout.activity_citizen_my_complaints ? R.id.citizen_my_complaints
                : currentScreen == R.layout.activity_citizen_notifications ? R.id.citizen_notifications
                : currentScreen == R.layout.activity_citizen_profile_settings ? R.id.citizen_profile_settings
                : R.id.citizen_home;
        nav.setSelectedItemId(selected);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.citizen_home) showScreen(R.layout.activity_citizen_home, true);
            else if (id == R.id.citizen_my_complaints) showScreen(R.layout.activity_citizen_my_complaints, true);
            else if (id == R.id.citizen_notifications) showScreen(R.layout.activity_citizen_notifications, true);
            else if (id == R.id.citizen_profile_settings) showScreen(R.layout.activity_citizen_profile_settings, true);
            return true;
        });
    }

    private void wireActions() {
        click(R.id.btnReportHero, () -> open(R.layout.activity_citizen_report_complaint));
        click(R.id.btnNavReport, () -> open(R.layout.activity_citizen_report_complaint));
        click(R.id.btnNavMyComplaints, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnNavTrack, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnNotification, () -> open(R.layout.activity_citizen_notifications));
        click(R.id.btnViewAll, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnAnalyze, () -> open(R.layout.activity_citizen_ai_analysis));
        click(R.id.btnSubmitFinal, () -> open(R.layout.activity_citizen_complaint_submitted));
        click(R.id.btnTrackComplaint, () -> open(R.layout.activity_citizen_complaint_details));
        click(R.id.btnBackHome, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnViewComplaint, () -> open(R.layout.activity_citizen_complaint_details));
        click(R.id.btnSkip, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnNext, () -> {
            if (currentScreen == R.layout.activity_citizen_onboarding_1) open(R.layout.activity_citizen_onboarding_2);
            else if (currentScreen == R.layout.activity_citizen_onboarding_2) open(R.layout.activity_citizen_onboarding_3);
            else showScreen(R.layout.activity_citizen_home, false);
        });
        click(R.id.btnGetStarted, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnLogout, () -> Toast.makeText(this, "You are signed out", Toast.LENGTH_SHORT).show());
        click(R.id.btnSaveDraft, () -> Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show());
        click(R.id.btnCamera, () -> Toast.makeText(this, "Camera upload is ready to connect", Toast.LENGTH_SHORT).show());
        click(R.id.btnGallery, () -> Toast.makeText(this, "Gallery upload is ready to connect", Toast.LENGTH_SHORT).show());
    }

    private void populateLists() {
        RecyclerView recent = findViewById(R.id.rvRecentComplaints);
        if (recent != null) setRows(recent, false);
        RecyclerView complaints = findViewById(R.id.rvMyComplaints);
        if (complaints != null) setRows(complaints, false);
        RecyclerView notifications = findViewById(R.id.rvNotifications);
        if (notifications != null) setRows(notifications, true);
    }

    private void setRows(RecyclerView list, boolean notifications) {
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new CitizenListAdapter(notifications,
                notifications ? Arrays.asList(
                        new Row("Complaint status updated", "Your pothole report is under review.", "2h ago", ""),
                        new Row("Report received", "We received your streetlight complaint.", "Yesterday", ""),
                        new Row("Civic alert for Ward 4", "Road maintenance is scheduled this week.", "Sep 20", ""))
                        : Arrays.asList(
                        new Row("CIV-2026-00124", "Large pothole near school road", "2h ago", "High"),
                        new Row("CIV-2026-00119", "Streetlight not working", "Yesterday", "Medium"),
                        new Row("CIV-2026-00108", "Water leakage near Main Road", "Sep 18", "Low"))));
    }

    private void click(int id, Runnable action) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(v -> action.run());
    }

    private void open(int layout) { showScreen(layout, true); }

    private void goBack() {
        if (screenHistory.isEmpty()) {
            if (currentScreen != R.layout.activity_citizen_home) showScreen(R.layout.activity_citizen_home, false);
            else finish();
        } else {
            showScreen(screenHistory.pop(), false);
        }
    }

    private static final class Row {
        final String title, detail, time, priority;
        Row(String title, String detail, String time, String priority) {
            this.title = title; this.detail = detail; this.time = time; this.priority = priority;
        }
    }

    private final class CitizenListAdapter extends RecyclerView.Adapter<CitizenListAdapter.RowHolder> {
        private final boolean notifications;
        private final List<Row> rows;
        CitizenListAdapter(boolean notifications, List<Row> rows) { this.notifications = notifications; this.rows = rows; }
        @NonNull @Override public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = notifications ? R.layout.item_citizen_notification_card : R.layout.item_citizen_complaint_card;
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new RowHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            Row row = rows.get(position);
            if (notifications) {
                text(holder.itemView, R.id.tvTitle, row.title);
                text(holder.itemView, R.id.tvMessage, row.detail);
                text(holder.itemView, R.id.tvTimestamp, row.time);
            } else {
                text(holder.itemView, R.id.tvTicketId, row.title);
                text(holder.itemView, R.id.tvTitle, row.detail);
                text(holder.itemView, R.id.tvTimestamp, "• " + row.time);
                text(holder.itemView, R.id.tvPriorityBadge, "● " + row.priority);
                text(holder.itemView, R.id.tvLocation, "Main Road, Surendranagar");
            }
            holder.itemView.setOnClickListener(v -> open(notifications
                    ? R.layout.activity_citizen_notification_details
                    : R.layout.activity_citizen_complaint_details));
        }
        @Override public int getItemCount() { return rows.size(); }
        final class RowHolder extends RecyclerView.ViewHolder { RowHolder(@NonNull View itemView) { super(itemView); } }
    }

    private static void text(View root, int id, String value) {
        TextView view = root.findViewById(id);
        if (view != null) view.setText(value);
    }
}
