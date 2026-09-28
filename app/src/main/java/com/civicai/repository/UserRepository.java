package com.civicai.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.civicai.firebase.FirestoreHelper;
import com.civicai.model.User;
import com.civicai.model.UserRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stores the citizen profile locally and synchronizes it with Firestore. */
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

    public void refreshCurrentUser(RepositoryCallback<User> callback) {
        String userId = currentUser.getUserId();
        FirestoreHelper.getUserById(userId, new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                currentUser = user;
                userCache.put(userId, user);
                persistLocally(user);
                if (callback != null) callback.onSuccess(user);
            }
            @Override public void onError(Exception error) {
                if (callback != null) callback.onSuccess(currentUser);
            }
        });
    }

    @Override public void getUserById(String userId, RepositoryCallback<User> callback) {
        User cached = userCache.get(userId);
        if (cached != null) {
            if (callback != null) callback.onSuccess(cached);
            return;
        }
        FirestoreHelper.getUserById(userId, new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                userCache.put(userId, user);
                if (callback != null) callback.onSuccess(user);
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }

    @Override public void saveUser(User user, RepositoryCallback<Void> callback) {
        if (user == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("User cannot be null"));
            return;
        }
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            user.setUserId(currentUser.getUserId());
        }
        if (user.getCreatedAt() <= 0) user.setCreatedAt(System.currentTimeMillis());
        currentUser = user;
        userCache.put(user.getUserId(), user);
        persistLocally(user);
        FirestoreHelper.saveUserToFirestore(user, callback);
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
