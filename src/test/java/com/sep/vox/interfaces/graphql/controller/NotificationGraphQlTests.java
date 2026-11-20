package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationUseCase;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationsCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.notification.ViewMyUnreadNotificationCountUseCase;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.common.CursorPage;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends notification queries through the real schema and asserts that {@code Instant} values leave as
 * {@code DateTime} text (ISO-8601 in UTC).
 */
@GraphQlControllerTest(NotificationController.class)
class NotificationGraphQlTests {

    private static final Instant READ_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-27T23:15:30Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewMyNotificationsCursorPageUseCase viewMyNotificationsCursorPageUseCase;
    @MockitoBean
    private ViewMyUnreadNotificationCountUseCase viewMyUnreadNotificationCountUseCase;
    @MockitoBean
    private ViewMyNotificationUseCase viewMyNotificationUseCase;

    @Test
    @WithMockUser
    void my_notification_should_return_timestamps_as_date_time() {
        when(viewMyNotificationUseCase.execute(any())).thenReturn(notification(READ_AT));

        graphQlTester.document("{ myNotification(id: \"%s\") { readAt createdAt } }".formatted(UUID.randomUUID()))
            .execute()
            .errors().verify()
            .path("myNotification.readAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("myNotification.createdAt").entity(String.class).isEqualTo("2026-07-27T23:15:30Z");
    }

    @Test
    @WithMockUser
    void my_notifications_should_return_null_read_at_for_an_unread_notification() {
        when(viewMyNotificationsCursorPageUseCase.execute(any()))
            .thenReturn(new CursorPage<>(List.of(notification(null)), null, false));

        graphQlTester.document("{ myNotifications { content { readAt createdAt } } }")
            .execute()
            .errors().verify()
            .path("myNotifications.content[0].readAt").valueIsNull()
            .path("myNotifications.content[0].createdAt").entity(String.class).isEqualTo("2026-07-27T23:15:30Z");
    }

    private NotificationDto notification(Instant readAt) {
        return new NotificationDto(UUID.randomUUID(), UUID.randomUUID(), null, "InvoicePaid", "Title", "Body",
            null, readAt, CREATED_AT);
    }
}
