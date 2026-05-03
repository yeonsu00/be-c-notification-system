package com.becnotificationsystem.infrastructure.sendlog;

import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSendLogJpaRepository extends JpaRepository<NotificationSendLog, Long> {

    boolean existsByNotificationIdAndResult(Long notificationId, SendLogResult result);
}
