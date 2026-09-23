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
import com.civicai.databinding.FragmentCitizenComplaintSubmittedBinding;

/**
 * Citizen Complaint Submitted Screen (ID: 659625924f1b4a26bb315fb29a8e7d62).
 */
public class CitizenComplaintSubmittedFragment extends Fragment {

    private FragmentCitizenComplaintSubmittedBinding binding;
    private String complaintId = "CMP-849201";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenComplaintSubmittedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_COMPLAINT_ID)) {
            complaintId = getArguments().getString(Constants.EXTRA_COMPLAINT_ID);
        }
        binding.tvSubmittedComplaintId.setText("Tracking ID: " + complaintId);

        binding.btnViewComplaintDetails.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, complaintId);
            Navigation.findNavController(v).navigate(R.id.action_submitted_to_details, args);
        });

        binding.btnBackToHome.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_submitted_to_home);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
