package com.becnotificationsystem.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    DUPLICATE_NOTIFICATION(409, "DUPLICATE_NOTIFICATION", "이미 처리된 알림 요청입니다."),
    INVALID_CHANNEL(400, "INVALID_CHANNEL", "지원하지 않는 발송 채널입니다."),
    INVALID_NOTIFICATION_TYPE(400, "INVALID_NOTIFICATION_TYPE", "지원하지 않는 알림 타입입니다.");

    private final int status;
    private final String code;
    private final String message;
}
