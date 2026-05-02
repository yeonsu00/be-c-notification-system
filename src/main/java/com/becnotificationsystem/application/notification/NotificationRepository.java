package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findByIdAndDeletedFalse(Long id);

    Page<Notification> findByReceiverIdAndStatusAndDeletedFalse(
            Long receiverId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByReceiverIdAndDeletedFalse(Long receiverId, Pageable pageable);

    int compareAndSwap(Long id, NotificationStatus expectedStatus, NotificationStatus newStatus);

    List<Notification> findByStatusAndDeletedFalse(NotificationStatus status);

    List<Notification> findByStatusAndUpdatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before);

    List<Notification> findByStatusAndCreatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before);

}
