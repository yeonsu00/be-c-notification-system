package com.becnotificationsystem.domain.notification;

import com.becnotificationsystem.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
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
        NotificationStatus initialStatus = (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now()))
                ? NotificationStatus.SCHEDULED
                : NotificationStatus.PENDING;

        return Notification.builder()
                .receiverId(receiverId)
                .notificationType(notificationType)
                .channel(channel)
                .status(initialStatus)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .maxRetryCount(3)
                .scheduledAt(scheduledAt)
                .deleted(false)
                .build();
    }

    public void markAsPending() {
        this.status = NotificationStatus.PENDING;
    }

    public boolean isProcessable() {
        return status == NotificationStatus.PENDING || status == NotificationStatus.FAILED;
    }

    public void startProcessing() {
        this.status = NotificationStatus.PROCESSING;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.retryCount++;
        this.failureReason = reason;
        this.status = NotificationStatus.FAILED;
    }

    public void markAsDeadLetter() {
        this.status = NotificationStatus.DEAD_LETTER;
    }

    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }

    public void resetToPending() {
        this.status = NotificationStatus.PENDING;
    }

    public void markAsRead(LocalDateTime readAt) {
        this.status = NotificationStatus.READ;
        this.readAt = readAt;
    }

    public boolean matchesReceiver(Long receiverId) {
        return Objects.equals(this.receiverId, receiverId);
    }

    public static String generateIdempotencyKey(Long receiverId, NotificationType notificationType,
                                                Long referenceId, String referenceType,
                                                NotificationChannel channel) {
        String raw = receiverId + ":" + notificationType.name() + ":" + referenceId + ":" + referenceType + ":" + channel.name();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
