package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;

public interface NotificationRepository {

    Notification save(Notification notification);

}
