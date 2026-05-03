package com.becnotificationsystem.domain.notification;

public enum NotificationChannel {
    EMAIL,
    IN_APP;

    public static boolean isEmail(NotificationChannel channel) {
        return channel == EMAIL;
    }
}
