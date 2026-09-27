package com.civicai.government;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.civicai.R;
import com.civicai.common.UiUtils;
import com.civicai.model.Complaint;
import com.civicai.model.Priority;
import com.google.android.material.button.MaterialButton;

/**
 * Complaint Details Fragment for Government officials.
 * Clearly distinguishes AI Recommendation (advisory) from Official Government Decision (authoritative).
 */
public class GovComplaintDetailsFragment extends Fragment {

    private GovViewModel viewModel;
    private String complaintId;

    // Loading / Error / Content
    private ProgressBar loadingView;
    private TextView errorView;
    private LinearLayout contentView;

    // Complaint info
    private TextView detailComplaintId;
    private TextView detailPriorityBadge;
    private TextView detailStatusBadge;
    private TextView detailTitle;
    private TextView detailDescription;
    private TextView detailCitizenName;
    private TextView detailCategory;
    private TextView detailLocation;

    // AI Recommendation
    private TextView detailAiSummary;
    private TextView detailAiCategory;
    private TextView detailAiPriority;
    private TextView detailAiSeverity;
    private TextView detailAiUrgency;
    private TextView detailAiSafetyRisk;
    private TextView detailAiAffected;
    private TextView detailAiDept;
    private TextView detailAiAction;
    private TextView detailAiReason;
    private ProgressBar detailAiConfidenceBar;
    private TextView detailAiConfidenceText;

    // Official Decision
    private TextView detailNoDecision;
    private LinearLayout detailDecisionContent;
    private TextView detailOfficialPriority;
    private TextView detailAssignedDept;
    private TextView detailAssignedOfficer;
    private TextView detailOfficialDecisionText;
    private TextView detailInternalNotes;

    // Action button
    private MaterialButton btnOpenDecision;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gov_complaint_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve argument
        Bundle args = getArguments();
        complaintId = (args != null) ? args.getString("complaintId", "") : "";

        bindViews(view);

        // Navigation header
        View btnBack = view.findViewById(R.id.btn_details_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }
        View btnHome = view.findViewById(R.id.btn_details_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> Navigation.findNavController(v).popBackStack(
                    R.id.govDashboardFragment,
                    false
            ));
        }

        viewModel = new ViewModelProvider(requireActivity()).get(GovViewModel.class);

        observeViewModel(view);

        if (complaintId == null || complaintId.isEmpty()) {
            showError("No complaint ID provided.");
        } else {
            showLoading();
            viewModel.loadComplaintById(complaintId);
        }
    }

    private void bindViews(View view) {
        loadingView = view.findViewById(R.id.details_loading);
        errorView = view.findViewById(R.id.details_error);
        contentView = view.findViewById(R.id.details_content);

        detailComplaintId = view.findViewById(R.id.detail_complaint_id);
        detailPriorityBadge = view.findViewById(R.id.detail_priority_badge);
        detailStatusBadge = view.findViewById(R.id.detail_status_badge);
        detailTitle = view.findViewById(R.id.detail_title);
        detailDescription = view.findViewById(R.id.detail_description);
        detailCitizenName = view.findViewById(R.id.detail_citizen_name);
        detailCategory = view.findViewById(R.id.detail_category);
        detailLocation = view.findViewById(R.id.detail_location);

        detailAiSummary = view.findViewById(R.id.detail_ai_summary);
        detailAiCategory = view.findViewById(R.id.detail_ai_category);
        detailAiPriority = view.findViewById(R.id.detail_ai_priority);
        detailAiSeverity = view.findViewById(R.id.detail_ai_severity);
        detailAiUrgency = view.findViewById(R.id.detail_ai_urgency);
        detailAiSafetyRisk = view.findViewById(R.id.detail_ai_safety_risk);
        detailAiAffected = view.findViewById(R.id.detail_ai_affected);
        detailAiDept = view.findViewById(R.id.detail_ai_dept);
        detailAiAction = view.findViewById(R.id.detail_ai_action);
        detailAiReason = view.findViewById(R.id.detail_ai_reason);
        detailAiConfidenceBar = view.findViewById(R.id.detail_ai_confidence_bar);
        detailAiConfidenceText = view.findViewById(R.id.detail_ai_confidence_text);

        detailNoDecision = view.findViewById(R.id.detail_no_decision);
        detailDecisionContent = view.findViewById(R.id.detail_decision_content);
        detailOfficialPriority = view.findViewById(R.id.detail_official_priority);
        detailAssignedDept = view.findViewById(R.id.detail_assigned_dept);
        detailAssignedOfficer = view.findViewById(R.id.detail_assigned_officer);
        detailOfficialDecisionText = view.findViewById(R.id.detail_official_decision_text);
        detailInternalNotes = view.findViewById(R.id.detail_internal_notes);

        btnOpenDecision = view.findViewById(R.id.btn_open_decision);
    }

    private void observeViewModel(View rootView) {
        viewModel.getSelectedComplaint().observe(getViewLifecycleOwner(), complaint -> {
            if (complaint != null) {
                populateViews(complaint);
                showContent();
            }
        });

        viewModel.getDetailError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
    }

    private void populateViews(Complaint c) {
        // Complaint ID
        detailComplaintId.setText(c.getComplaintId() != null ? "#" + c.getComplaintId() : "—");

        // Priority badge
        Priority p = c.getEffectivePriority();
        String priorityLabel = p != null ? p.name() + " PRIORITY" : "UNKNOWN";
        detailPriorityBadge.setText(priorityLabel);
        if (requireContext() != null) {
            if (p == Priority.HIGH) {
                detailPriorityBadge.setTextColor(requireContext().getColor(R.color.high_priority));
                detailPriorityBadge.setBackgroundResource(R.drawable.bg_badge_high_priority);
            } else if (p == Priority.LOW) {
                detailPriorityBadge.setTextColor(requireContext().getColor(R.color.low_priority));
                detailPriorityBadge.setBackgroundResource(R.drawable.bg_badge_low_priority);
            } else {
                detailPriorityBadge.setTextColor(requireContext().getColor(R.color.medium_priority));
                detailPriorityBadge.setBackgroundResource(R.drawable.bg_badge_medium_priority);
            }
        }

        // Status badge
        if (c.getStatus() != null) {
            detailStatusBadge.setText(c.getStatus().getDisplayName());
            detailStatusBadge.setTextColor(UiUtils.getStatusColor(requireContext(), c.getStatus()));
        }

        detailTitle.setText(nvl(c.getTitle()));
        detailDescription.setText(nvl(c.getDescription()));
        detailCitizenName.setText(nvl(c.getCitizenName()));
        detailCategory.setText(nvl(c.getCategory()));
        detailLocation.setText(nvl(c.getLocationAddress()));

        // AI fields
        detailAiSummary.setText(nvl(c.getAiSummary()));
        detailAiCategory.setText(nvl(c.getAiCategory()));
        detailAiPriority.setText(c.getAiPriority() != null ? c.getAiPriority().name() : "—");
        detailAiSeverity.setText(nvl(c.getAiSeverity()));
        detailAiUrgency.setText(nvl(c.getAiUrgency()));
        detailAiSafetyRisk.setText(nvl(c.getAiSafetyRisk()));
        detailAiAffected.setText(c.getAiAffectedPeople() > 0
                ? String.valueOf(c.getAiAffectedPeople()) + " people" : "—");
        detailAiDept.setText(nvl(c.getSuggestedDepartment()));
        detailAiAction.setText(nvl(c.getSuggestedAction()));
        detailAiReason.setText(nvl(c.getAiReason()));

        int confidencePct = (int) (c.getAiConfidence() * 100);
        detailAiConfidenceBar.setProgress(confidencePct);
        detailAiConfidenceText.setText(confidencePct + "%");

        // Official decision section
        boolean hasDecision = c.getOfficialDecision() != null && !c.getOfficialDecision().isEmpty();
        detailNoDecision.setVisibility(hasDecision ? View.GONE : View.VISIBLE);
        detailDecisionContent.setVisibility(hasDecision ? View.VISIBLE : View.GONE);

        if (hasDecision) {
            detailOfficialPriority.setText(c.getOfficialPriority() != null
                    ? c.getOfficialPriority().name() : "—");
            detailAssignedDept.setText(nvl(c.getAssignedDepartment()));
            detailAssignedOfficer.setText(nvl(c.getAssignedOfficer()));
            detailOfficialDecisionText.setText(nvl(c.getOfficialDecision()));
            detailInternalNotes.setText(nvl(c.getInternalNotes()));
        }

        // Navigate to decision screen
        btnOpenDecision.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("complaintId", c.getComplaintId());
            Navigation.findNavController(v)
                    .navigate(R.id.action_complaintDetails_to_decision, args);
        });
    }

    private String nvl(String value) {
        return (value != null && !value.isEmpty()) ? value : "—";
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message != null ? message : "An error occurred.");
        contentView.setVisibility(View.GONE);
    }
}
