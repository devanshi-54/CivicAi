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
import com.civicai.databinding.FragmentGovComplaintsBinding;

/**
 * Government Complaints Screen (ID: 875d03a8e950447c926f924f3f5f50d7).
 */
public class GovComplaintsFragment extends Fragment {

    private FragmentGovComplaintsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovComplaintsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvGovComplaintsList.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnOpenSampleComplaintGov.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-SAMPLE-01");
            Navigation.findNavController(v).navigate(R.id.action_complaints_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
