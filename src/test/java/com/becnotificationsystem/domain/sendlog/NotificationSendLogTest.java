package com.becnotificationsystem.domain.sendlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationSendLogTest {

    @Test
    @DisplayName("SUCCESS result로 생성하면 모든 필드가 올바르게 설정된다")
    void createsSendLog_whenResultIsSuccess() {
        // arrange
        Long notificationId = 1L;
        int attemptNumber = 1;
        SendLogResult result = SendLogResult.SUCCESS;

        // act
        NotificationSendLog log = NotificationSendLog.of(notificationId, attemptNumber, result, null);

        // assert
        assertAll(
                () -> assertThat(log.getNotificationId()).isEqualTo(notificationId),
                () -> assertThat(log.getAttemptNumber()).isEqualTo(attemptNumber),
                () -> assertThat(log.getResult()).isEqualTo(result),
                () -> assertThat(log.getFailureReason()).isNull(),
                () -> assertThat(log.getAttemptedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("FAILURE result로 생성하면 failureReason이 설정된다")
    void createsSendLog_whenResultIsFailure() {
        // arrange
        Long notificationId = 1L;
        int attemptNumber = 1;
        SendLogResult result = SendLogResult.FAILURE;
        String failureReason = "SMTP Error";

        // act
        NotificationSendLog log = NotificationSendLog.of(notificationId, attemptNumber, result, failureReason);

        // assert
        assertAll(
                () -> assertThat(log.getNotificationId()).isEqualTo(notificationId),
                () -> assertThat(log.getAttemptNumber()).isEqualTo(attemptNumber),
                () -> assertThat(log.getResult()).isEqualTo(result),
                () -> assertThat(log.getFailureReason()).isEqualTo(failureReason),
                () -> assertThat(log.getAttemptedAt()).isNotNull()
        );
    }
}
