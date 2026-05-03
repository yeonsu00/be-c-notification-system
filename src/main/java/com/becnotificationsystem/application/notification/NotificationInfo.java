package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;

import java.time.LocalDateTime;

public class NotificationInfo {

    public record ProcessResult(int attemptNumber, boolean success, String failureReason) {
        public static ProcessResult of(int attemptNumber, boolean success, String failureReason) {
            return new ProcessResult(attemptNumber, success, failureReason);
        }
    }

    public record Detail(
            Long notificationId,
            NotificationStatus status,
            Long receiverId,
            NotificationType notificationType,
            NotificationChannel channel,
            Long referenceId,
            String referenceType,
            int retryCount,
            String failureReason,
            LocalDateTime scheduledAt,
            LocalDateTime sentAt,
            LocalDateTime createdAt
    ) {
        public static Detail from(Notification notification) {
            return new Detail(
                    notification.getId(),
                    notification.getStatus(),
                    notification.getReceiverId(),
                    notification.getNotificationType(),
                    notification.getChannel(),
                    notification.getReferenceId(),
                    notification.getReferenceType(),
                    notification.getRetryCount(),
                    notification.getFailureReason(),
                    notification.getScheduledAt(),
                    notification.getSentAt(),
                    notification.getCreatedAt()
            );
        }
    }

    public record ListItem(
            Long notificationId,
            NotificationType notificationType,
            NotificationChannel channel,
            NotificationStatus status,
            boolean isRead,
            String message,
            LocalDateTime sentAt,
            LocalDateTime createdAt
    ) {
        public static ListItem from(Notification notification, String message) {
            return new ListItem(
                    notification.getId(),
                    notification.getNotificationType(),
                    notification.getChannel(),
                    notification.getStatus(),
                    notification.getStatus() == NotificationStatus.READ,
                    message,
                    notification.getSentAt(),
                    notification.getCreatedAt()
            );
        }
    }
}
