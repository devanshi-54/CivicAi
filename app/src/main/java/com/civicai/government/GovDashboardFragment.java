package com.civicai.government;

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
import com.civicai.databinding.FragmentGovDashboardBinding;

/**
 * Government Dashboard Screen (ID: eee2c08b5cf34e5a96fcacdeab24220d).
 */
public class GovDashboardFragment extends Fragment {

    private FragmentGovDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cardHighPriority.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_priority_queue);
        });

        binding.cardActiveClusters.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_clusters);
        });

        binding.btnGovNavPriorityQueue.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_priority_queue);
        });

        binding.btnGovNavIssueClusters.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_clusters);
        });

        binding.btnGovNavAnalytics.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_analytics);
        });

        binding.btnGovNavDepartment.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("departmentId", "DEPT-PWD-01");
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_department, args);
        });

        binding.btnGovNavProfile.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_profile);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
