package com.civicai;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.civicai.databinding.ActivityMainBinding;
import com.civicai.model.UserRole;
import com.civicai.navigation.AppNavigator;

/**
 * Root Entry Activity.
 * Enables quick role selection and routing between Citizen and Government Official workflows.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRoleCitizen.setOnClickListener(v -> {
            AppNavigator.navigateToRoleHome(MainActivity.this, UserRole.CITIZEN);
        });

        binding.btnRoleGovernment.setOnClickListener(v -> {
            AppNavigator.navigateToRoleHome(MainActivity.this, UserRole.GOVERNMENT_OFFICIAL);
        });
    }
}
