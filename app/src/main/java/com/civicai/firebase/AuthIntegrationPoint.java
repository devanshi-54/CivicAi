package com.civicai.firebase;

import com.civicai.model.User;
import com.civicai.model.UserRole;
import com.civicai.repository.RepositoryCallback;

/**
 * Clean integration interface for the Authentication module managed by team member Maheswaran.
 * Provides authenticated session data, role verification, and token retrieval without competing with the auth pipeline.
 */
public interface AuthIntegrationPoint {

    /**
     * Check if a user is currently authenticated.
     */
    boolean isAuthenticated();

    /**
     * Get the active user's unique identifier.
     */
    String getCurrentUserId();

    /**
     * Get the active user's assigned role (CITIZEN or GOVERNMENT_OFFICIAL).
     */
    UserRole getCurrentRole();

    /**
     * Retrieve full profile information of the currently authenticated user.
     */
    void getCurrentUserProfile(RepositoryCallback<User> callback);

    /**
     * Listener interface for authentication state and role changes.
     */
    interface AuthStateListener {
        void onAuthStateChanged(boolean isAuthenticated, User user, UserRole role);
    }

    void registerAuthStateListener(AuthStateListener listener);
    void unregisterAuthStateListener(AuthStateListener listener);
}
