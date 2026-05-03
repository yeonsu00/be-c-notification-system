package com.becnotificationsystem.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import com.becnotificationsystem.infrastructure.notification.EmailNotificationSender;
import com.becnotificationsystem.infrastructure.notification.InAppNotificationSender;
import com.becnotificationsystem.infrastructure.notification.NotificationJpaRepository;
import com.becnotificationsystem.infrastructure.sendlog.NotificationSendLogJpaRepository;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import com.becnotificationsystem.interfaces.listener.NotificationEventListener;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
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

    @MockitoSpyBean
    private EmailNotificationSender emailNotificationSender;

    @MockitoSpyBean
    private InAppNotificationSender inAppNotificationSender;

    @MockitoBean
    private NotificationEventListener notificationEventListener;

    @MockitoBean
    private TaskScheduler taskScheduler;

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

    @Test
    @DisplayName("발송 성공 시 SUCCESS SendLog 1건 저장된다")
    void savesSendLog_whenNotificationSentSuccessfully() {
        // arrange
        Notification saved = saveNotification(NotificationStatus.PENDING);

        // act
        notificationFacade.sendNotification(saved.getId());

        // assert
        List<NotificationSendLog> logs = notificationSendLogJpaRepository.findAll();
        assertAll(
                () -> assertThat(logs).hasSize(1),
                () -> assertThat(logs.get(0).getNotificationId()).isEqualTo(saved.getId()),
                () -> assertThat(logs.get(0).getResult()).isEqualTo(SendLogResult.SUCCESS)
        );
    }

    @Test
    @DisplayName("발송 실패 시 FAILURE SendLog 1건 저장된다")
    void savesSendLog_whenNotificationSendFails() {
        // arrange
        Notification saved = saveNotification(NotificationStatus.PENDING);
        doThrow(new RuntimeException("SMTP error")).when(emailNotificationSender).send(any());

        // act
        notificationFacade.sendNotification(saved.getId());

        // assert
        List<NotificationSendLog> logs = notificationSendLogJpaRepository.findAll();
        assertAll(
                () -> assertThat(logs).hasSize(1),
                () -> assertThat(logs.get(0).getNotificationId()).isEqualTo(saved.getId()),
                () -> assertThat(logs.get(0).getResult()).isEqualTo(SendLogResult.FAILURE),
                () -> assertThat(logs.get(0).getFailureReason()).contains("SMTP error")
        );
    }

    @Test
    @DisplayName("register() — scheduledAt이 미래이면 TaskScheduler.schedule이 호출되고 즉시 이벤트 리스너는 호출되지 않는다")
    void schedulesTask_whenRegisterWithFutureScheduledAt() {
        NotificationCreateRequest request = new NotificationCreateRequest(
                        1L,
                        NotificationType.ENROLLMENT_COMPLETE,
                        NotificationChannel.IN_APP,
                        11_000L,
                        "FACADE_SCHEDULE",
                        LocalDateTime.now().plusHours(3)
                );

        notificationFacade.register(request);

        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        verify(notificationEventListener, never()).handleNotificationCreated(any());
    }

    @Test
    @DisplayName("register() — scheduledAt이 null이면 TaskScheduler를 쓰지 않고 커밋 후 이벤트가 발행된다")
    void publishesEvent_whenRegisterImmediate() {
        NotificationCreateRequest request = new NotificationCreateRequest(
                        1L,
                        NotificationType.PAYMENT_CONFIRMED,
                        NotificationChannel.EMAIL,
                        11_001L,
                        "FACADE_IMMEDIATE",
                        null
                );

        notificationFacade.register(request);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(notificationEventListener, atLeastOnce()).handleNotificationCreated(any());
    }

    @Test
    @DisplayName("recoverScheduledNotifications() — 아직 시각이 안 된 예약은 TaskScheduler에 재등록한다")
    void reRegistersSchedule_whenScheduledTimeIsStillInFuture() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        Notification n = Notification.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SCHEDULED)
                .referenceId(11_002L)
                .referenceType("RECOVER_FUTURE")
                .idempotencyKey("recover-future-" + System.nanoTime())
                .retryCount(0)
                .maxRetryCount(3)
                .scheduledAt(future)
                .deleted(false)
                .build();
        Notification saved = notificationJpaRepository.save(n);

        notificationFacade.recoverScheduledNotifications(LocalDateTime.now());

        verify(taskScheduler, times(1)).schedule(any(Runnable.class), eq(future.atZone(ZoneId.systemDefault()).toInstant()));
        Notification persisted = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
    }

    @Test
    @DisplayName("recoverScheduledNotifications() — 이미 시각이 지난 예약은 즉시 PENDING 전환 및 이벤트 발행")
    void movesToPending_whenScheduledTimeHasPassedOnRecover() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(30);
        Notification n = Notification.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SCHEDULED)
                .referenceId(11_003L)
                .referenceType("RECOVER_PAST")
                .idempotencyKey("recover-past-" + System.nanoTime())
                .retryCount(0)
                .maxRetryCount(3)
                .scheduledAt(past)
                .deleted(false)
                .build();
        Notification saved = notificationJpaRepository.save(n);

        notificationFacade.recoverScheduledNotifications(LocalDateTime.now());

        verify(notificationService, times(1)).scheduledToPending(saved.getId());
        Notification persisted = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("sendScheduledNotifications() — 만기된 SCHEDULED 알림을 PENDING으로 전환한다")
    void promotesDueScheduledToPending_whenSendScheduledNotifications() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        Notification n = Notification.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SCHEDULED)
                .referenceId(11_004L)
                .referenceType("POLL_DUE")
                .idempotencyKey("poll-due-" + System.nanoTime())
                .retryCount(0)
                .maxRetryCount(3)
                .scheduledAt(past)
                .deleted(false)
                .build();
        Notification saved = notificationJpaRepository.save(n);

        notificationFacade.sendScheduledNotifications();

        Notification persisted = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(notificationEventListener, atLeastOnce()).handleNotificationCreated(any());
    }

    @Test
    @DisplayName("처리 불가 상태(SENT)에서는 SendLog가 저장되지 않는다")
    void doesNotSaveSendLog_whenNotificationAlreadySent() {
        // arrange
        Notification notification = saveNotification(NotificationStatus.SENT);

        // act
        notificationFacade.sendNotification(notification.getId());

        // assert
        List<NotificationSendLog> logs = notificationSendLogJpaRepository.findAll();
        assertThat(logs).isEmpty();
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
