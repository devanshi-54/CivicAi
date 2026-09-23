package com.civicai.repository;

import com.civicai.model.AppNotification;
import com.civicai.model.UserRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Repository handling in-app notifications.
 */
public class NotificationRepository implements INotificationRepository {

    private static NotificationRepository instance;
    private final List<AppNotification> notifications = new ArrayList<>();

    private NotificationRepository() {
        initSampleNotifications();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    private void initSampleNotifications() {
        AppNotification n1 = new AppNotification(
                "NOTIF-1",
                "usr_citizen_01",
                UserRole.CITIZEN,
                "Complaint Status Updated",
                "Your complaint regarding 'Open pothole on Main Street' is now Under Review.",
                "STATUS_UPDATE",
                "CMP-001"
        );
        n1.setTimestamp(System.currentTimeMillis() - 3600000);

        AppNotification n2 = new AppNotification(
                "NOTIF-2",
                "usr_gov_01",
                UserRole.GOVERNMENT_OFFICIAL,
                "Critical Priority Triage Alert",
                "AI prioritized a new complaint #CMP-001 as HIGH priority in Ward 12.",
                "PRIORITY_ALERT",
                "CMP-001"
        );
        n2.setTimestamp(System.currentTimeMillis() - 1800000);

        notifications.add(n1);
        notifications.add(n2);
    }

    @Override
    public void getNotificationsForRole(UserRole role, RepositoryCallback<List<AppNotification>> callback) {
        List<AppNotification> results = new ArrayList<>();
        for (AppNotification n : notifications) {
            if (n.getTargetRole() == role) {
                results.add(n);
            }
        }
        Collections.sort(results, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        callback.onSuccess(results);
    }

    @Override
    public void markAsRead(String notificationId, RepositoryCallback<Void> callback) {
        for (AppNotification n : notifications) {
            if (n.getNotificationId().equals(notificationId)) {
                n.setRead(true);
                break;
            }
        }
        callback.onSuccess(null);
    }

    @Override
    public void addNotification(AppNotification notification, RepositoryCallback<Void> callback) {
        if (notification.getNotificationId() == null) {
            notification.setNotificationId("NOTIF-" + UUID.randomUUID().toString().substring(0, 8));
        }
        notifications.add(0, notification);
        callback.onSuccess(null);
    }
}
