package rewear.services;

import java.util.ArrayList;
import java.util.List;
import rewear.models.Notification;
import rewear.storage.DataStore;

/**
 * Service managing persistent student notifications.
 */
public class NotificationService {
    private final DataStore dataStore;

    public NotificationService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Notification> getNotificationsForUser(String userId) {
        List<Notification> list = new ArrayList<>();
        for (Notification n : dataStore.getAllNotifications()) {
            if (n.getUserId().equals(userId)) {
                list.add(n);
            }
        }
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }

    public int getUnreadCount(String userId) {
        int count = 0;
        for (Notification n : dataStore.getAllNotifications()) {
            if (n.getUserId().equals(userId) && !n.isRead()) {
                count++;
            }
        }
        return count;
    }

    public void markAsRead(String notificationId, String userId) {
        for (Notification n : dataStore.getAllNotifications()) {
            if (n.getId().equals(notificationId) && n.getUserId().equals(userId)) {
                n.setRead(true);
                dataStore.updateNotification(n);
                break;
            }
        }
    }

    public void markAllAsRead(String userId) {
        for (Notification n : dataStore.getAllNotifications()) {
            if (n.getUserId().equals(userId) && !n.isRead()) {
                n.setRead(true);
                dataStore.updateNotification(n);
            }
        }
    }
}

