package com.becnotificationsystem.domain.sendlog;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "notification_send_log")
public class NotificationSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long notificationId;

    @Column(nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SendLogResult result;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Builder
    private NotificationSendLog(Long notificationId, int attemptNumber,
                                 SendLogResult result, String failureReason,
                                 LocalDateTime attemptedAt) {
        this.notificationId = notificationId;
        this.attemptNumber = attemptNumber;
        this.result = result;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }

    public static NotificationSendLog of(Long notificationId, int attemptNumber,
                                          SendLogResult result, String failureReason) {
        return NotificationSendLog.builder()
                .notificationId(notificationId)
                .attemptNumber(attemptNumber)
                .result(result)
                .failureReason(failureReason)
                .attemptedAt(LocalDateTime.now())
                .build();
    }
}
