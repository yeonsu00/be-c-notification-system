package com.becnotificationsystem.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import com.becnotificationsystem.infrastructure.notification.EmailNotificationSender;
import com.becnotificationsystem.infrastructure.notification.InAppNotificationSender;
import com.becnotificationsystem.infrastructure.notification.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class NotificationSendServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @MockitoSpyBean
    private EmailNotificationSender emailNotificationSender;

    @MockitoSpyBean
    private InAppNotificationSender inAppNotificationSender;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    private Notification savePendingNotification(NotificationChannel channel) {
        Notification notification = Notification.of(
                1L, NotificationType.ENROLLMENT_COMPLETE, channel,
                100L, "ORDER",
                Notification.generateIdempotencyKey(1L, NotificationType.ENROLLMENT_COMPLETE, 100L, "ORDER", channel),
                null
        );
        return notificationJpaRepository.save(notification);
    }

    private Notification saveFailedNotification(int retryCount) {
        Notification notification = Notification.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.FAILED)
                .referenceId(100L)
                .referenceType("ORDER")
                .idempotencyKey("failed-key-" + retryCount)
                .retryCount(retryCount)
                .maxRetryCount(3)
                .deleted(false)
                .build();
        return notificationJpaRepository.save(notification);
    }

    @DisplayName("알림을 발송할 때,")
    @Nested
    class Process {

        @DisplayName("PENDING 상태의 EMAIL 알림을 발송하면 SENT 상태가 되고 sentAt이 설정된다.")
        @Test
        void transitionsToSent_whenEmailNotificationIsPending() {
            // arrange
            Notification saved = savePendingNotification(NotificationChannel.EMAIL);

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertAll(
                    () -> assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT),
                    () -> assertThat(result.getSentAt()).isNotNull()
            );
        }

        @DisplayName("PENDING 상태의 IN_APP 알림을 발송하면 SENT 상태가 된다.")
        @Test
        void transitionsToSent_whenInAppNotificationIsPending() {
            // arrange
            Notification saved = savePendingNotification(NotificationChannel.IN_APP);

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        }

        @DisplayName("발송 중 예외가 발생하고 재시도 가능하면 FAILED 상태가 되고 retryCount가 증가한다.")
        @Test
        void transitionsToFailed_whenSendThrowsAndCanRetry() {
            // arrange
            Notification saved = savePendingNotification(NotificationChannel.EMAIL);
            doThrow(new RuntimeException("SMTP error")).when(emailNotificationSender).send(any());

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertAll(
                    () -> assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED),
                    () -> assertThat(result.getRetryCount()).isEqualTo(1),
                    () -> assertThat(result.getFailureReason()).contains("SMTP error")
            );
        }

        @DisplayName("재시도 횟수가 maxRetryCount에 도달하면 DEAD_LETTER 상태가 된다.")
        @Test
        void transitionsToDeadLetter_whenRetryCountReachesMax() {
            // arrange
            Notification saved = saveFailedNotification(2); // maxRetryCount=3, retryCount=2 → 다음 실패 시 3 >= 3
            doThrow(new RuntimeException("SMTP error")).when(emailNotificationSender).send(any());

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
        }

        @DisplayName("이미 SENT 상태인 알림에 process()를 호출해도 상태가 변경되지 않는다.")
        @Test
        void doesNotChangeStatus_whenAlreadySent() {
            // arrange
            Notification notification = Notification.builder()
                    .receiverId(1L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.SENT)
                    .referenceId(100L)
                    .referenceType("ORDER")
                    .idempotencyKey("sent-key")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            Notification saved = notificationJpaRepository.save(notification);

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        }

        @DisplayName("이미 PROCESSING 상태인 알림에 process()를 호출해도 상태가 변경되지 않는다.")
        @Test
        void doesNotChangeStatus_whenAlreadyProcessing() {
            // arrange
            Notification notification = Notification.builder()
                    .receiverId(1L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PROCESSING)
                    .referenceId(100L)
                    .referenceType("ORDER")
                    .idempotencyKey("processing-key")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            Notification saved = notificationJpaRepository.save(notification);

            // act
            notificationService.process(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        }

        @DisplayName("존재하지 않는 notificationId로 호출하면 NOTIFICATION_NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenNotificationDoesNotExist() {
            // arrange
            Long nonExistentId = 999L;

            // act & assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    notificationService.process(nonExistentId)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
