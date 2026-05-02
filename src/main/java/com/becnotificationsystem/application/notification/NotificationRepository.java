package com.becnotificationsystem.application.notification;

import com.becnotificationsystem.domain.notification.Notification;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findByIdAndDeletedFalse(Long id);

}
