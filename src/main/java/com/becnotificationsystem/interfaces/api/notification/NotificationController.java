package com.becnotificationsystem.interfaces.api.notification;

import com.becnotificationsystem.application.notification.NotificationInfo;
import com.becnotificationsystem.application.notification.NotificationInfo.ListItem;
import com.becnotificationsystem.application.notification.NotificationService;
import com.becnotificationsystem.global.common.response.CommonApiResponse;
import com.becnotificationsystem.global.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommonApiResponse<NotificationInfo.Detail> register(
            @Valid @RequestBody NotificationCreateRequest request) {
        return CommonApiResponse.success(notificationService.register(request), "알림 발송 요청이 접수되었습니다.");
    }

    @GetMapping("/{notificationId}")
    public CommonApiResponse<NotificationInfo.Detail> getStatus(
            @PathVariable Long notificationId) {
        return CommonApiResponse.success(notificationService.findById(notificationId), "조회 성공");
    }

    @GetMapping
    public CommonApiResponse<PageResponse<ListItem>> getList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Boolean readFilter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return CommonApiResponse.success(notificationService.findByReceiverId(userId, readFilter, pageable), "조회 성공");
    }
}
