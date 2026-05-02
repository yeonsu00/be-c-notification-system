package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;

public interface NotificationSender {

    NotificationChannel support();

    void send(Notification notification);
}
