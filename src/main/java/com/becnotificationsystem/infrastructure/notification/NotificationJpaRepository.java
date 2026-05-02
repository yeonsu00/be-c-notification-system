package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.domain.notification.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndDeletedFalse(Long id);

}
