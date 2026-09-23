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
import com.civicai.databinding.FragmentCitizenOnboarding2Binding;

/**
 * Citizen Onboarding Screen 2 (ID: 8b9cf4b17bd14df3809f196f03d39594).
 */
public class CitizenOnboarding2Fragment extends Fragment {

    private FragmentCitizenOnboarding2Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenOnboarding2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnNextOnboarding2.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding2_to_onboarding3);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
