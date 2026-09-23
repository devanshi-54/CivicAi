package com.civicai.notifications;

import com.civicai.model.AppNotification;
import com.civicai.repository.RepositoryCallback;

/**
 * Service interface for dispatching local and remote notifications.
 */
public interface INotificationService {
    void sendNotification(AppNotification notification, RepositoryCallback<Void> callback);
    void subscribeToTopic(String topic, RepositoryCallback<Void> callback);
    void unsubscribeFromTopic(String topic, RepositoryCallback<Void> callback);
}
