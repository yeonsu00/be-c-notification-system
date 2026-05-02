package com.becnotificationsystem.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "SUCCESS", "OK");

    private final int status;
    private final String code;
    private final String message;
}
