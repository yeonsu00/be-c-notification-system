package com.becnotificationsystem.interfaces.scheduler;

import com.becnotificationsystem.application.notification.NotificationRepository;
import com.becnotificationsystem.application.notification.NotificationService;
import com.becnotificationsystem.application.notification.NotificationCreatedEvent;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationRecoveryScheduler {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60_000)
    public void recoverPendingNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        notificationRepository
                .findByStatusAndCreatedAtBeforeAndDeletedFalse(NotificationStatus.PENDING, threshold)
                .forEach(n -> eventPublisher.publishEvent(NotificationCreatedEvent.of(n.getId())));
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void recoverStuckProcessingNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        notificationRepository
                .findByStatusAndUpdatedAtBeforeAndDeletedFalse(NotificationStatus.PROCESSING, threshold)
                .forEach(n -> {
                    n.resetToPending();
                    notificationRepository.save(n);
                    eventPublisher.publishEvent(NotificationCreatedEvent.of(n.getId()));
                });
    }

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedNotifications() {
        notificationRepository
                .findByStatusAndDeletedFalse(NotificationStatus.FAILED)
                .forEach(n -> notificationService.process(n.getId()));
    }
}
