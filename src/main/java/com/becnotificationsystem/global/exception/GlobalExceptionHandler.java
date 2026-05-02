package com.becnotificationsystem.global.exception;

import com.becnotificationsystem.global.common.response.CommonApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonApiResponse.fail(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .badRequest()
                .body(CommonApiResponse.fail("INVALID_INPUT", message));
    }

    @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
    public ResponseEntity<CommonApiResponse<Void>> handleBindingException(org.springframework.web.bind.ServletRequestBindingException e) {
        log.warn("BindingException: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(CommonApiResponse.fail("INVALID_INPUT", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(CommonApiResponse.fail("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
