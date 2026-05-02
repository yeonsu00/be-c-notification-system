package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.application.notification.NotificationSender;
import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InAppNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel support() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        log.info("[IN_APP] notificationId={}, receiverId={}, type={}",
                notification.getId(), notification.getReceiverId(), notification.getNotificationType());
    }
}
