package com.civicai.government;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.civicai.R;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Official Decision Fragment for Government officials.
 * Allows entering/updating priority, department, officer, decision text, notes, and status.
 */
public class GovDecisionFragment extends Fragment {

    private GovViewModel viewModel;
    private String complaintId;

    private Spinner spinnerPriority;
    private Spinner spinnerStatus;
    private TextInputEditText inputDepartment;
    private TextInputEditText inputOfficer;
    private TextInputEditText inputDecision;
    private TextInputEditText inputNotes;
    private MaterialButton btnSubmit;
    private MaterialButton btnCancel;
    private ProgressBar loadingBar;
    private TextView titlePreview;

    private final Priority[] priorityValues = Priority.values();
    private final ComplaintStatus[] statusValues = ComplaintStatus.values();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gov_decision, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        complaintId = (args != null) ? args.getString("complaintId", "") : "";

        spinnerPriority = view.findViewById(R.id.spinner_official_priority);
        spinnerStatus = view.findViewById(R.id.spinner_complaint_status);
        inputDepartment = view.findViewById(R.id.input_assigned_department);
        inputOfficer = view.findViewById(R.id.input_assigned_officer);
        inputDecision = view.findViewById(R.id.input_official_decision);
        inputNotes = view.findViewById(R.id.input_internal_notes);
        btnSubmit = view.findViewById(R.id.btn_submit_decision);
        btnCancel = view.findViewById(R.id.btn_cancel_decision);
        loadingBar = view.findViewById(R.id.decision_loading);
        titlePreview = view.findViewById(R.id.decision_complaint_title_preview);

        viewModel = new ViewModelProvider(requireActivity()).get(GovViewModel.class);

        setupSpinners();
        observeViewModel(view);
        prefillFromSelectedComplaint();

        btnSubmit.setOnClickListener(v -> submitDecision());
        btnCancel.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Navigation header
        View btnBack = view.findViewById(R.id.btn_decision_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }
        View btnHome = view.findViewById(R.id.btn_decision_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> Navigation.findNavController(v).popBackStack(
                    R.id.govDashboardFragment,
                    false
            ));
        }
    }

    private void setupSpinners() {
        // Priority spinner
        String[] priorityLabels = new String[priorityValues.length];
        for (int i = 0; i < priorityValues.length; i++) {
            priorityLabels[i] = priorityValues[i].name();
        }
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                priorityLabels) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        // Status spinner
        String[] statusLabels = new String[statusValues.length];
        for (int i = 0; i < statusValues.length; i++) {
            statusLabels[i] = statusValues[i].getDisplayName();
        }
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                statusLabels) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
    }

    /** Pre-fill form with existing data from the currently selected complaint. */
    private void prefillFromSelectedComplaint() {
        Complaint existing = viewModel.getSelectedComplaint().getValue();
        if (existing == null) return;

        // Title preview
        if (existing.getTitle() != null) {
            titlePreview.setText("Complaint: " + existing.getTitle());
        }

        // Pre-fill priority
        if (existing.getOfficialPriority() != null) {
            for (int i = 0; i < priorityValues.length; i++) {
                if (priorityValues[i] == existing.getOfficialPriority()) {
                    spinnerPriority.setSelection(i);
                    break;
                }
            }
        } else if (existing.getAiPriority() != null) {
            // Default to AI recommendation as a suggestion
            for (int i = 0; i < priorityValues.length; i++) {
                if (priorityValues[i] == existing.getAiPriority()) {
                    spinnerPriority.setSelection(i);
                    break;
                }
            }
        }

        // Pre-fill status
        if (existing.getStatus() != null) {
            for (int i = 0; i < statusValues.length; i++) {
                if (statusValues[i] == existing.getStatus()) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        }

        // Pre-fill text fields
        if (existing.getAssignedDepartment() != null) {
            inputDepartment.setText(existing.getAssignedDepartment());
        } else if (existing.getSuggestedDepartment() != null) {
            // Default to AI suggestion as a starting point
            inputDepartment.setText(existing.getSuggestedDepartment());
        }

        if (existing.getAssignedOfficer() != null) {
            inputOfficer.setText(existing.getAssignedOfficer());
        }
        if (existing.getOfficialDecision() != null) {
            inputDecision.setText(existing.getOfficialDecision());
        }
        if (existing.getInternalNotes() != null) {
            inputNotes.setText(existing.getInternalNotes());
        }
    }

    private void observeViewModel(View rootView) {
        viewModel.getDecisionLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!isLoading);
        });

        viewModel.getDecisionSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.clearDecisionResult();
                Toast.makeText(requireContext(),
                        "Decision saved successfully!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(rootView).navigateUp();
            }
        });

        viewModel.getDecisionError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                viewModel.clearDecisionResult();
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitDecision() {
        // Validate required fields
        String department = inputDepartment.getText() != null
                ? inputDepartment.getText().toString().trim() : "";
        String decisionText = inputDecision.getText() != null
                ? inputDecision.getText().toString().trim() : "";

        if (TextUtils.isEmpty(department)) {
            inputDepartment.setError("Assigned department is required");
            inputDepartment.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(decisionText)) {
            inputDecision.setError("Official decision is required");
            inputDecision.requestFocus();
            return;
        }
        if (complaintId == null || complaintId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No complaint ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        Priority officialPriority = priorityValues[spinnerPriority.getSelectedItemPosition()];
        ComplaintStatus newStatus = statusValues[spinnerStatus.getSelectedItemPosition()];

        String officer = inputOfficer.getText() != null
                ? inputOfficer.getText().toString().trim() : "";
        String notes = inputNotes.getText() != null
                ? inputNotes.getText().toString().trim() : "";

        viewModel.submitDecision(complaintId, officialPriority, department,
                officer, decisionText, notes, newStatus);
    }
}