package com.civicai.notifications;

import android.util.Log;
import com.civicai.model.AppNotification;
import com.civicai.repository.NotificationRepository;
import com.civicai.repository.RepositoryCallback;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Concrete implementation of INotificationService.
 * Handles Firebase Cloud Messaging topics and local notification dispatch.
 */
public class NotificationService implements INotificationService {

    private static final String TAG = "NotificationService";
    private static NotificationService instance;

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    @Override
    public void sendNotification(AppNotification notification, RepositoryCallback<Void> callback) {
        NotificationRepository.getInstance().addNotification(notification, callback);
    }

    @Override
    public void subscribeToTopic(String topic, RepositoryCallback<Void> callback) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to FCM topic: " + topic);
                            callback.onSuccess(null);
                        } else {
                            Log.w(TAG, "Failed to subscribe to FCM topic: " + topic, task.getException());
                            callback.onError(task.getException() != null ? task.getException() : new Exception("Subscription failed"));
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "FCM subscription deferred: " + e.getMessage());
            callback.onSuccess(null);
        }
    }

    @Override
    public void unsubscribeFromTopic(String topic, RepositoryCallback<Void> callback) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            callback.onError(task.getException() != null ? task.getException() : new Exception("Unsubscribe failed"));
                        }
                    });
        } catch (Exception e) {
            callback.onSuccess(null);
        }
    }
}
