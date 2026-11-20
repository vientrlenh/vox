package com.sep.vox.application.port.input.usecase.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.notification.NotificationDtoMapper;
import com.sep.vox.application.port.input.query.ViewMyNotificationQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.repository.NotificationRepository;

/**
 * Đọc đúng một thông báo của chính người đang đăng nhập.
 *
 * <p>Sinh ra cho luồng bấm vào thông báo đẩy: lúc đó client chỉ cầm mỗi id trên URL, chưa
 * có danh sách nào trong tay, nên phải hỏi lại server payload để biết mở màn hình nào.
 *
 * <p>Lọc theo {@code userId} ngay trong truy vấn chứ không kiểm sau khi lấy: hai cách đều
 * chặn được người này đọc thông báo của người kia, nhưng cách này còn không phân biệt được
 * "id không tồn tại" với "id của người khác" -- kẻ dò id không học được gì từ phản hồi.
 */
@Service
public class ViewMyNotificationUseCase implements IUseCase<ViewMyNotificationQuery, NotificationDto>{

    private final NotificationRepository notificationRepository;
    private final UserContextPort userContextPort;

    public ViewMyNotificationUseCase(
            NotificationRepository notificationRepository,
            UserContextPort userContextPort) {
        this.notificationRepository = notificationRepository;
        this.userContextPort = userContextPort;
    }

    @Transactional(readOnly = true)
    public NotificationDto execute(ViewMyNotificationQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        return notificationRepository.findByIdAndUserId(input.id(), userId)
            .map(NotificationDtoMapper::toNotificationDto)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
    }
}
