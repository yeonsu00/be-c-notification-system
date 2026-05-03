package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.application.notification.NotificationInfo.ListItem;
import com.becnotificationsystem.domain.notification.*;
import com.becnotificationsystem.global.common.response.PageResponse;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final List<NotificationSender> senders;

    @Transactional
    public NotificationInfo.Detail register(NotificationCreateRequest request) {
        String idempotencyKey = Notification.generateIdempotencyKey(
                request.receiverId(),
                request.notificationType(),
                request.referenceId(),
                request.referenceType(),
                request.channel()
        );

        Notification notification = Notification.of(
                request.receiverId(),
                request.notificationType(),
                request.channel(),
                request.referenceId(),
                request.referenceType(),
                idempotencyKey,
                request.scheduledAt()
        );

        NotificationInfo.Detail result = NotificationInfo.Detail.from(notificationRepository.save(notification));
        eventPublisher.publishEvent(NotificationCreatedEvent.of(result.notificationId()));
        return result;
    }

    @Transactional(readOnly = true)
    public NotificationInfo.Detail findById(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return NotificationInfo.Detail.from(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListItem> findByReceiverId(Long receiverId, Boolean readFilter, Pageable pageable) {
        Page<Notification> notificationPage;
        if (readFilter == null) {
            notificationPage = notificationRepository.findByReceiverIdAndDeletedFalse(receiverId, pageable);
        } else if (readFilter) {
            notificationPage = notificationRepository.findByReceiverIdAndStatusAndDeletedFalse(
                    receiverId, NotificationStatus.READ, pageable);
        } else {
            notificationPage = notificationRepository.findByReceiverIdAndStatusAndDeletedFalse(
                    receiverId, NotificationStatus.SENT, pageable);
        }

        return PageResponse.from(notificationPage.map(n ->
                NotificationInfo.ListItem.from(n, n.getNotificationType().render(Map.of()))));
    }

    @Transactional
    public Optional<NotificationInfo.ProcessResult> sendNotification(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isProcessable()) {
            log.debug("처리 불가능한 상태의 알림입니다. notificationId={}, status={}", notificationId, notification.getStatus());
            return Optional.empty();
        }

        int updated = notificationRepository.compareAndSwap(notificationId, notification.getStatus(), NotificationStatus.PROCESSING);
        if (updated == 0) {
            log.debug("다른 인스턴스가 이미 처리 중입니다. notificationId={}", notificationId);
            return Optional.empty();
        }

        notification.startProcessing();

        NotificationSender sender = senders.stream()
                .filter(s -> s.support() == notification.getChannel())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_CHANNEL_NOT_SUPPORTED));

        int attemptNumber = notification.getRetryCount() + 1;
        NotificationInfo.ProcessResult result;

        try {
            sender.send(notification);
            notification.markAsSent();
            result = NotificationInfo.ProcessResult.of(attemptNumber, true, null);
        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
            if (!notification.canRetry()) {
                notification.markAsDeadLetter();
            }
            result = NotificationInfo.ProcessResult.of(attemptNumber, false, e.getMessage());
        }

        notificationRepository.save(notification);
        return Optional.of(result);
    }

    @Transactional
    public void recoverPendingNotifications(LocalDateTime threshold) {
        notificationRepository
                .findByStatusAndCreatedAtBeforeAndDeletedFalse(NotificationStatus.PENDING, threshold)
                .forEach(n -> eventPublisher.publishEvent(NotificationCreatedEvent.of(n.getId())));
    }

    @Transactional(readOnly = true)
    public List<Long> findStuckProcessingIdsBefore(LocalDateTime threshold) {
        return notificationRepository
                .findByStatusAndUpdatedAtBeforeAndDeletedFalse(NotificationStatus.PROCESSING, threshold)
                .stream().map(Notification::getId).toList();
    }

    @Transactional
    public void recoverStuckNotification(Long notificationId, boolean hasSuccessLog) {
        Notification n = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (hasSuccessLog) {
            n.markAsSent();
        } else {
            n.resetToPending();
            eventPublisher.publishEvent(NotificationCreatedEvent.of(notificationId));
        }
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Long> findFailedIds() {
        return notificationRepository.findByStatusAndDeletedFalse(NotificationStatus.FAILED)
                .stream().map(Notification::getId).toList();
    }

}
