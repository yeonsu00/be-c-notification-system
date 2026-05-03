package com.becnotificationsystem.infrastructure.sendlog;

import com.becnotificationsystem.application.sendlog.NotificationSendLogRepository;
import com.becnotificationsystem.domain.sendlog.NotificationSendLog;
import com.becnotificationsystem.domain.sendlog.SendLogResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationSendLogRepositoryImpl implements NotificationSendLogRepository {

    private final NotificationSendLogJpaRepository notificationSendLogJpaRepository;

    @Override
    public void save(NotificationSendLog log) {
        notificationSendLogJpaRepository.save(log);
    }

    @Override
    public boolean existsByNotificationIdAndResult(Long notificationId, SendLogResult result) {
        return notificationSendLogJpaRepository.existsByNotificationIdAndResult(notificationId, result);
    }
}
