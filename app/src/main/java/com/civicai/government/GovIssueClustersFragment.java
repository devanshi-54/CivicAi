package com.civicai.government;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.civicai.R;
import com.civicai.common.Constants;
import com.civicai.databinding.FragmentGovIssueClustersBinding;

/**
 * Government Issue Clusters Screen (ID: b1be558d366742f9984c38e7c0d967f7).
 */
public class GovIssueClustersFragment extends Fragment {

    private FragmentGovIssueClustersBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovIssueClustersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvIssueClusters.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnOpenSampleCluster.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_CLUSTER_ID, "CLS-101");
            Navigation.findNavController(v).navigate(R.id.action_clusters_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
