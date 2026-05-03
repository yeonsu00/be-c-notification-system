package com.becnotificationsystem.interfaces.scheduler;

import com.becnotificationsystem.application.notification.NotificationFacade;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationStartupScheduler {

    private final NotificationFacade notificationFacade;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        LocalDateTime now = LocalDateTime.now();
        notificationFacade.recoverScheduledNotifications(now);
    }
}
