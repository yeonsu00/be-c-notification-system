package com.becnotificationsystem.interfaces.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.becnotificationsystem.application.notification.NotificationFacade;
import com.becnotificationsystem.application.notification.NotificationInfo;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.infrastructure.notification.NotificationJpaRepository;
import com.becnotificationsystem.interfaces.api.notification.NotificationCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationEventListenerIntegrationTest {

    @Autowired
    private NotificationFacade notificationFacade;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @DisplayName("NotificationCreatedEvent를 수신했을 때,")
    @Nested
    class HandleNotificationCreated {

        @DisplayName("트랜잭션 커밋 후 별도 스레드에서 발송 처리가 실행되어 알림이 SENT 상태가 된다.")
        @Test
        void processesNotificationAsync_afterTransactionCommit() throws InterruptedException {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL,
                    100L, "ORDER", null
            );

            // act
            NotificationInfo.Detail result = notificationFacade.register(request);

            // assert — 비동기 발송 처리 완료 대기 (최대 3초, 100ms 간격 폴링)
            NotificationStatus status = NotificationStatus.PENDING;
            long deadline = System.currentTimeMillis() + 3_000;
            while (System.currentTimeMillis() < deadline) {
                status = notificationJpaRepository.findById(result.notificationId()).orElseThrow().getStatus();
                if (status == NotificationStatus.SENT) break;
                Thread.sleep(100);
            }
            assertThat(status).isEqualTo(NotificationStatus.SENT);
        }
    }
}
