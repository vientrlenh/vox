package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewMyNotificationsCursorPageQuery;
import com.sep.vox.application.port.input.usecase.notification.ViewMyNotificationsCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.notification.ViewMyUnreadNotificationCountUseCase;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.common.CursorPage;

@Controller("graphqlNotificationController")
public class NotificationController {

    private final ViewMyNotificationsCursorPageUseCase viewMyNotificationsCursorPageUseCase;
    private final ViewMyUnreadNotificationCountUseCase viewMyUnreadNotificationCountUseCase;

    public NotificationController(
            ViewMyNotificationsCursorPageUseCase viewMyNotificationsCursorPageUseCase,
            ViewMyUnreadNotificationCountUseCase viewMyUnreadNotificationCountUseCase) {
        this.viewMyNotificationsCursorPageUseCase = viewMyNotificationsCursorPageUseCase;
        this.viewMyUnreadNotificationCountUseCase = viewMyUnreadNotificationCountUseCase;
    }

    @QueryMapping(name = "myNotifications")
    @PreAuthorize("isAuthenticated()")
    public CursorPage<NotificationDto> myNotifications(
            @Argument(name = "cursor") UUID cursor,
            @Argument(name = "limit") Integer limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Giới hạn số phần tử lấy lên không hợp lệ");
        }
        return viewMyNotificationsCursorPageUseCase.execute(
            new ViewMyNotificationsCursorPageQuery(cursor, limit));
    }

    @QueryMapping(name = "myUnreadNotificationCount")
    @PreAuthorize("isAuthenticated()")
    public long myUnreadNotificationCount() {
        return viewMyUnreadNotificationCountUseCase.execute();
    }
}
