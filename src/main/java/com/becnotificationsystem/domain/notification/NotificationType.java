package com.becnotificationsystem.domain.notification;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    ENROLLMENT_COMPLETE("수강신청 완료", "{강의명} 수강신청이 완료되었습니다."),
    PAYMENT_CONFIRMED("결제 완료", "{강의명} 결제가 완료되었습니다."),
    LECTURE_REMINDER("강의 시작 알림", "{강의명} 강의가 {시간} 후 시작됩니다."),
    CANCELLATION("수강 취소", "{강의명} 수강신청이 취소되었습니다.");

    private final String title;
    private final String bodyTemplate;

    public String render(Map<String, String> variables) {
        String body = bodyTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            body = body.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return body;
    }
}
