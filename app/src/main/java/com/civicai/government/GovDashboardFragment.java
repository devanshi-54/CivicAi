package com.civicai.government;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.civicai.R;
import com.civicai.common.UiUtils;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Government Dashboard Fragment.
 * Displays aggregate complaint statistics and a preview of recent complaints.
 */
public class GovDashboardFragment extends Fragment {

    private GovViewModel viewModel;

    private TextView statTotalCount;
    private TextView statHighCount;
    private TextView statPendingCount;
    private TextView statResolvedCount;
    private LinearLayout recentComplaintsContainer;
    private TextView emptyState;
    private MaterialCardView cardOpenPriorityQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gov_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statTotalCount = view.findViewById(R.id.stat_total_count);
        statHighCount = view.findViewById(R.id.stat_high_count);
        statPendingCount = view.findViewById(R.id.stat_pending_count);
        statResolvedCount = view.findViewById(R.id.stat_resolved_count);
        recentComplaintsContainer = view.findViewById(R.id.recent_complaints_container);
        emptyState = view.findViewById(R.id.dashboard_empty_state);
        cardOpenPriorityQueue = view.findViewById(R.id.card_open_priority_queue);

        viewModel = new ViewModelProvider(requireActivity()).get(GovViewModel.class);

        // Greeting with time of day
        TextView greeting = view.findViewById(R.id.dashboard_greeting);
        greeting.setText(getGreeting());

        cardOpenPriorityQueue.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_priorityQueue));

        observeViewModel();
        viewModel.loadAllComplaints();
    }

    private void observeViewModel() {
        viewModel.getComplaints().observe(getViewLifecycleOwner(), this::updateDashboard);
    }

    private void updateDashboard(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            statTotalCount.setText("0");
            statHighCount.setText("0");
            statPendingCount.setText("0");
            statResolvedCount.setText("0");
            emptyState.setVisibility(View.VISIBLE);
            recentComplaintsContainer.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        recentComplaintsContainer.setVisibility(View.VISIBLE);

        int total = complaints.size();
        int high = 0;
        int pending = 0;
        int resolved = 0;

        for (Complaint c : complaints) {
            Priority p = c.getEffectivePriority();
            if (p == Priority.HIGH) high++;

            ComplaintStatus s = c.getStatus();
            if (s == ComplaintStatus.SUBMITTED || s == ComplaintStatus.UNDER_REVIEW) pending++;
            if (s == ComplaintStatus.RESOLVED) resolved++;
        }

        statTotalCount.setText(String.valueOf(total));
        statHighCount.setText(String.valueOf(high));
        statPendingCount.setText(String.valueOf(pending));
        statResolvedCount.setText(String.valueOf(resolved));

        // Show up to 5 recent complaints as summary cards
        recentComplaintsContainer.removeAllViews();
        int limit = Math.min(complaints.size(), 5);
        for (int i = 0; i < limit; i++) {
            View itemView = buildRecentItem(complaints.get(i));
            recentComplaintsContainer.addView(itemView);
        }
    }

    private View buildRecentItem(Complaint complaint) {
        // Inflate a simple card programmatically
        MaterialCardView card = new MaterialCardView(requireContext());
        int dp12 = dp(12);
        int dp8 = dp(8);
        int dp10 = dp(10);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp8);
        card.setLayoutParams(cardParams);
        card.setCardElevation(2f);
        card.setRadius(dp(10));
        card.setStrokeWidth(1);
        card.setStrokeColor(requireContext().getColor(R.color.border_color));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp12, dp10, dp12, dp10);

        // Title row
        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView priorityBadge = new TextView(requireContext());
        Priority p = complaint.getEffectivePriority();
        priorityBadge.setText(p != null ? p.name() : "?");
        priorityBadge.setTextSize(10f);
        priorityBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        priorityBadge.setPadding(dp8, dp(4), dp8, dp(4));
        if (p == Priority.HIGH) {
            priorityBadge.setTextColor(requireContext().getColor(R.color.high_priority));
            priorityBadge.setBackgroundResource(R.drawable.bg_badge_high_priority);
        } else if (p == Priority.LOW) {
            priorityBadge.setTextColor(requireContext().getColor(R.color.low_priority));
            priorityBadge.setBackgroundResource(R.drawable.bg_badge_low_priority);
        } else {
            priorityBadge.setTextColor(requireContext().getColor(R.color.medium_priority));
            priorityBadge.setBackgroundResource(R.drawable.bg_badge_medium_priority);
        }

        TextView title = new TextView(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp8);
        title.setLayoutParams(titleParams);
        String titleText = complaint.getTitle() != null ? complaint.getTitle() : "Untitled";
        title.setText(titleText);
        title.setTextColor(requireContext().getColor(R.color.primary_text));
        title.setTextSize(13f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        titleRow.addView(priorityBadge);
        titleRow.addView(title);
        inner.addView(titleRow);

        // Category + status
        LinearLayout metaRow = new LinearLayout(requireContext());
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        metaParams.setMargins(0, dp(4), 0, 0);
        metaRow.setLayoutParams(metaParams);

        TextView category = new TextView(requireContext());
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        category.setLayoutParams(catParams);
        category.setText(complaint.getCategory() != null ? complaint.getCategory() : "—");
        category.setTextColor(requireContext().getColor(R.color.secondary_text));
        category.setTextSize(12f);

        TextView statusText = new TextView(requireContext());
        ComplaintStatus status = complaint.getStatus();
        statusText.setText(status != null ? status.getDisplayName() : "—");
        statusText.setTextColor(UiUtils.getStatusColor(requireContext(), status));
        statusText.setTextSize(11f);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);

        metaRow.addView(category);
        metaRow.addView(statusText);
        inner.addView(metaRow);

        card.addView(inner);

        // Navigate to priority queue on tap (user can then open details)
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_priorityQueue));

        return card;
    }

    private String getGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning, Officer";
        if (hour < 17) return "Good afternoon, Officer";
        return "Good evening, Officer";
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
