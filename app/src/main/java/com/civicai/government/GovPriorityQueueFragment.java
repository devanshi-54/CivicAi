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
import com.civicai.databinding.FragmentGovPriorityQueueBinding;

/**
 * Government Priority Queue Screen (ID: cb382d3c335440a8b91d078a0f105b63).
 */
public class GovPriorityQueueFragment extends Fragment {

    private FragmentGovPriorityQueueBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovPriorityQueueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvPriorityQueue.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnOpenPriorityComplaint.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-PRIORITY-01");
            Navigation.findNavController(v).navigate(R.id.action_priority_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
