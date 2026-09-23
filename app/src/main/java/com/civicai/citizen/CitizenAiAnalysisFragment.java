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
import com.civicai.databinding.FragmentCitizenAiAnalysisBinding;

/**
 * Citizen AI Analysis Screen (ID: 38e9fdafdb564fbbb3ad2623dc9c8faa).
 * Displays AI triage result before final submission confirmation.
 */
public class CitizenAiAnalysisFragment extends Fragment {

    private FragmentCitizenAiAnalysisBinding binding;
    private String complaintId = "CMP-AI-849201";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenAiAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_COMPLAINT_ID)) {
            complaintId = getArguments().getString(Constants.EXTRA_COMPLAINT_ID);
        }

        binding.btnProceedToSubmitted.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, complaintId);
            Navigation.findNavController(v).navigate(R.id.action_ai_analysis_to_submitted, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
