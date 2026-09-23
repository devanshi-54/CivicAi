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
import com.civicai.databinding.FragmentCitizenNotificationsBinding;

/**
 * Citizen Notifications Screen (ID: 04377913b7b74d8fb48b9cc86e3a4b6e).
 */
public class CitizenNotificationsFragment extends Fragment {

    private FragmentCitizenNotificationsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvCitizenNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnSelectSampleNotification.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_NOTIFICATION_ID, "NOTIF-1");
            args.putString(Constants.EXTRA_COMPLAINT_ID, "CMP-001");
            Navigation.findNavController(v).navigate(R.id.action_notifications_to_details, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
