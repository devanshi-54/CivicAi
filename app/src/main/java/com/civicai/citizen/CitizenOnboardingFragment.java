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
import com.civicai.databinding.FragmentCitizenOnboardingBinding;

/**
 * Citizen Onboarding Screen 1 (ID: c3c3b709963f4ed68274935f74ffbd4d).
 */
public class CitizenOnboardingFragment extends Fragment {

    private FragmentCitizenOnboardingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnNextOnboarding1.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding_to_onboarding2);
        });

        binding.btnSkipOnboarding.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding_to_home);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
