package com.becnotificationsystem.global.common.response;

import com.becnotificationsystem.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CommonApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    @Builder
    private CommonApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonApiResponse<T> success(T data) {
        return CommonApiResponse.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> CommonApiResponse<T> fail(ErrorCode errorCode) {
        return CommonApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> CommonApiResponse<T> fail(String code, String message) {
        return CommonApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
