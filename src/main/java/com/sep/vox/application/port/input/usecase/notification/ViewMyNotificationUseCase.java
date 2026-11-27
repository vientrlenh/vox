package com.sep.vox.application.port.input.usecase.notification;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.notification.NotificationDtoMapper;
import com.sep.vox.application.port.input.query.notification.ViewMyNotificationQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.repository.NotificationRepository;

/**
 * Reads exactly ONE notification of the logged-in user.
 *
 * <p>Made for the tap-a-push-notification flow: at that moment the client only holds the id from the URL and has no
 * list yet, so it must ask the server for the payload to know which screen to open.
 *
 * <p>Filters by {@code userId} inside the query instead of checking after the fetch: both ways stop one user reading
 * another's notification, but this one also cannot tell "the id does not exist" from "the id belongs to someone
 * else" -- someone probing ids learns nothing from the response.
 */
@Service
@RequiredArgsConstructor
public class ViewMyNotificationUseCase implements IUseCase<ViewMyNotificationQuery, NotificationDto>{

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    @Transactional(readOnly = true)
    public NotificationDto execute(ViewMyNotificationQuery input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        return notificationRepository.findByIdAndUserId(input.id(), userId)
            .map(NotificationDtoMapper::toNotificationDto)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }
}
