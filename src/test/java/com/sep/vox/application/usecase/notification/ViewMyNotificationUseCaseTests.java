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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewMyNotificationQuery;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.repository.NotificationRepository;

/**
 * Truy vấn phục vụ route chuyển hướng {@code /n/{id}}: client bấm vào thông báo đẩy chỉ
 * cầm mỗi id, phải hỏi lại payload mới biết mở màn hình nào.
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
        var payload = "{\"eventType\":\"ExamResultReleased\",\"target\":\"EXAM_RESULT_DETAIL\"}";
        when(notificationRepository.findByIdAndUserId(notificationId, userId))
            .thenReturn(Optional.of(new Notification(
                notificationId, userId, UUID.randomUUID(), "ExamResultReleased",
                "Điểm thi của bạn đã có", "Kỳ thi giữa kỳ: 8.5 điểm", payload,
                null, Instant.parse("2026-09-01T03:00:00Z"))));

        var result = useCase.execute(new ViewMyNotificationQuery(notificationId));

        assertThat(result.id()).isEqualTo(notificationId);
        assertThat(result.payload()).isEqualTo(payload);
    }

    /**
     * Lọc theo userId nằm TRONG truy vấn, không phải một phép kiểm sau khi lấy: nhờ vậy
     * thông báo của người khác và id không tồn tại cho ra cùng một phản hồi, và người dò id
     * không suy ra được thông báo nào có thật.
     */
    @Test
    void should_scope_the_lookup_to_the_current_user() {
        when(notificationRepository.findByIdAndUserId(notificationId, userId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewMyNotificationQuery(notificationId)))
            .isInstanceOf(NotFoundException.class);

        verify(notificationRepository).findByIdAndUserId(notificationId, userId);
    }
}
