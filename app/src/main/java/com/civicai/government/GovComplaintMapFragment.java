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
import com.civicai.databinding.FragmentGovComplaintMapBinding;

/**
 * Government Geographic Complaint Map Screen (ID: 18c13714cbaa4d389bf90229a866b97b).
 * Navigates to complaint details when a map marker is clicked.
 */
public class GovComplaintMapFragment extends Fragment {

    private FragmentGovComplaintMapBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovComplaintMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectMapMarker.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-MAP-01");
            Navigation.findNavController(v).navigate(R.id.action_map_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
