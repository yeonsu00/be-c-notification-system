package com.becnotificationsystem.interfaces.scheduler;

import com.becnotificationsystem.application.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduledSendScheduler {

    private final NotificationFacade notificationFacade;

    @Scheduled(fixedDelay = 600_000)
    public void sendScheduledNotifications() {
        notificationFacade.sendScheduledNotifications();
    }
}
