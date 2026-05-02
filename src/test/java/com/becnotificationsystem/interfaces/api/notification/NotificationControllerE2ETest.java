package com.becnotificationsystem.interfaces.api.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.becnotificationsystem.application.notification.NotificationInfo;
import com.becnotificationsystem.application.notification.NotificationRepository;
import com.becnotificationsystem.domain.notification.Notification;
import com.becnotificationsystem.domain.notification.NotificationChannel;
import com.becnotificationsystem.domain.notification.NotificationStatus;
import com.becnotificationsystem.domain.notification.NotificationType;
import com.becnotificationsystem.global.common.response.CommonApiResponse;
import com.becnotificationsystem.global.common.response.PageResponse;
import com.becnotificationsystem.support.IntegrationTest;
import com.becnotificationsystem.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationControllerE2ETest extends IntegrationTest {

    private static final String ENDPOINT = "/api/v1/notifications";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/notifications - 알림 등록")
    @Nested
    class Register {

        @DisplayName("유효한 요청이 주어지면 202 Accepted와 알림 상세 정보를 반환한다.")
        @Test
        void returns202WithDetail_whenRequestIsValid() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 100L, "ORDER", null
            );

            // act
            ResponseEntity<CommonApiResponse<NotificationInfo.Detail>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("SUCCESS");

            NotificationInfo.Detail detail = response.getBody().getData();
            assertAll(
                    () -> assertThat(detail.notificationId()).isNotNull(),
                    () -> assertThat(detail.receiverId()).isEqualTo(1L),
                    () -> assertThat(detail.status()).isEqualTo(NotificationStatus.PENDING)
            );
        }

        @DisplayName("필수 필드가 누락되면 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenRequiredFieldIsMissing() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    null, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 100L, "ORDER", null
            );

            // act
            ResponseEntity<CommonApiResponse<Void>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("동일한 요청이 중복으로 들어오면 409 Conflict를 반환한다.")
        @Test
        void returns409_whenDuplicateRequestGiven() {
            // arrange
            NotificationCreateRequest request = new NotificationCreateRequest(
                    1L, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 100L, "ORDER", null
            );
            testRestTemplate.postForEntity(ENDPOINT, request, CommonApiResponse.class);

            // act
            ResponseEntity<CommonApiResponse<Void>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getCode()).isEqualTo("NOTIFICATION_DUPLICATE");
        }
    }

    @DisplayName("GET /api/v1/notifications/{notificationId} - 알림 상태 조회")
    @Nested
    class GetStatus {

        @DisplayName("존재하는 알림 ID로 조회하면 200 OK와 알림 상세 정보를 반환한다.")
        @Test
        void returns200WithDetail_whenNotificationExists() {
            // arrange
            Notification notification = Notification.of(
                    1L, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 200L, "PAYMENT", "key-1", null
            );
            Notification saved = notificationRepository.save(notification);

            // act
            ResponseEntity<CommonApiResponse<NotificationInfo.Detail>> response = testRestTemplate.exchange(
                    ENDPOINT + "/" + saved.getId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().notificationId()).isEqualTo(saved.getId());
        }

        @DisplayName("존재하지 않는 알림 ID로 조회하면 404 Not Found를 반환한다.")
        @Test
        void returns404_whenNotificationDoesNotExist() {
            // act
            ResponseEntity<CommonApiResponse<Void>> response = testRestTemplate.exchange(
                    ENDPOINT + "/9999",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
        }
    }

    @DisplayName("GET /api/v1/notifications - 알림 목록 조회")
    @Nested
    class GetList {

        @DisplayName("X-User-Id 헤더와 함께 요청하면 해당 수신자의 알림 목록을 반환한다.")
        @Test
        void returnsNotificationList_forUser() {
            // arrange
            Long userId = 1L;
            notificationRepository.save(Notification.of(userId, NotificationType.ENROLLMENT_COMPLETE, NotificationChannel.EMAIL, 1L, "ORDER", "key-1", null));
            notificationRepository.save(Notification.of(userId, NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP, 2L, "PAYMENT", "key-2", null));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());

            // act
            ResponseEntity<CommonApiResponse<PageResponse<NotificationInfo.ListItem>>> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().content()).hasSize(2);
        }

        @DisplayName("X-User-Id 헤더가 누락되면 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenUserIdHeaderIsMissing() {
            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
