package com.civicai.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.civicai.databinding.FragmentCitizenProfileBinding;
import com.civicai.model.User;
import com.civicai.repository.RepositoryCallback;
import com.civicai.repository.UserRepository;

/**
 * Citizen Profile & Settings Screen.
 */
public class CitizenProfileFragment extends Fragment {

    private FragmentCitizenProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCitizenProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (binding != null && user != null) {
                    binding.tvCitizenName.setText(user.getName());
                    binding.tvCitizenEmail.setText(user.getEmail());
                }
            }

            @Override
            public void onError(Exception exception) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
