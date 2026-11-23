package com.sep.vox.application.usecase.gradelevel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.CreateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.gradelevel.CreateGradeLevelUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CreateGradeLevelUseCaseTests {

    @Mock
    private GradeLevelRepository gradeLevelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContextPort userContextPort;

    @InjectMocks
    private CreateGradeLevelUseCase useCase;

    @Test
    void execute_throwsDuplicatedWhenTheRepositoryReportsADataConflict() {
        UUID adminId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(userRepository.existsByIdAndStatus(adminId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(gradeLevelRepository.save(any(GradeLevel.class))).thenThrow(new DataConflictException("unique violation", null));
        CreateGradeLevelCommand command = new CreateGradeLevelCommand("grade10", "Grade 10", "Tenth grade", 10);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessage("Mã hoặc thứ tự khối học đã tồn tại.");
    }
}
