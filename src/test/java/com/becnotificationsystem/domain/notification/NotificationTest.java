package com.becnotificationsystem.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

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
}
