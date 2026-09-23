package com.civicai.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.civicai.MainActivity;
import com.civicai.databinding.FragmentCitizenProfileSettingsBinding;
import com.civicai.model.User;
import com.civicai.repository.RepositoryCallback;
import com.civicai.repository.UserRepository;

/**
 * Citizen Profile & Settings Screen (ID: bd8a62c47eaa48859fce68e3ed7ea300).
 */
public class CitizenProfileSettingsFragment extends Fragment {

    private FragmentCitizenProfileSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenProfileSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (binding != null && user != null) {
                    binding.tvCitizenProfileName.setText(user.getName());
                    binding.tvCitizenProfileEmail.setText(user.getEmail());
                    binding.tvCitizenProfilePhone.setText(user.getPhone() != null ? user.getPhone() : "+91 9876543210");
                }
            }

            @Override
            public void onError(Exception exception) {}
        });

        binding.btnCitizenSignOut.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
