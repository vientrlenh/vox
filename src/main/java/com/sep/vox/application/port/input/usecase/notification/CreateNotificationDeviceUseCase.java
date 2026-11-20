package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.repository.NotificationDeviceRepository;

@Service
public class CreateNotificationDeviceUseCase implements IUseCase<CreateNotificationDeviceCommand, Void> {
    
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final UserContextPort userContextPort;

    public CreateNotificationDeviceUseCase(NotificationDeviceRepository notificationDeviceRepository, UserContextPort userContextPort) {
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(CreateNotificationDeviceCommand input) {
        var command = normalize(input);
        var now = Instant.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        
        NotificationDevicePlatform platform;
        try {
            platform = NotificationDevicePlatform.valueOf(command.platform());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nền tảng " + command.platform() + " không được hỗ trợ");
        }

        notificationDeviceRepository.deleteByUserIdAndDeviceIdAndExceptInstallationId(userId, command.deviceId(), command.installationId());
        notificationDeviceRepository.registerDevice(userId, command.deviceId(), platform, command.installationId(), now);
        
        return null;
    }

    private CreateNotificationDeviceCommand normalize(CreateNotificationDeviceCommand input) {
        return new CreateNotificationDeviceCommand(
            StringNormalization.trimAndCollapseSpaces(input.deviceId()), 
            StringNormalization.normalizeCode(input.platform()), 
            StringNormalization.trimAndCollapseSpaces(input.installationId())
        );
    }
}
