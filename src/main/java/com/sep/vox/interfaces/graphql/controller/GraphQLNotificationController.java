package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.notification.ViewMyNotificationQuery;
import com.sep.vox.application.port.input.query.notification.ViewMyNotificationsCursorPageQuery;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationUseCase;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationsCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.notification.ViewMyUnreadNotificationCountUseCase;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.common.CursorPage;

@Controller
@RequiredArgsConstructor
public class GraphQLNotificationController {

    private final ViewMyNotificationsCursorPageUseCase viewMyNotificationsCursorPageUseCase;
    private final ViewMyUnreadNotificationCountUseCase viewMyUnreadNotificationCountUseCase;
    private final ViewMyNotificationUseCase viewMyNotificationUseCase;

    @QueryMapping(name = "myNotifications")
    @PreAuthorize("isAuthenticated()")
    public CursorPage<NotificationDto> myNotifications(
            @Argument(name = "cursor") UUID cursor,
            @Argument(name = "limit") Integer limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Invalid limit on the number of items to fetch");
        }
        return viewMyNotificationsCursorPageUseCase.execute(
            new ViewMyNotificationsCursorPageQuery(cursor, limit));
    }

    @QueryMapping(name = "myUnreadNotificationCount")
    @PreAuthorize("isAuthenticated()")
    public long myUnreadNotificationCount() {
        return viewMyUnreadNotificationCountUseCase.execute();
    }

    @QueryMapping(name = "myNotification")
    @PreAuthorize("isAuthenticated()")
    public NotificationDto myNotification(@Argument(name = "id") UUID id) {
        return viewMyNotificationUseCase.execute(new ViewMyNotificationQuery(id));
    }
}
