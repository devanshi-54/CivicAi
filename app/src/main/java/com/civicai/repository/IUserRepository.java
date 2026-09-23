package com.civicai.repository;

import com.civicai.model.User;
import com.civicai.model.UserRole;

public interface IUserRepository {
    void getCurrentUser(RepositoryCallback<User> callback);
    void getUserById(String userId, RepositoryCallback<User> callback);
    void saveUser(User user, RepositoryCallback<Void> callback);
    void setCurrentRole(UserRole role);
    UserRole getCurrentRole();
}
