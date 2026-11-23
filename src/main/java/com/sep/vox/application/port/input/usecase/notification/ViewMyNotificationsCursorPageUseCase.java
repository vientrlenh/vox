package com.sep.vox.application.port.input.usecase.notification;

import com.sep.vox.domain.model.notification.Notification;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.notification.NotificationDtoMapper;
import com.sep.vox.application.port.input.query.ViewMyNotificationsCursorPageQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.common.CursorPage;
import com.sep.vox.domain.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class ViewMyNotificationsCursorPageUseCase
        implements IUseCase<ViewMyNotificationsCursorPageQuery, CursorPage<NotificationDto>> {

    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<NotificationDto> execute(ViewMyNotificationsCursorPageQuery input) {
        int limit = Math.min(input.limit(), MAX_LIMIT);
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        List<Notification> results = input.cursor() == null
            ? notificationRepository.findByUserIdOrderByIdDesc(userId, limit)
            : notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, input.cursor(), limit);

        boolean hasNext = results.size() > limit;
        List<Notification> page = hasNext ? results.subList(0, limit) : results;
        UUID nextCursor = hasNext ? page.getLast().getId() : null;

        return new CursorPage<>(
            NotificationDtoMapper.toNotificationDtoList(page),
            nextCursor,
            hasNext
        );
    }
}
