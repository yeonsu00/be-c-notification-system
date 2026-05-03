package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.application.sendlog.NotificationSendLogService;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final NotificationSendLogService notificationSendLogService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationInfo.Detail register(NotificationCreateRequest request) {
        NotificationInfo.Detail result = notificationService.register(request);
        if (NotificationStatus.isPending(result.status())) {
            eventPublisher.publishEvent(NotificationCreatedEvent.of(result.notificationId()));
        } else {
            scheduleTask(result.notificationId(), result.scheduledAt());
        }
        return result;
    }

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

    public void recoverPendingNotifications(LocalDateTime threshold) {
        notificationService.recoverPendingNotifications(threshold);
    }

    public void recoverStuckProcessingNotifications(LocalDateTime threshold) {
        notificationService.findStuckProcessingIdsBefore(threshold)
                .forEach(id -> notificationService.recoverStuckNotification(
                        id, notificationSendLogService.existsSuccessLog(id)));
    }

    public void retryFailedNotifications() {
        notificationService.findFailedIds().forEach(this::sendNotification);
    }

    public void recoverScheduledNotifications(LocalDateTime baseTime) {
        notificationService.findAllScheduled().forEach(info -> {
            if (info.scheduledAt().isAfter(baseTime)) {
                scheduleTask(info.notificationId(), info.scheduledAt());
                log.info("예약 알림 재등록: notificationId={}, scheduledAt={}", info.notificationId(), info.scheduledAt());
            } else {
                notificationService.scheduledToPending(info.notificationId());
                log.info("지연된 예약 알림 즉시 처리: notificationId={}", info.notificationId());
            }
        });
    }

    public void sendScheduledNotifications() {
        notificationService.findDueScheduledIds().forEach(notificationService::scheduledToPending);
    }

    private void scheduleTask(Long notificationId, LocalDateTime scheduledAt) {
        taskScheduler.schedule(
                () -> notificationService.scheduledToPending(notificationId),
                scheduledAt.atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
