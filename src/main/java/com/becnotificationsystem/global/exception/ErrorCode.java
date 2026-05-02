package com.becnotificationsystem.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOTIFICATION_DUPLICATE(409, "NOTIFICATION_DUPLICATE", "동일한 이벤트에 대한 알림이 이미 존재합니다."),
    NOTIFICATION_CHANNEL_NOT_SUPPORTED(400, "NOTIFICATION_CHANNEL_NOT_SUPPORTED", "해당 채널에서 지원하지 않는 동작입니다."),
    NOTIFICATION_RETRY_NOT_ALLOWED(400, "NOTIFICATION_RETRY_NOT_ALLOWED", "재시도 불가 상태의 알림입니다."),
    NOTIFICATION_ACCESS_DENIED(403, "NOTIFICATION_ACCESS_DENIED", "수신자 본인만 처리할 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;
}
