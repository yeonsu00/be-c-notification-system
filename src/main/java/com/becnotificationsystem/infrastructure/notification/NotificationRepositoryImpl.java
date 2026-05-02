package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.application.notification.NotificationRepository;
import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Notification save(Notification notification) {
        try {
            return notificationJpaRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.NOTIFICATION_DUPLICATE);
        }
    }

    @Override
    public Optional<Notification> findByIdAndDeletedFalse(Long id) {
        return notificationJpaRepository.findByIdAndDeletedFalse(id);
    }
}
