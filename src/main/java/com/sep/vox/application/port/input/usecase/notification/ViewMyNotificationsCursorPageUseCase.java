package com.sep.vox.application.port.input.usecase.notification;

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
public class ViewMyNotificationsCursorPageUseCase
        implements IUseCase<ViewMyNotificationsCursorPageQuery, CursorPage<NotificationDto>> {

    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    public ViewMyNotificationsCursorPageUseCase(
            NotificationRepository notificationRepository,
            UserContextPort userContextPort) {
        this.notificationRepository = notificationRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<NotificationDto> execute(ViewMyNotificationsCursorPageQuery input) {
        var limit = Math.min(input.limit(), MAX_LIMIT);
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var results = input.cursor() == null
            ? notificationRepository.findByUserIdOrderByIdDesc(userId, limit)
            : notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, input.cursor(), limit);

        var hasNext = results.size() > limit;
        var page = hasNext ? results.subList(0, limit) : results;
        var nextCursor = hasNext ? page.getLast().getId() : null;

        return new CursorPage<>(
            NotificationDtoMapper.toNotificationDtoList(page),
            nextCursor,
            hasNext
        );
    }
}
