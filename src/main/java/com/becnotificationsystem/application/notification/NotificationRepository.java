package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findByIdAndDeletedFalse(Long id);

    Page<Notification> findByReceiverIdAndStatusAndDeletedFalse(
            Long receiverId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByReceiverIdAndDeletedFalse(Long receiverId, Pageable pageable);

}
