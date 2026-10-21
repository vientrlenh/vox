package com.sep.vox.application.port.input.usecase.notification;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
public class ViewMyUnreadNotificationCountUseCase {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    public ViewMyUnreadNotificationCountUseCase(
            NotificationRepository notificationRepository,
            UserContextPort userContextPort) {
        this.notificationRepository = notificationRepository;
        this.userContextPort = userContextPort;
    }

    @Transactional(readOnly = true)
    public long execute() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        return notificationRepository.countUnreadByUserId(userId);
    }
}
