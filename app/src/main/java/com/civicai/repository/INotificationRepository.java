package com.civicai.repository;

import com.civicai.model.AppNotification;
import com.civicai.model.UserRole;

import java.util.List;

public interface INotificationRepository {
    void getNotificationsForRole(UserRole role, RepositoryCallback<List<AppNotification>> callback);
    void markAsRead(String notificationId, RepositoryCallback<Void> callback);
    void addNotification(AppNotification notification, RepositoryCallback<Void> callback);
}
