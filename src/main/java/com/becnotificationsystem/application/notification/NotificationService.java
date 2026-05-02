package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.*;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

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

        return NotificationInfo.Detail.from(notificationRepository.save(notification));
    }

    public NotificationInfo.Detail findById(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return NotificationInfo.Detail.from(notification);
    }

}
