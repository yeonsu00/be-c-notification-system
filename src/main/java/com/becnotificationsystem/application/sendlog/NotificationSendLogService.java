package com.becnotificationsystem.application.sendlog;

import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSendLogService {

    private final NotificationSendLogRepository notificationSendLogRepository;

    public void saveSendLog(NotificationSendLog log) {
        notificationSendLogRepository.save(log);
    }

    public boolean existsSuccessLog(Long notificationId) {
        return notificationSendLogRepository.existsByNotificationIdAndResult(notificationId, SendLogResult.SUCCESS);
    }
}
