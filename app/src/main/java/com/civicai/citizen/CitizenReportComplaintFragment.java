package com.civicai.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.civicai.R;
import com.civicai.common.Constants;
import com.civicai.databinding.FragmentCitizenReportBinding;

/**
 * Citizen Report Complaint Screen (ID: 61528aab13944d61862a88851c46cd6a).
 * Triggers AI triage analysis route.
 */
public class CitizenReportComplaintFragment extends Fragment {

    private FragmentCitizenReportBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubmitComplaint.setOnClickListener(v -> {
            String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString().trim() : "";
            if (title.isEmpty()) {
                binding.etTitle.setText("Dangerous Road Pothole near School");
            }
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-NEW-TRIAGE-01");
            Navigation.findNavController(v).navigate(R.id.action_report_to_ai_analysis, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
