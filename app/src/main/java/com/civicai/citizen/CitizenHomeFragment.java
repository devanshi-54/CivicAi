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
import com.civicai.databinding.FragmentCitizenHomeBinding;

/**
 * Citizen Home Screen (ID: 154709252b154afe9135b316e0bc5730).
 */
public class CitizenHomeFragment extends Fragment {

    private FragmentCitizenHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnQuickReport.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_home_to_report);
        });

        binding.btnCitizenNavOnboarding.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_home_to_onboarding);
        });

        binding.btnCitizenNavMyComplaints.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.citizen_my_complaints);
        });

        binding.btnCitizenNavNotifications.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.citizen_notifications);
        });

        binding.btnCitizenNavProfile.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.citizen_profile_settings);
        });

        binding.btnOpenSampleComplaintCitizen.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-CITIZEN-01");
            Navigation.findNavController(v).navigate(R.id.action_home_to_complaint_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
