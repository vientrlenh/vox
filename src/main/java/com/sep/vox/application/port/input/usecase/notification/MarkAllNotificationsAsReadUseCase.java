package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class MarkAllNotificationsAsReadUseCase implements IUseCase<Void, Void> {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(Void input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        Instant now = Instant.now();
        notificationRepository.markAllRead(userId, now);
        return null;
    }
    
}
