package com.becnotificationsystem.application.notification;

public record NotificationCreatedEvent(Long notificationId) {

    public static NotificationCreatedEvent of(Long notificationId) {
        return new NotificationCreatedEvent(notificationId);
    }
}
