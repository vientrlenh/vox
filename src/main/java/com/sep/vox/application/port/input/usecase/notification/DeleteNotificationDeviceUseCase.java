package com.sep.vox.application.port.input.usecase.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationDeviceRepository;

@Service
public class DeleteNotificationDeviceUseCase implements IUseCase<String, Void> {

    private final NotificationDeviceRepository notificationDeviceRepository;
    private final UserContextPort userContextPort;

    public DeleteNotificationDeviceUseCase(
            NotificationDeviceRepository notificationDeviceRepository,
            UserContextPort userContextPort) {
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(String input) {
        var installationId = StringNormalization.trimAndCollapseSpaces(input);
        if (installationId == null || installationId.isEmpty()) {
            throw new IllegalArgumentException("FID không được để trống");
        }

        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var removed = notificationDeviceRepository.deleteByUserIdAndInstallationId(userId, installationId);
        if (removed == 0) {
            throw new NotFoundException("Không tìm thấy thiết bị nhận thông báo");
        }
        return null;
    }
}
