package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.application.notification.NotificationInfo.ListItem;
import com.becnotificationsystem.domain.notification.*;
import com.becnotificationsystem.global.common.response.PageResponse;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import java.util.List;
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
@Transactional(readOnly = true)
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

    public NotificationInfo.Detail findById(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return NotificationInfo.Detail.from(notification);
    }

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

        return PageResponse.from(notificationPage.map(n -> NotificationInfo.ListItem.from(n, null)));
    }

    @Transactional
    public void process(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isProcessable()) {
            log.debug("처리 불가능한 상태의 알림입니다. notificationId={}, status={}", notificationId, notification.getStatus());
            return;
        }

        int updated = notificationRepository.compareAndSwap(notificationId, notification.getStatus(), NotificationStatus.PROCESSING);
        if (updated == 0) {
            log.debug("다른 인스턴스가 이미 처리 중입니다. notificationId={}", notificationId);
            return;
        }

        notification.startProcessing();

        NotificationSender sender = senders.stream()
                .filter(s -> s.support() == notification.getChannel())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_CHANNEL_NOT_SUPPORTED));

        try {
            sender.send(notification);
            notification.markAsSent();
        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
            if (!notification.canRetry()) {
                notification.markAsDeadLetter();
            }
        }

        notificationRepository.save(notification);
    }

}
