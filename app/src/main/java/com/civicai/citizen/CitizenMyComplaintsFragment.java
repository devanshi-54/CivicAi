package com.civicai.citizen;

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
import com.civicai.databinding.FragmentCitizenMyComplaintsBinding;

/**
 * Citizen My Complaints Screen (ID: 16864e6bd56a47159e441c8a20d42910).
 */
public class CitizenMyComplaintsFragment extends Fragment {

    private FragmentCitizenMyComplaintsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenMyComplaintsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvMyComplaints.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnSelectSampleComplaint.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-1082");
            Navigation.findNavController(v).navigate(R.id.action_my_complaints_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
