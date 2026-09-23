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
import com.civicai.databinding.FragmentCitizenOnboarding3Binding;

/**
 * Citizen Onboarding Screen 3 (ID: 35d38b90a1f34a40be1ab95c042b3a29).
 * Completes onboarding and transitions into Citizen Home.
 */
public class CitizenOnboarding3Fragment extends Fragment {

    private FragmentCitizenOnboarding3Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenOnboarding3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnFinishOnboarding.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding3_to_home);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
