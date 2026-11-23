package com.sep.vox.application.usecase.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.UpdateProfileCommand;
import com.sep.vox.application.port.input.usecase.user.UpdateProfileUseCase;
import com.sep.vox.application.port.output.AvatarConfigPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateProfileUseCaseTests {

    @Mock
    private UserContextPort userContextPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvatarConfigPort avatarConfigPort;

    @InjectMocks
    private UpdateProfileUseCase useCase;

    @Test
    void execute_throwsDuplicatedWhenTheRepositoryReportsADataConflict() {
        UUID callerId = UUID.randomUUID();
        User caller = User.builder().id(callerId).status(UserStatus.ACTIVE).build();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdForUpdate(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataConflictException("unique violation", null));
        UpdateProfileCommand command = new UpdateProfileCommand(
            null, false, "0987654321", true, null, false, null, false, null, false);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessage("Số điện thoại đã tồn tại");
    }
}
