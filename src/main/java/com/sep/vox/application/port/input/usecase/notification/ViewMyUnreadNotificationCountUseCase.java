package com.sep.vox.application.port.input.usecase.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class ViewMyUnreadNotificationCountUseCase {

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    @Transactional(readOnly = true)
    public long execute() {
        return notificationRepository.countUnreadByUserId(userContextPort.getCurrentAuthenticatedUserId());
    }
}
