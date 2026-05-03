package com.becnotificationsystem.interfaces.api.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class NotificationControllerE2ETest extends IntegrationTest {

    private static final String ENDPOINT = "/api/v1/notifications";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long persistInAppSent(long receiverId, long referenceId, String idempotencyKey) {
        Notification saved = notificationRepository.save(Notification.of(
                receiverId,
                NotificationType.ENROLLMENT_COMPLETE,
                NotificationChannel.IN_APP,
                referenceId,
                "E2E_READ",
                idempotencyKey,
                null
        ));
        jdbcTemplate.update(
                "UPDATE notification SET status = ?, sent_at = ? WHERE id = ?",
                NotificationStatus.SENT.name(),
                LocalDateTime.now(),
                saved.getId()
        );
        return saved.getId();
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

    @DisplayName("POST /api/v1/notifications — 예약 발송 검증 (MockMvc)")
    @Nested
    class RegisterSchedule {

        @DisplayName("scheduledAt이 미래이면 202이고 응답 status는 SCHEDULED이다.")
        @Test
        void returns202WithScheduledStatus_whenScheduledAtIsFuture() throws Exception {
            NotificationCreateRequest body = new NotificationCreateRequest(
                    1L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.IN_APP,
                    20_001L,
                    "E2E_SCHEDULE",
                    LocalDateTime.now().plusHours(2)
            );

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        }

        @DisplayName("scheduledAt이 현재 이전이면 400과 검증 메시지를 반환한다.")
        @Test
        void returns400_whenScheduledAtIsNotFuture() throws Exception {
            NotificationCreateRequest body = new NotificationCreateRequest(
                    1L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.IN_APP,
                    20_002L,
                    "E2E_PAST_SCHEDULE",
                    LocalDateTime.now().minusMinutes(1)
            );

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @DisplayName("PATCH /api/v1/notifications/{id}/read (MockMvc)")
    @Nested
    class MarkAsRead {

        @DisplayName("IN_APP SENT이고 X-User-Id가 수신자와 일치하면 200과 READ를 반환한다.")
        @Test
        void returns200_whenInAppSentAndReceiverMatches() throws Exception {
            Long id = persistInAppSent(10L, 30_001L, "read-e2e-1");

            mockMvc.perform(patch(ENDPOINT + "/" + id + "/read")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("READ"))
                    .andExpect(jsonPath("$.data.readAt").exists());
        }

        @DisplayName("이미 READ인 경우에도 200으로 멱등 응답한다.")
        @Test
        void returns200_whenAlreadyRead() throws Exception {
            Long id = persistInAppSent(10L, 30_002L, "read-e2e-idem");
            LocalDateTime existingReadAt = LocalDateTime.now().minusDays(1);
            jdbcTemplate.update(
                    "UPDATE notification SET status = ?, read_at = ? WHERE id = ?",
                    NotificationStatus.READ.name(),
                    existingReadAt,
                    id
            );

            mockMvc.perform(patch(ENDPOINT + "/" + id + "/read")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("READ"));

            mockMvc.perform(patch(ENDPOINT + "/" + id + "/read")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("READ"));
        }

        @DisplayName("EMAIL 채널이면 400 NOTIFICATION_CHANNEL_NOT_SUPPORTED이다.")
        @Test
        void returns400_whenChannelIsEmail() throws Exception {
            Notification saved = notificationRepository.save(Notification.of(
                    10L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.EMAIL,
                    30_003L,
                    "READ_EMAIL",
                    "read-e2e-email",
                    null
            ));
            jdbcTemplate.update(
                    "UPDATE notification SET status = ?, sent_at = ? WHERE id = ?",
                    NotificationStatus.SENT.name(),
                    LocalDateTime.now(),
                    saved.getId()
            );

            mockMvc.perform(patch(ENDPOINT + "/" + saved.getId() + "/read")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_CHANNEL_NOT_SUPPORTED"));
        }

        @DisplayName("수신자가 아니면 403 NOTIFICATION_ACCESS_DENIED이다.")
        @Test
        void returns403_whenUserIsNotReceiverForRead() throws Exception {
            Long id = persistInAppSent(10L, 30_004L, "read-e2e-403");

            mockMvc.perform(patch(ENDPOINT + "/" + id + "/read")
                            .header("X-User-Id", "99"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_ACCESS_DENIED"));
        }
    }

    @DisplayName("POST /api/v1/notifications/{id}/retry (MockMvc)")
    @Nested
    class ManualRetry {

        @DisplayName("DEAD_LETTER이고 수신자 본인이면 200과 PENDING을 반환한다.")
        @Test
        void returns200_whenDeadLetterAndReceiverMatches() throws Exception {
            Notification saved = notificationRepository.save(Notification.builder()
                    .receiverId(10L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.DEAD_LETTER)
                    .referenceId(40_001L)
                    .referenceType("RETRY_E2E")
                    .idempotencyKey("retry-e2e-1")
                    .retryCount(3)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build());

            MvcResult result = mockMvc.perform(post(ENDPOINT + "/" + saved.getId() + "/retry")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(root.get("data").get("retryCount").asInt()).isEqualTo(3);
        }

        @DisplayName("DEAD_LETTER가 아니면 400 NOTIFICATION_RETRY_NOT_ALLOWED이다.")
        @Test
        void returns400_whenNotDeadLetter() throws Exception {
            Notification saved = notificationRepository.save(Notification.of(
                    10L,
                    NotificationType.ENROLLMENT_COMPLETE,
                    NotificationChannel.IN_APP,
                    40_002L,
                    "RETRY_PENDING",
                    "retry-e2e-pending",
                    null
            ));

            mockMvc.perform(post(ENDPOINT + "/" + saved.getId() + "/retry")
                            .header("X-User-Id", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_RETRY_NOT_ALLOWED"));
        }

        @DisplayName("수신자가 아니면 403 NOTIFICATION_ACCESS_DENIED이다.")
        @Test
        void returns403_whenUserIsNotReceiverForRetry() throws Exception {
            Notification saved = notificationRepository.save(Notification.builder()
                    .receiverId(10L)
                    .notificationType(NotificationType.ENROLLMENT_COMPLETE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.DEAD_LETTER)
                    .referenceId(40_003L)
                    .referenceType("RETRY_FORBIDDEN")
                    .idempotencyKey("retry-e2e-403")
                    .retryCount(3)
                    .maxRetryCount(3)
                    .deleted(false)
                    .build());

            mockMvc.perform(post(ENDPOINT + "/" + saved.getId() + "/retry")
                            .header("X-User-Id", "99"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_ACCESS_DENIED"));
        }
    }
}
