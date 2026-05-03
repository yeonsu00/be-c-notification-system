package com.becnotificationsystem.application.sendlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import com.becnotificationsystem.infrastructure.sendlog.NotificationSendLogJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationSendLogServiceIntegrationTest {

    @Autowired
    private NotificationSendLogService notificationSendLogService;

    @Autowired
    private NotificationSendLogJpaRepository notificationSendLogJpaRepository;

    @AfterEach
    void tearDown() {
        notificationSendLogJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("발송 로그를 저장하면 DB에 정상적으로 저장된다")
    void saveSendLog_savesToDatabase() {
        // arrange
        NotificationSendLog log = NotificationSendLog.of(1L, 1, SendLogResult.SUCCESS, null);

        // act
        notificationSendLogService.saveSendLog(log);

        // assert
        assertThat(notificationSendLogJpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("성공 로그 존재 여부를 확인하면 DB 데이터를 기준으로 결과를 반환한다")
    void existsSuccessLog_returnsTrue_whenSuccessLogExists() {
        // arrange
        Long notificationId = 1L;
        NotificationSendLog log = NotificationSendLog.of(notificationId, 1, SendLogResult.SUCCESS, null);
        notificationSendLogJpaRepository.save(log);

        // act
        boolean result = notificationSendLogService.existsSuccessLog(notificationId);

        // assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("성공 로그가 없으면 false를 반환한다")
    void existsSuccessLog_returnsFalse_whenNoSuccessLogExists() {
        // arrange
        Long notificationId = 1L;
        NotificationSendLog log = NotificationSendLog.of(notificationId, 1, SendLogResult.FAILURE, "error");
        notificationSendLogJpaRepository.save(log);

        // act
        boolean result = notificationSendLogService.existsSuccessLog(notificationId);

        // assert
        assertThat(result).isFalse();
    }
}
