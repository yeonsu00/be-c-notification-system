package com.becnotificationsystem.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @DisplayName("Notification 객체를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 올바르게 주어지면 PENDING 상태로 생성된다.")
        @Test
        void createsNotification_whenAllFieldsAreValid() {
            // arrange
            Long receiverId = 1L;
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            NotificationChannel channel = NotificationChannel.EMAIL;
            Long referenceId = 100L;
            String referenceType = "ORDER";
            String idempotencyKey = "test-key";

            // act
            Notification notification = Notification.of(receiverId, type, channel, referenceId, referenceType, idempotencyKey, null);

            // assert
            assertAll(
                    () -> assertThat(notification.getReceiverId()).isEqualTo(receiverId),
                    () -> assertThat(notification.getNotificationType()).isEqualTo(type),
                    () -> assertThat(notification.getChannel()).isEqualTo(channel),
                    () -> assertThat(notification.getReferenceId()).isEqualTo(referenceId),
                    () -> assertThat(notification.getReferenceType()).isEqualTo(referenceType),
                    () -> assertThat(notification.getIdempotencyKey()).isEqualTo(idempotencyKey),
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING),
                    () -> assertThat(notification.getRetryCount()).isEqualTo(0),
                    () -> assertThat(notification.getMaxRetryCount()).isEqualTo(3),
                    () -> assertThat(notification.isDeleted()).isFalse(),
                    () -> assertThat(notification.getScheduledAt()).isNull()
            );
        }

        @DisplayName("scheduledAt이 null이어도 정상적으로 생성된다.")
        @Test
        void createsNotification_whenScheduledAtIsNull() {
            // arrange & act
            Notification notification = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    100L, "ORDER", "key", null
            );

            // assert
            assertThat(notification.getScheduledAt()).isNull();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @DisplayName("scheduledAt이 현재 시각보다 미래이면 SCHEDULED 상태로 생성된다.")
        @Test
        void createsScheduledNotification_whenScheduledAtIsInFuture() {
            // arrange
            LocalDateTime future = LocalDateTime.now().plusDays(1);

            // act
            Notification notification = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    100L, "ORDER", "scheduled-key", future
            );

            // assert
            assertAll(
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SCHEDULED),
                    () -> assertThat(notification.getScheduledAt()).isEqualTo(future)
            );
        }

        @DisplayName("scheduledAt이 현재 시각 이전이면 PENDING 상태로 생성된다.")
        @Test
        void createsPendingNotification_whenScheduledAtIsInPast() {
            // arrange
            LocalDateTime past = LocalDateTime.now().minusMinutes(1);

            // act
            Notification notification = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    100L, "ORDER", "past-schedule-key", past
            );

            // assert
            assertAll(
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING),
                    () -> assertThat(notification.getScheduledAt()).isEqualTo(past)
            );
        }
    }

    @DisplayName("멱등성 키를 생성할 때,")
    @Nested
    class GenerateIdempotencyKey {

        @DisplayName("동일한 파라미터로 호출하면 항상 같은 키를 반환한다.")
        @Test
        void returnsSameKey_whenSameParametersGiven() {
            // arrange
            Long receiverId = 1L;
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            Long referenceId = 100L;
            String referenceType = "ORDER";
            NotificationChannel channel = NotificationChannel.EMAIL;

            // act
            String key1 = Notification.generateIdempotencyKey(receiverId, type, referenceId, referenceType, channel);
            String key2 = Notification.generateIdempotencyKey(receiverId, type, referenceId, referenceType, channel);

            // assert
            assertThat(key1).isEqualTo(key2);
        }

        @DisplayName("receiverId가 다르면 다른 키를 반환한다.")
        @Test
        void returnsDifferentKey_whenReceiverIdDiffers() {
            // arrange
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            Long referenceId = 100L;
            String referenceType = "ORDER";
            NotificationChannel channel = NotificationChannel.EMAIL;

            // act
            String key1 = Notification.generateIdempotencyKey(1L, type, referenceId, referenceType, channel);
            String key2 = Notification.generateIdempotencyKey(2L, type, referenceId, referenceType, channel);

            // assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @DisplayName("notificationType이 다르면 다른 키를 반환한다.")
        @Test
        void returnsDifferentKey_whenNotificationTypeDiffers() {
            // arrange
            Long receiverId = 1L;
            Long referenceId = 100L;
            String referenceType = "ORDER";
            NotificationChannel channel = NotificationChannel.EMAIL;

            // act
            String key1 = Notification.generateIdempotencyKey(receiverId, NotificationType.ENROLLMENT_COMPLETE, referenceId, referenceType, channel);
            String key2 = Notification.generateIdempotencyKey(receiverId, NotificationType.PAYMENT_CONFIRMED, referenceId, referenceType, channel);

            // assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @DisplayName("referenceId가 다르면 다른 키를 반환한다.")
        @Test
        void returnsDifferentKey_whenReferenceIdDiffers() {
            // arrange
            Long receiverId = 1L;
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            String referenceType = "ORDER";
            NotificationChannel channel = NotificationChannel.EMAIL;

            // act
            String key1 = Notification.generateIdempotencyKey(receiverId, type, 100L, referenceType, channel);
            String key2 = Notification.generateIdempotencyKey(receiverId, type, 200L, referenceType, channel);

            // assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @DisplayName("referenceType이 다르면 다른 키를 반환한다.")
        @Test
        void returnsDifferentKey_whenReferenceTypeDiffers() {
            // arrange
            Long receiverId = 1L;
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            Long referenceId = 100L;
            NotificationChannel channel = NotificationChannel.EMAIL;

            // act
            String key1 = Notification.generateIdempotencyKey(receiverId, type, referenceId, "ORDER", channel);
            String key2 = Notification.generateIdempotencyKey(receiverId, type, referenceId, "PAYMENT", channel);

            // assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @DisplayName("channel이 다르면 다른 키를 반환한다.")
        @Test
        void returnsDifferentKey_whenChannelDiffers() {
            // arrange
            Long receiverId = 1L;
            NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
            Long referenceId = 100L;
            String referenceType = "ORDER";

            // act
            String key1 = Notification.generateIdempotencyKey(receiverId, type, referenceId, referenceType, NotificationChannel.EMAIL);
            String key2 = Notification.generateIdempotencyKey(receiverId, type, referenceId, referenceType, NotificationChannel.IN_APP);

            // assert
            assertThat(key1).isNotEqualTo(key2);
        }
    }

    @DisplayName("상태 전이 메서드를 호출할 때,")
    @Nested
    class StatusTransition {

        private Notification createPendingNotification() {
            return Notification.of(1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL,
                    100L, "ORDER", "test-key", null);
        }

        @DisplayName("startProcessing()을 호출하면 PROCESSING 상태로 전이된다.")
        @Test
        void transitionsToProcessing_whenStartProcessingCalled() {
            // arrange
            Notification notification = createPendingNotification();

            // act
            notification.startProcessing();

            // assert
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        }

        @DisplayName("markAsSent()을 호출하면 SENT 상태가 되고 sentAt이 설정된다.")
        @Test
        void transitionsToSent_whenMarkAsSentCalled() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();
            LocalDateTime before = LocalDateTime.now();

            // act
            notification.markAsSent();

            // assert
            assertAll(
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT),
                    () -> assertThat(notification.getSentAt()).isNotNull(),
                    () -> assertThat(notification.getSentAt()).isAfterOrEqualTo(before)
            );
        }

        @DisplayName("markAsFailed()을 호출하면 FAILED 상태가 되고 retryCount가 증가하며 failureReason이 설정된다.")
        @Test
        void transitionsToFailed_whenMarkAsFailedCalled() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();

            // act
            notification.markAsFailed("connection timeout");

            // assert
            assertAll(
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED),
                    () -> assertThat(notification.getRetryCount()).isEqualTo(1),
                    () -> assertThat(notification.getFailureReason()).isEqualTo("connection timeout")
            );
        }

        @DisplayName("markAsDeadLetter()을 호출하면 DEAD_LETTER 상태로 전이된다.")
        @Test
        void transitionsToDeadLetter_whenMarkAsDeadLetterCalled() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();
            notification.markAsFailed("error");

            // act
            notification.markAsDeadLetter();

            // assert
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
        }

        @DisplayName("canRetry()는 retryCount가 maxRetryCount보다 작으면 true를 반환한다.")
        @Test
        void returnsTrue_whenRetryCountLessThanMax() {
            // arrange
            Notification notification = createPendingNotification();

            // act & assert
            assertThat(notification.canRetry()).isTrue();
        }

        @DisplayName("canRetry()는 retryCount가 maxRetryCount 이상이면 false를 반환한다.")
        @Test
        void returnsFalse_whenRetryCountReachesMax() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();
            notification.markAsFailed("err");
            notification.startProcessing();
            notification.markAsFailed("err");
            notification.startProcessing();
            notification.markAsFailed("err");

            // act & assert
            assertThat(notification.canRetry()).isFalse();
        }

        @DisplayName("resetToPending()을 호출하면 PENDING 상태로 복구된다.")
        @Test
        void resetsToPending_whenResetToPendingCalled() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();

            // act
            notification.resetToPending();

            // assert
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @DisplayName("markAsPending()을 호출하면 SCHEDULED에서 PENDING으로 전이된다.")
        @Test
        void transitionsToPending_whenMarkAsPendingCalledFromScheduled() {
            // arrange
            LocalDateTime future = LocalDateTime.now().plusHours(1);
            Notification notification = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    100L, "ORDER", "sched-to-pending", future
            );

            // act
            notification.markAsPending();

            // assert
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }

        @DisplayName("markAsRead()을 호출하면 READ 상태가 되고 readAt이 설정된다.")
        @Test
        void transitionsToRead_whenMarkAsReadCalled() {
            // arrange
            Notification notification = createPendingNotification();
            notification.startProcessing();
            notification.markAsSent();
            LocalDateTime readAt = LocalDateTime.now().plusSeconds(1);

            // act
            notification.markAsRead(readAt);

            // assert
            assertAll(
                    () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ),
                    () -> assertThat(notification.getReadAt()).isEqualTo(readAt)
            );
        }
    }

    @DisplayName("matchesReceiver()를 호출할 때,")
    @Nested
    class MatchesReceiver {

        @DisplayName("동일한 receiverId이면 true를 반환한다.")
        @Test
        void returnsTrue_whenReceiverIdMatches() {
            // arrange
            Notification notification = Notification.of(
                    42L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    1L, "ORDER", "match-key", null
            );

            // act & assert
            assertThat(notification.matchesReceiver(42L)).isTrue();
        }

        @DisplayName("다른 receiverId이면 false를 반환한다.")
        @Test
        void returnsFalse_whenReceiverIdDiffers() {
            // arrange
            Notification notification = Notification.of(
                    42L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    1L, "ORDER", "mismatch-key", null
            );

            // act & assert
            assertThat(notification.matchesReceiver(99L)).isFalse();
        }

        @DisplayName("null을 넘기면 false를 반환한다.")
        @Test
        void returnsFalse_whenReceiverIdIsNull() {
            // arrange
            Notification notification = Notification.of(
                    42L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    1L, "ORDER", "null-receiver-key", null
            );

            // act & assert
            assertThat(notification.matchesReceiver(null)).isFalse();
        }
    }

    @DisplayName("isProcessable()을 호출할 때,")
    @Nested
    class IsProcessable {

        @DisplayName("PENDING이면 true를 반환한다.")
        @Test
        void returnsTrue_whenPending() {
            Notification n = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL,
                    1L, "ORDER", "proc-pending", null
            );
            assertThat(n.isProcessable()).isTrue();
        }

        @DisplayName("FAILED이면 true를 반환한다.")
        @Test
        void returnsTrue_whenFailed() {
            Notification n = Notification.builder()
                    .receiverId(1L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.FAILED)
                    .referenceId(1L)
                    .referenceType("ORDER")
                    .idempotencyKey("proc-failed")
                    .retryCount(1)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            assertThat(n.isProcessable()).isTrue();
        }

        @DisplayName("SCHEDULED이면 false를 반환한다.")
        @Test
        void returnsFalse_whenScheduled() {
            Notification n = Notification.of(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.IN_APP,
                    1L, "ORDER", "proc-scheduled", LocalDateTime.now().plusHours(1)
            );
            assertThat(n.isProcessable()).isFalse();
        }

        @DisplayName("SENT이면 false를 반환한다.")
        @Test
        void returnsFalse_whenSent() {
            Notification n = Notification.builder()
                    .receiverId(1L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.IN_APP)
                    .status(NotificationStatus.SENT)
                    .referenceId(1L)
                    .referenceType("ORDER")
                    .idempotencyKey("proc-sent")
                    .retryCount(0)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build();
            assertThat(n.isProcessable()).isFalse();
        }
    }
}
