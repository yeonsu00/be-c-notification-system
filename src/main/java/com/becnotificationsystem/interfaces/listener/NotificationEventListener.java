package com.becnotificationsystem.interfaces.listener;

import com.becnotificationsystem.application.notification.NotificationCreatedEvent;
import com.becnotificationsystem.application.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationFacade notificationFacade;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        notificationFacade.sendNotification(event.notificationId());
    }
}
