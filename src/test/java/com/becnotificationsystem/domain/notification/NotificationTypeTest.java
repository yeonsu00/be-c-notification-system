package com.becnotificationsystem.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    @DisplayName("변수 Map을 전달하면 {변수명}이 치환된다")
    void replacesVariables_whenMapGiven() {
        // arrange
        NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
        Map<String, String> variables = Map.of("강의명", "자바 정복");

        // act
        String result = type.render(variables);

        // assert
        assertThat(result).isEqualTo("자바 정복 수강신청이 완료되었습니다.");
    }

    @Test
    @DisplayName("빈 Map을 전달하면 원문 그대로 반환된다")
    void returnsOriginalTemplate_whenEmptyMapGiven() {
        // arrange
        NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
        Map<String, String> variables = Map.of();

        // act
        String result = type.render(variables);

        // assert
        assertThat(result).isEqualTo("{강의명} 수강신청이 완료되었습니다.");
    }

    @Test
    @DisplayName("여러 변수가 있을 때 모두 치환된다 (LECTURE_REMINDER의 {강의명}, {시간})")
    void replacesAllVariables_whenMultipleVariablesGiven() {
        // arrange
        NotificationType type = NotificationType.LECTURE_REMINDER;
        Map<String, String> variables = Map.of(
                "강의명", "스프링 마스터",
                "시간", "10분"
        );

        // act
        String result = type.render(variables);

        // assert
        assertThat(result).isEqualTo("스프링 마스터 강의가 10분 후 시작됩니다.");
    }

    @Test
    @DisplayName("Map에 없는 키는 치환되지 않고 원문 유지된다")
    void keepsUnknownVariables_whenKeyNotInMap() {
        // arrange
        NotificationType type = NotificationType.LECTURE_REMINDER;
        Map<String, String> variables = Map.of("강의명", "스프링 마스터");

        // act
        String result = type.render(variables);

        // assert
        assertThat(result).isEqualTo("스프링 마스터 강의가 {시간} 후 시작됩니다.");
    }

    @Test
    @DisplayName("템플릿에 변수가 없으면 그대로 반환된다")
    void returnsTemplateAsIs_whenNoVariablesInTemplate() {
        // arrange
        NotificationType type = NotificationType.ENROLLMENT_COMPLETE;
        Map<String, String> variables = Map.of("unknown", "value");

        // act
        String result = type.render(variables);

        // assert
        assertThat(result).isEqualTo("{강의명} 수강신청이 완료되었습니다.");
    }
}
