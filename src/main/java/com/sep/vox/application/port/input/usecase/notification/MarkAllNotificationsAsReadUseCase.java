package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var now = Instant.now();
        var _ = notificationRepository.markAllRead(userId, now);
        return null;
    }
    
}
