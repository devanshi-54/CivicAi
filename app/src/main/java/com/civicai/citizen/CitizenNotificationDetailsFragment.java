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
import com.civicai.common.Constants;
import com.civicai.databinding.FragmentCitizenNotificationDetailsBinding;

/**
 * Citizen Notification Details Screen (ID: 100a0f5680ea402db6883d359f7fc030).
 * Accepts notificationId and optional complaintId.
 */
public class CitizenNotificationDetailsFragment extends Fragment {

    private FragmentCitizenNotificationDetailsBinding binding;
    private String notificationId = "NOTIF-1";
    private String complaintId = "CMP-001";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenNotificationDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().containsKey(Constants.EXTRA_NOTIFICATION_ID)) {
                notificationId = getArguments().getString(Constants.EXTRA_NOTIFICATION_ID);
            }
            if (getArguments().containsKey(Constants.EXTRA_COMPLAINT_ID)) {
                complaintId = getArguments().getString(Constants.EXTRA_COMPLAINT_ID);
            }
        }

        binding.tvNotificationDetailId.setText("Notification ID: " + notificationId);

        binding.btnGoToRelatedComplaint.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_COMPLAINT_ID, complaintId);
            Navigation.findNavController(v).navigate(R.id.action_notif_details_to_complaint_details, args);
        });

        binding.btnBackFromNotifDetails.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
