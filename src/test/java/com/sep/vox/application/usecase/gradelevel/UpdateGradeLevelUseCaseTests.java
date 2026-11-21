package com.sep.vox.application.usecase.gradelevel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.gradelevel.UpdateGradeLevelUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateGradeLevelUseCaseTests {

    @Mock
    private GradeLevelRepository gradeLevelRepository;

    @Mock
    private UserContextPort userContextPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateGradeLevelUseCase useCase;

    @Test
    void execute_throwsDuplicatedWhenTheRepositoryReportsADataConflict() {
        UUID adminId = UUID.randomUUID();
        UUID gradeLevelId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(userRepository.existsByIdAndStatus(adminId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(gradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(GradeLevel.builder().id(gradeLevelId).build()));
        when(gradeLevelRepository.updateGradeLevelAtomic(eq(gradeLevelId), any(), any(), any(), any(Instant.class), any()))
            .thenThrow(new DataConflictException("unique violation", null));
        UpdateGradeLevelCommand command = new UpdateGradeLevelCommand(gradeLevelId, "Grade 10", "Tenth grade", 10);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(DuplicatedException.class)
            .hasMessage("Thứ tự khối học đã được sử dụng.");
    }
}
