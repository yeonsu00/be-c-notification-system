package com.becnotificationsystem.domain.notification;

import com.becnotificationsystem.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@Entity
@Table(
        name = "notification",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false, length = 50)
    private String referenceType;

    @Column(nullable = false, length = 255)
    private String idempotencyKey;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetryCount;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Builder
    private Notification(Long receiverId, NotificationType notificationType, NotificationChannel channel,
                         NotificationStatus status, Long referenceId, String referenceType,
                         String idempotencyKey, int retryCount, int maxRetryCount,
                         LocalDateTime scheduledAt, boolean deleted) {
        this.receiverId = receiverId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.status = status;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.idempotencyKey = idempotencyKey;
        this.retryCount = retryCount;
        this.maxRetryCount = maxRetryCount;
        this.scheduledAt = scheduledAt;
        this.deleted = deleted;
    }

    public static Notification of(Long receiverId, NotificationType notificationType, NotificationChannel channel,
                                   Long referenceId, String referenceType, String idempotencyKey,
                                   LocalDateTime scheduledAt) {
        return Notification.builder()
                .receiverId(receiverId)
                .notificationType(notificationType)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .maxRetryCount(3)
                .scheduledAt(scheduledAt)
                .deleted(false)
                .build();
    }
}
