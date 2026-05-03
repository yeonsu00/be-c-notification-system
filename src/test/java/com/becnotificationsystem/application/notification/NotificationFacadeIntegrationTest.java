package com.becnotificationsystem.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import com.becnotificationsystem.infrastructure.notification.NotificationJpaRepository;
import com.becnotificationsystem.infrastructure.sendlog.NotificationSendLogJpaRepository;
import com.becnotificationsystem.interfaces.listener.NotificationEventListener;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class NotificationFacadeIntegrationTest {

    @Autowired
    private NotificationFacade notificationFacade;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private NotificationSendLogJpaRepository notificationSendLogJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationEventListener notificationEventListener;

    @AfterEach
    void tearDown() {
        notificationSendLogJpaRepository.deleteAll();
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("PROCESSING stuck + SUCCESS SendLog 있음 → SENT로 상태 복구, 재발송 없음")
    void recoverAsSent_whenSuccessLogExists() {
        // arrange
        Notification notification = saveNotification(NotificationStatus.PROCESSING);
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        updateUpdatedAt(notification.getId(), threshold.minusMinutes(1));
        saveSendLog(notification.getId(), SendLogResult.SUCCESS);

        // act
        notificationFacade.recoverStuckProcessingNotifications(threshold);

        // assert
        Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("PROCESSING stuck + SUCCESS SendLog 없음 → PENDING으로 상태 복구, 이벤트 발행")
    void recoverAsPending_whenSuccessLogDoesNotExist() {
        // arrange
        Notification notification = saveNotification(NotificationStatus.PROCESSING);
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        updateUpdatedAt(notification.getId(), threshold.minusMinutes(1));

        // act
        notificationFacade.recoverStuckProcessingNotifications(threshold);

        // assert
        Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(notificationEventListener, atLeastOnce()).handleNotificationCreated(any());
    }

    @Test
    @DisplayName("recoverPendingNotifications() — PENDING stuck → 이벤트 발행 확인")
    void publishesEvent_whenPendingNotificationIsStuck() {
        // arrange
        Notification notification = saveNotification(NotificationStatus.PENDING);
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        updateCreatedAt(notification.getId(), threshold.minusMinutes(1));

        // act
        notificationFacade.recoverPendingNotifications(threshold);

        // assert
        verify(notificationEventListener, atLeastOnce()).handleNotificationCreated(any());
    }

    @Test
    @DisplayName("retryFailedNotifications() — FAILED 알림 → sendNotification() 호출 확인")
    void retrySendNotification_whenNotificationIsFailed() {
        // arrange
        Notification notification = saveNotification(NotificationStatus.FAILED);

        // act
        notificationFacade.retryFailedNotifications();

        // assert
        verify(notificationService, atLeastOnce()).sendNotification(notification.getId());
    }

    private Notification saveNotification(NotificationStatus status) {
        Notification notification = Notification.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                .channel(NotificationChannel.EMAIL)
                .status(status)
                .referenceId(100L)
                .referenceType("ORDER")
                .idempotencyKey("key-" + status + "-" + Math.random())
                .retryCount(0)
                .maxRetryCount(3)
                .deleted(false)
                .build();
        return notificationJpaRepository.save(notification);
    }

    private void saveSendLog(Long notificationId, SendLogResult result) {
        NotificationSendLog log = NotificationSendLog.of(notificationId, 1, result, null);
        notificationSendLogJpaRepository.save(log);
    }

    private void updateUpdatedAt(Long id, LocalDateTime updatedAt) {
        jdbcTemplate.update("UPDATE notification SET updated_at = ? WHERE id = ?", updatedAt, id);
    }

    private void updateCreatedAt(Long id, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE notification SET created_at = ? WHERE id = ?", createdAt, id);
    }
}
