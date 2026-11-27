package com.sep.vox.application.usecase.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.notification.ViewMyNotificationQuery;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.repository.NotificationRepository;

/**
 * The query that serves the redirect route {@code /n/{id}}: a client tapping a push notification only holds the id,
 * and has to ask for the payload to know which screen to open.
 */
class ViewMyNotificationUseCaseTests {

    private NotificationRepository notificationRepository;
    private UserContextPort userContextPort;
    private ViewMyNotificationUseCase useCase;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewMyNotificationUseCase(notificationRepository, userContextPort);

        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
    }

    @Test
    void should_return_the_notification_with_its_navigation_payload() {
        String payload = "{\"eventType\":\"AssessmentResultReleased\",\"target\":\"ASSESSMENT_RESULT_DETAIL\"}";
        when(notificationRepository.findByIdAndUserId(notificationId, userId))
            .thenReturn(Optional.of(Notification.builder()
                    .id(notificationId)
                    .userId(userId)
                    .eventId(UUID.randomUUID())
                    .eventType("AssessmentResultReleased")
                    .title("Điểm thi của bạn đã có")
                    .body("Kỳ thi giữa kỳ: 8.5 điểm")
                    .payload(payload)
                    .readAt(null)
                    .createdAt(Instant.parse("2026-09-01T03:00:00Z"))
                    .build()));

        NotificationDto result = useCase.execute(new ViewMyNotificationQuery(notificationId));

        assertThat(result.id()).isEqualTo(notificationId);
        assertThat(result.payload()).isEqualTo(payload);
    }

    /**
     * Filtering by userId lives INSIDE the query, not as a check after fetching: so another user's notification and
     * an id that does not exist give the same response, and whoever probes ids cannot infer which notifications are
     * real.
     */
    @Test
    void should_scope_the_lookup_to_the_current_user() {
        when(notificationRepository.findByIdAndUserId(notificationId, userId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewMyNotificationQuery(notificationId)))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationRepository).findByIdAndUserId(notificationId, userId);
    }
}
