package com.sep.vox.application.port.input.usecase.devicesession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RegisterPushTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

@Service
public class RegisterPushTokenUseCase implements IUseCase<RegisterPushTokenCommand, Void> {

    private final DeviceSessionRepository deviceSessionRepository;
    private final UserContextPort userContextPort;

    public RegisterPushTokenUseCase(DeviceSessionRepository deviceSessionRepository, UserContextPort userContextPort) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(RegisterPushTokenCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        deviceSessionRepository.updatePushToken(currentUserId, input.deviceId(), input.pushToken());
        return null;
    }
}
