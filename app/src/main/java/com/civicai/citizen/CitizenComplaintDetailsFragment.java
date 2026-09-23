package com.civicai.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.civicai.common.Constants;
import com.civicai.databinding.FragmentCitizenComplaintDetailsBinding;
import com.civicai.model.Complaint;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;

/**
 * Citizen Complaint Details Screen (ID: 393bf25c81914912916d45e74ced8e3f).
 * Accepts complaintId as a navigation argument.
 */
public class CitizenComplaintDetailsFragment extends Fragment {

    private FragmentCitizenComplaintDetailsBinding binding;
    private String complaintId = "CMP-DEFAULT";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenComplaintDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_COMPLAINT_ID)) {
            complaintId = getArguments().getString(Constants.EXTRA_COMPLAINT_ID);
        }

        binding.tvDetailComplaintId.setText("ID: " + complaintId);

        ComplaintRepository.getInstance().getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint result) {
                if (binding != null && result != null) {
                    binding.tvDetailComplaintTitle.setText(result.getTitle());
                    binding.tvDetailComplaintStatus.setText("Status: " + result.getStatus().getDisplayName());
                }
            }

            @Override
            public void onError(Exception exception) {
                if (binding != null) {
                    binding.tvDetailComplaintTitle.setText("Complaint #" + complaintId);
                }
            }
        });

        binding.btnBackFromDetails.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
