package com.becnotificationsystem.infrastructure.notification;

import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("UPDATE Notification n SET n.status = :newStatus WHERE n.id = :id AND n.status = :expectedStatus")
    int compareAndSwap(@Param("id") Long id,
                       @Param("expectedStatus") NotificationStatus expectedStatus,
                       @Param("newStatus") NotificationStatus newStatus);

    Optional<Notification> findByIdAndDeletedFalse(Long id);

    Page<Notification> findByReceiverIdAndStatusAndDeletedFalse(
            Long receiverId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByReceiverIdAndDeletedFalse(Long receiverId, Pageable pageable);

    List<Notification> findByStatusAndDeletedFalse(NotificationStatus status);

    List<Notification> findByStatusAndUpdatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before);

    List<Notification> findByStatusAndCreatedAtBeforeAndDeletedFalse(NotificationStatus status, LocalDateTime before);

    List<Notification> findByStatusAndScheduledAtLessThanEqualAndDeletedFalse(
            NotificationStatus status, LocalDateTime scheduledAt);

}
