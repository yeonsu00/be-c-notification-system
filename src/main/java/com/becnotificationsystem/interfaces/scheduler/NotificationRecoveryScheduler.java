package com.becnotificationsystem.interfaces.scheduler;

import com.becnotificationsystem.application.notification.NotificationFacade;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRecoveryScheduler {

    private final NotificationFacade notificationFacade;

    @Scheduled(fixedDelay = 60_000)
    public void recoverPendingNotifications() {
        notificationFacade.recoverPendingNotifications(LocalDateTime.now().minusMinutes(2));
    }

    @Scheduled(fixedDelay = 300_000)
    public void recoverStuckProcessingNotifications() {
        notificationFacade.recoverStuckProcessingNotifications(LocalDateTime.now().minusMinutes(5));
    }

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedNotifications() {
        notificationFacade.retryFailedNotifications();
    }
}
