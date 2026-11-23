package com.sep.vox.application.port.input.usecase.notification;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.NotificationDeviceRepository;

@Service
@RequiredArgsConstructor
public class DeleteNotificationDeviceUseCase implements IUseCase<String, Void> {

    private final NotificationDeviceRepository notificationDeviceRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(String input) {
        String installationId = StringNormalization.trimAndCollapseSpaces(input);
        if (installationId == null || installationId.isEmpty()) {
            throw new IllegalArgumentException("FID không được để trống");
        }

        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        int removed = notificationDeviceRepository.deleteByUserIdAndInstallationId(userId, installationId);
        if (removed == 0) {
            throw new ResourceNotFoundException("Không tìm thấy thiết bị nhận thông báo");
        }
        return null;
    }
}
