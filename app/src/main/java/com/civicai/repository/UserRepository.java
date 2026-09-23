package com.civicai.repository;

import com.civicai.model.User;
import com.civicai.model.UserRole;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for User profiles and current session role state.
 */
public class UserRepository implements IUserRepository {

    private static UserRepository instance;
    private final Map<String, User> userCache = new HashMap<>();
    private User currentUser;
    private UserRole currentRole = UserRole.CITIZEN;

    private UserRepository() {
        initDefaultUsers();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    private void initDefaultUsers() {
        User citizen = new User("usr_citizen_01", UserRole.CITIZEN, "Arun Kumar", "arun@civicai.org", "+91 9876543210");
        User official = new User("usr_gov_01", UserRole.GOVERNMENT_OFFICIAL, "Dr. S. Ramesh", "ramesh.gov@civicai.org", "+91 9876500001");
        official.setDepartment("Public Works & Infrastructure");
        official.setWard("Ward 12 Central");

        userCache.put(citizen.getUserId(), citizen);
        userCache.put(official.getUserId(), official);
        currentUser = citizen;
    }

    @Override
    public void getCurrentUser(RepositoryCallback<User> callback) {
        callback.onSuccess(currentUser);
    }

    @Override
    public void getUserById(String userId, RepositoryCallback<User> callback) {
        User u = userCache.get(userId);
        if (u != null) {
            callback.onSuccess(u);
        } else {
            callback.onError(new Exception("User not found: " + userId));
        }
    }

    @Override
    public void saveUser(User user, RepositoryCallback<Void> callback) {
        userCache.put(user.getUserId(), user);
        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
            currentUser = user;
        }
        callback.onSuccess(null);
    }

    @Override
    public void setCurrentRole(UserRole role) {
        this.currentRole = role;
        if (role == UserRole.GOVERNMENT_OFFICIAL) {
            currentUser = userCache.get("usr_gov_01");
        } else {
            currentUser = userCache.get("usr_citizen_01");
        }
    }

    @Override
    public UserRole getCurrentRole() {
        return currentRole;
    }
}
