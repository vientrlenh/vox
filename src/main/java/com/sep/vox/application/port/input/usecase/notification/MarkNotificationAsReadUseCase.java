package com.sep.vox.application.port.input.usecase.notification;

import com.sep.vox.domain.model.notification.Notification;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.MarkNotificationAsReadCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class MarkNotificationAsReadUseCase implements IUseCase<MarkNotificationAsReadCommand, UUID> {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(MarkNotificationAsReadCommand input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        Instant now = Instant.now();

        Notification notification = notificationRepository.findByIdAndUserId(input.id(), userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        notificationRepository.markRead(userId, input.id(), now);

        return notification.getId();
    }
    
}
