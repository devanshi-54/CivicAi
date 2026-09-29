package com.civicai.repository;

import com.civicai.model.User;
import com.civicai.model.UserRole;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
        if (isAuthenticated()) {
            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fUser == null) {
                callback.onError(new Exception("Not authenticated"));
                return;
            }
            if (currentUser != null && currentUser.getUserId().equals(fUser.getUid())) {
                callback.onSuccess(currentUser);
                return;
            }
            
            FirebaseFirestore.getInstance().collection("users").document(fUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        callback.onSuccess(currentUser);
                    } else {
                        // Create new profile for phone auth user
                        User newUser = new User(fUser.getUid(), UserRole.CITIZEN, "New Citizen", "", fUser.getPhoneNumber());
                        saveUser(newUser, new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                currentUser = newUser;
                                callback.onSuccess(currentUser);
                            }
                            @Override
                            public void onError(Exception e) {
                                callback.onError(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
        } else {
            callback.onError(new Exception("User not authenticated"));
        }
    }

    public boolean isAuthenticated() {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public void setAuthenticated(boolean auth) {
        if (!auth) {
            FirebaseAuth.getInstance().signOut();
            currentUser = null;
        }
    }

    @Override
    public void getUserById(String userId, RepositoryCallback<User> callback) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    callback.onSuccess(doc.toObject(User.class));
                } else {
                    callback.onError(new Exception("User not found: " + userId));
                }
            })
            .addOnFailureListener(e -> callback.onError(e));
    }

    @Override
    public void saveUser(User user, RepositoryCallback<Void> callback) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUserId())
            .set(user)
            .addOnSuccessListener(aVoid -> {
                if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
                    currentUser = user;
                }
                callback.onSuccess(null);
            })
            .addOnFailureListener(e -> callback.onError(e));
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
