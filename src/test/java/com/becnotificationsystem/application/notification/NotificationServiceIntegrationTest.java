package com.becnotificationsystem.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.global.common.response.PageResponse;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import com.becnotificationsystem.infrastructure.notification.EmailNotificationSender;
import com.becnotificationsystem.infrastructure.notification.InAppNotificationSender;
import com.becnotificationsystem.infrastructure.notification.NotificationJpaRepository;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import com.becnotificationsystem.interfaces.listener.NotificationEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockitoSpyBean
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @MockitoBean
    private NotificationEventListener notificationEventListener;

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

    @DisplayName("알림을 등록할 때,")
    @Nested
    class Register {

        @DisplayName("유효한 요청이 주어지면 PENDING 상태의 알림이 저장되고 Detail이 반환된다.")
        @Test
        void savesNotification_whenRequestIsValid() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.EMAIL,
                    100L,
                    "ORDER",
                    null
            );

            // act
            NotificationInfo.Detail result = notificationService.register(request);

            // assert
            assertAll(
                    () -> assertThat(result.notificationId()).isNotNull(),
                    () -> assertThat(result.status()).isEqualTo(NotificationStatus.PENDING),
                    () -> assertThat(result.receiverId()).isEqualTo(1L),
                    () -> assertThat(result.notificationType()).isEqualTo(NotificationType.ENROLLMENT_COMPLETE),
                    () -> assertThat(result.channel()).isEqualTo(NotificationChannel.EMAIL),
                    () -> assertThat(result.referenceId()).isEqualTo(100L),
                    () -> assertThat(result.referenceType()).isEqualTo("ORDER"),
                    () -> assertThat(result.retryCount()).isEqualTo(0),
                    () -> assertThat(result.createdAt()).isNotNull()
            );

            // verify
            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @DisplayName("동일한 요청이 중복으로 들어오면 NOTIFICATION_DUPLICATE 예외가 발생한다.")
        @Test
        void throwsException_whenDuplicateRequestGiven() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.EMAIL,
                    100L,
                    "ORDER",
                    null
            );
            notificationService.register(request);

            // act & assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    notificationService.register(request)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_DUPLICATE);
        }
    }

    @DisplayName("알림을 ID로 조회할 때,")
    @Nested
    class FindById {

        @DisplayName("존재하는 알림 ID로 조회하면 알림 상세 정보가 반환된다.")
        @Test
        void returnsDetail_whenNotificationExists() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L,
                    NotificationType.PAYMENT_CONFIRMED,
                    NotificationChannel.IN_APP,
                    200L,
                    "PAYMENT",
                    null
            );
            NotificationInfo.Detail saved = notificationService.register(request);

            // act
            NotificationInfo.Detail result = notificationService.findById(saved.notificationId());

            // assert
            assertAll(
                    () -> assertThat(result.notificationId()).isEqualTo(saved.notificationId()),
                    () -> assertThat(result.receiverId()).isEqualTo(1L),
                    () -> assertThat(result.notificationType()).isEqualTo(NotificationType.PAYMENT_CONFIRMED),
                    () -> assertThat(result.channel()).isEqualTo(NotificationChannel.IN_APP),
                    () -> assertThat(result.status()).isEqualTo(NotificationStatus.PENDING)
            );
        }

        @DisplayName("존재하지 않는 알림 ID로 조회하면 NOTIFICATION_NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenNotificationDoesNotExist() {
            // arrange
            Long nonExistentId = 999L;

            // act & assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    notificationService.findById(nonExistentId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @DisplayName("soft-delete된 알림 ID로 조회하면 NOTIFICATION_NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenNotificationIsSoftDeleted() {
            // arrange
            Notification deleted = Notification.builder()
                    .receiverId(1L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .referenceId(1L)
                    .referenceType("ORDER")
                    .idempotencyKey("deleted-key")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(true)
                    .build();
            Notification saved = notificationJpaRepository.save(deleted);

            // act & assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    notificationService.findById(saved.getId())
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @DisplayName("수신자 ID로 알림 목록을 조회할 때,")
    @Nested
    class FindByReceiverId {

        @DisplayName("readFilter가 null이면 수신자의 삭제되지 않은 모든 알림 목록을 반환한다.")
        @Test
        void returnsAllNotifications_whenReadFilterIsNull() {
            // arrange
            Long receiverId = 1L;
            notificationService.register(new NotificationCreateRequest(receiverId, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 1L, "ORDER", null));
            notificationService.register(new NotificationCreateRequest(receiverId, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 2L, "PAYMENT", null));

            // act
            PageResponse<NotificationInfo.ListItem> result = notificationService.findByReceiverId(receiverId, null, PageRequest.of(0, 20));

            // assert
            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content()).allSatisfy(item -> assertThat(item.message()).isNotEmpty());
        }

        @DisplayName("readFilter가 false이면 SENT 상태의 알림만 반환한다.")
        @Test
        void returnsSentNotificationsOnly_whenReadFilterIsFalse() {
            // arrange
            Long receiverId = 1L;
            Notification sentNotification = Notification.builder()
                    .receiverId(receiverId)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.SENT)
                    .referenceId(1L)
                    .referenceType("ORDER")
                    .idempotencyKey("sent-key")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            notificationJpaRepository.save(sentNotification);
            notificationService.register(new NotificationCreateRequest(receiverId, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 2L, "PAYMENT", null));

            // act
            PageResponse<NotificationInfo.ListItem> result = notificationService.findByReceiverId(receiverId, false, PageRequest.of(0, 20));

            // assert
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).status()).isEqualTo(NotificationStatus.SENT);
        }

        @DisplayName("readFilter가 true이면 READ 상태의 알림만 반환한다.")
        @Test
        void returnsReadNotificationsOnly_whenReadFilterIsTrue() {
            // arrange
            Long receiverId = 1L;
            Notification readNotification = Notification.builder()
                    .receiverId(receiverId)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.READ)
                    .referenceId(1L)
                    .referenceType("ORDER")
                    .idempotencyKey("read-key")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            notificationJpaRepository.save(readNotification);
            notificationService.register(new NotificationCreateRequest(receiverId, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 2L, "PAYMENT", null));

            // act
            PageResponse<NotificationInfo.ListItem> result = notificationService.findByReceiverId(receiverId, true, PageRequest.of(0, 20));

            // assert
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).status()).isEqualTo(NotificationStatus.READ);
        }

        @DisplayName("알림이 없는 수신자로 조회하면 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoNotificationsExist() {
            // arrange
            Long receiverId = 999L;

            // act
            PageResponse<NotificationInfo.ListItem> result = notificationService.findByReceiverId(receiverId, null, PageRequest.of(0, 20));

            // assert
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
        }

        @DisplayName("다른 수신자의 알림은 결과에 포함되지 않는다.")
        @Test
        void returnsOnlyTargetReceiverNotifications() {
            // arrange
            notificationService.register(new NotificationCreateRequest(1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 1L, "ORDER", null));
            notificationService.register(new NotificationCreateRequest(2L, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 2L, "PAYMENT", null));

            // act
            PageResponse<NotificationInfo.ListItem> result = notificationService.findByReceiverId(1L, null, PageRequest.of(0, 20));

            // assert
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).notificationType()).isEqualTo(NotificationType.ENROLLMENT_COMPLETE);
        }
    }

    @DisplayName("알림을 발송할 때,")
    @Nested
    class SendNotification {

        @DisplayName("PENDING 상태의 EMAIL 알림을 발송하면 SENT 상태가 되고 sentAt이 설정된다.")
        @Test
        void transitionsToSent_whenEmailNotificationIsPending() {
            // arrange
            Notification saved = savePendingNotification(NotificationChannel.EMAIL);

            // act
            notificationService.sendNotification(saved.getId());

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
            notificationService.sendNotification(saved.getId());

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
            notificationService.sendNotification(saved.getId());

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
            notificationService.sendNotification(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
        }

        @DisplayName("이미 SENT 상태인 알림에 sendNotification()을 호출해도 상태가 변경되지 않는다.")
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
            notificationService.sendNotification(saved.getId());

            // assert
            Notification result = notificationJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        }

        @DisplayName("이미 PROCESSING 상태인 알림에 sendNotification()을 호출해도 상태가 변경되지 않는다.")
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
            notificationService.sendNotification(saved.getId());

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
                    notificationService.sendNotification(nonExistentId)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
