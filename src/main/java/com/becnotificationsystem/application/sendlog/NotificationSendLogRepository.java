package com.becnotificationsystem.application.sendlog;

import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;

public interface NotificationSendLogRepository {

    void save(NotificationSendLog log);

    boolean existsByNotificationIdAndResult(Long notificationId, SendLogResult result);
}
