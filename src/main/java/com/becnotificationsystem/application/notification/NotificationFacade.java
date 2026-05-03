package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.application.sendlog.NotificationSendLogService;
import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final NotificationSendLogService notificationSendLogService;

    @Transactional
    public void sendNotification(Long notificationId) {
        notificationService.sendNotification(notificationId).ifPresent(result ->
                notificationSendLogService.saveSendLog(
                        NotificationSendLog.of(
                                notificationId,
                                result.attemptNumber(),
                                result.success() ? SendLogResult.SUCCESS : SendLogResult.FAILURE,
                                result.failureReason())));
    }
}
