package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.MarkNotificationAsReadCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
public class MarkNotificationAsReadUseCase implements IUseCase<MarkNotificationAsReadCommand, UUID> {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    public MarkNotificationAsReadUseCase(NotificationRepository notificationRepository, UserContextPort userContextPort) {
        this.notificationRepository = notificationRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(MarkNotificationAsReadCommand input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var now = Instant.now();

        var notification = notificationRepository.findByIdAndUserId(input.id(), userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        var _ = notificationRepository.markRead(userId, input.id(), now);

        return notification.getId();
    }
    
}
