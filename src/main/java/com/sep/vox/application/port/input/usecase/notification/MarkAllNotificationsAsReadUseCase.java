package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
public class MarkAllNotificationsAsReadUseCase implements IUseCase<Void, Void> {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    public MarkAllNotificationsAsReadUseCase(NotificationRepository notificationRepository, UserContextPort userContextPort) {
        this.notificationRepository = notificationRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(Void input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        var now = Instant.now();
        var _ = notificationRepository.markAllRead(userId, now);
        return null;
    }
    
}
