package com.becnotificationsystem.interfaces.api.notification;

import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record NotificationCreateRequest(
        @NotNull(message = "수신자 ID는 필수입니다.")
        Long receiverId,

        @NotNull(message = "알림 타입은 필수입니다.")
        NotificationType notificationType,

        @NotNull(message = "발송 채널은 필수입니다.")
        NotificationChannel channel,

        @NotNull(message = "참조 ID는 필수입니다.")
        Long referenceId,

        @NotBlank(message = "참조 타입은 필수입니다.")
        String referenceType,

        LocalDateTime scheduledAt
) {}
