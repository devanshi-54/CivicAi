package com.civicai.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.civicai.firebase.FirestoreHelper;
import com.civicai.model.User;
import com.civicai.model.UserRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for User profiles and current session role state.
 */
public class UserRepository implements IUserRepository {
    private static final String PREFS = "civicai_user_profile";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static UserRepository instance;

    private final SharedPreferences preferences;
    private final Map<String, User> userCache = new HashMap<>();
    private User currentUser;
    private UserRole currentRole = UserRole.CITIZEN;

    private UserRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String userId = preferences.getString(KEY_USER_ID, null);
        if (userId == null || userId.trim().isEmpty()) {
            userId = "usr-" + UUID.randomUUID().toString();
            preferences.edit().putString(KEY_USER_ID, userId).apply();
        }
        currentUser = new User(userId, UserRole.CITIZEN,
                preferences.getString(KEY_NAME, ""), preferences.getString(KEY_EMAIL, ""),
                preferences.getString(KEY_PHONE, ""));
        userCache.put(userId, currentUser);
        FirestoreHelper.getUserById(userId, new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                currentUser = user;
                userCache.put(user.getUserId(), user);
                persistLocally(user);
            }
            @Override public void onError(Exception ignored) { }
        });
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) instance = new UserRepository(context);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) throw new IllegalStateException("UserRepository has not been initialized");
        return instance;
    }

    @Override public void getCurrentUser(RepositoryCallback<User> callback) {
        if (callback != null) callback.onSuccess(currentUser);
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

    private void persistLocally(User user) {
        preferences.edit()
                .putString(KEY_USER_ID, user.getUserId())
                .putString(KEY_NAME, user.getName() == null ? "" : user.getName())
                .putString(KEY_EMAIL, user.getEmail() == null ? "" : user.getEmail())
                .putString(KEY_PHONE, user.getPhone() == null ? "" : user.getPhone())
                .apply();
    }

    @Override public void setCurrentRole(UserRole role) {
        currentRole = role == null ? UserRole.CITIZEN : role;
        currentUser.setRole(currentRole);
    }

    @Override public UserRole getCurrentRole() { return currentRole; }
}
