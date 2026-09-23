package com.civicai.government;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.civicai.common.Constants;
import com.civicai.databinding.FragmentGovComplaintDetailsBinding;
import com.civicai.model.Complaint;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;

/**
 * Government Complaint Details & Triage Screen (ID: 0a8cdabde9824b3490ff48922d667fc2).
 * Accepts complaintId as a navigation argument.
 */
public class GovComplaintDetailsFragment extends Fragment {

    private FragmentGovComplaintDetailsBinding binding;
    private String complaintId = "CMP-GOV-01";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGovComplaintDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_COMPLAINT_ID)) {
            complaintId = getArguments().getString(Constants.EXTRA_COMPLAINT_ID);
        }

        binding.tvGovDetailComplaintId.setText("ID: " + complaintId);

        ComplaintRepository.getInstance().getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint result) {
                if (binding != null && result != null) {
                    binding.tvGovDetailTitle.setText(result.getTitle());
                    if (result.getAiPriority() != null) {
                        binding.tvGovDetailAiPriority.setText("Priority: " + result.getAiPriority().name() +
                                " | Severity: " + (result.getAiSeverity() != null ? result.getAiSeverity() : "Evaluated") +
                                " | Confidence: " + (int)(result.getAiConfidence() * 100) + "%");
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                if (binding != null) {
                    binding.tvGovDetailTitle.setText("Official Review Ticket #" + complaintId);
                }
            }
        });

        binding.btnBackFromGovDetails.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
