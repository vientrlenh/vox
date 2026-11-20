package com.sep.vox.application.port.input.usecase.notification;

import java.time.Instant;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
        CreateNotificationDeviceCommand command = normalize(input);
        Instant now = Instant.now();
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        
        notificationDeviceRepository.deleteByUserIdAndDeviceIdAndExceptInstallationId(userId, command.deviceId(), command.installationId());
        notificationDeviceRepository.registerDevice(userId, command.deviceId(), command.platform(), command.installationId(), now);
        
        return null;
    }

    private CreateNotificationDeviceCommand normalize(CreateNotificationDeviceCommand input) {
        return new CreateNotificationDeviceCommand(
            StringNormalization.trimAndCollapseSpaces(input.deviceId()), 
            input.platform(), 
            StringNormalization.trimAndCollapseSpaces(input.installationId())
        );
    }
}
