package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
}
