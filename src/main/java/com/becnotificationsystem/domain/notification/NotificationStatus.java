package com.becnotificationsystem.domain.notification;

public enum NotificationStatus {
    SCHEDULED,
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD_LETTER,
    READ;

    public static boolean isPending(NotificationStatus status) {
        return status == NotificationStatus.PENDING;
    }

    public static boolean isScheduled(NotificationStatus status) {
        return status == NotificationStatus.SCHEDULED;
    }
}
