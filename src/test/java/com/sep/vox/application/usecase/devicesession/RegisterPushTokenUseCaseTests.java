package com.sep.vox.application.usecase.devicesession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.RegisterPushTokenCommand;
import com.sep.vox.application.port.input.usecase.devicesession.RegisterPushTokenUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

class RegisterPushTokenUseCaseTests {

    private DeviceSessionRepository deviceSessionRepository;
    private UserContextPort userContextPort;
    private RegisterPushTokenUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new RegisterPushTokenUseCase(deviceSessionRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
    }

    @Test
    void should_update_push_token_for_current_user_and_device() {
        useCase.execute(new RegisterPushTokenCommand("device-1", "fcm-token-abc"));

        verify(deviceSessionRepository).updatePushToken(userId, "device-1", "fcm-token-abc");
    }
}
