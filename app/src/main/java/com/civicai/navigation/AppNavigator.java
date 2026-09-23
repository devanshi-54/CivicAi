package com.civicai.navigation;

import android.content.Context;
import android.content.Intent;
import com.civicai.citizen.CitizenMainActivity;
import com.civicai.government.GovMainActivity;
import com.civicai.model.UserRole;
import com.civicai.repository.UserRepository;

/**
 * Central navigation coordinator managing role-based entry routing.
 */
public class AppNavigator {

    public static void navigateToRoleHome(Context context, UserRole role) {
        UserRepository.getInstance().setCurrentRole(role);

        Intent intent;
        if (role == UserRole.GOVERNMENT_OFFICIAL) {
            intent = new Intent(context, GovMainActivity.class);
        } else {
            intent = new Intent(context, CitizenMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
