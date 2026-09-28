package com.civicai.government;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.civicai.R;
import com.civicai.model.Complaint;
import com.civicai.model.Priority;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Government Priority Queue Fragment.
 * Displays all complaints sorted by priority with filter toggle.
 */
public class GovPriorityQueueFragment extends Fragment {

    private GovViewModel viewModel;
    private GovComplaintAdapter adapter;

    private RecyclerView recyclerView;
    private TextView countLabel;
    private View emptyState;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterHigh;

    private boolean showHighOnly = false;
    private List<Complaint> allComplaints = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gov_priority_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.pq_recycler_view);
        countLabel = view.findViewById(R.id.pq_count_label);
        emptyState = view.findViewById(R.id.pq_empty_state);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterHigh = view.findViewById(R.id.btn_filter_high);

        // Navigation header
        View btnBack = view.findViewById(R.id.btn_pq_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }
        View btnHome = view.findViewById(R.id.btn_pq_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> Navigation.findNavController(v).popBackStack(
                    R.id.govDashboardFragment,
                    false
            ));
        }

        // Setup RecyclerView
        adapter = new GovComplaintAdapter(complaint -> navigateToDetails(view, complaint));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(GovViewModel.class);

        // Filter buttons
        btnFilterAll.setOnClickListener(v -> {
            showHighOnly = false;
            applyFilter();
        });
        btnFilterHigh.setOnClickListener(v -> {
            showHighOnly = true;
            applyFilter();
        });

        observeViewModel();

        // Load data (may already be loaded from Dashboard)
        if (viewModel.getComplaints().getValue() == null) {
            viewModel.loadAllComplaints();
        }
    }

    private void observeViewModel() {
        viewModel.getComplaints().observe(getViewLifecycleOwner(), complaints -> {
            allComplaints = complaints != null ? complaints : new ArrayList<>();
            applyFilter();
        });
    }

    private void applyFilter() {
        List<Complaint> filtered = new ArrayList<>();
        for (Complaint c : allComplaints) {
            if (!showHighOnly || c.getEffectivePriority() == Priority.HIGH) {
                filtered.add(c);
            }
        }

        // Sort: HIGH first, then MEDIUM, then LOW; within same priority by date
        filtered.sort((a, b) -> {
            int pa = priorityRank(a.getEffectivePriority());
            int pb = priorityRank(b.getEffectivePriority());
            if (pa != pb) return Integer.compare(pa, pb);
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        });

        adapter.setComplaints(filtered);
        updateEmptyState(filtered.isEmpty());
        updateCountLabel(filtered.size());
    }

    private int priorityRank(Priority p) {
        if (p == null) return 2;
        switch (p) {
            case HIGH: return 0;
            case MEDIUM: return 1;
            case LOW: return 2;
            default: return 2;
        }
    }

    private void updateEmptyState(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateCountLabel(int count) {
        String filter = showHighOnly ? " (High Priority)" : "";
        countLabel.setText(count + " complaint" + (count != 1 ? "s" : "") + filter);
    }

    private void navigateToDetails(View rootView, Complaint complaint) {
        if (complaint.getComplaintId() == null || complaint.getComplaintId().isEmpty()) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("complaintId", complaint.getComplaintId());
        Navigation.findNavController(rootView)
                .navigate(R.id.action_priorityQueue_to_complaintDetails, args);
    }
}
