package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.application.notification.NotificationRepository;
import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.global.exception.BusinessException;
import com.becnotificationsystem.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<Notification> findByReceiverIdAndStatusAndDeletedFalse(
            Long receiverId, NotificationStatus status, Pageable pageable) {
        return notificationJpaRepository.findByReceiverIdAndStatusAndDeletedFalse(receiverId, status, pageable);
    }

    @Override
    public Page<Notification> findByReceiverIdAndDeletedFalse(Long receiverId, Pageable pageable) {
        return notificationJpaRepository.findByReceiverIdAndDeletedFalse(receiverId, pageable);
    }

    @Override
    public int compareAndSwap(Long id, NotificationStatus expectedStatus, NotificationStatus newStatus) {
        return notificationJpaRepository.compareAndSwap(id, expectedStatus, newStatus);
    }

    @Override
    public List<Notification> findByStatusAndDeletedFalse(NotificationStatus status) {
        return notificationJpaRepository.findByStatusAndDeletedFalse(status);
    }

    @Override
    public List<Notification> findByStatusAndUpdatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before) {
        return notificationJpaRepository.findByStatusAndUpdatedAtBeforeAndDeletedFalse(status, before);
    }

    @Override
    public List<Notification> findByStatusAndCreatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before) {
        return notificationJpaRepository.findByStatusAndCreatedAtBeforeAndDeletedFalse(status, before);
    }
}
