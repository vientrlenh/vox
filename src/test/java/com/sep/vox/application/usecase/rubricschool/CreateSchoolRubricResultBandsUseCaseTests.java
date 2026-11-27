package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CreateSchoolRubricResultBandsUseCaseTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();

    @Mock
    private RubricResultBandRepository rubricResultBandRepository;

    @Mock
    private RubricVersionRepository rubricVersionRepository;

    @Mock
    private RubricRepository rubricRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContextPort userContextPort;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolUserRepository schoolUserRepository;

    @InjectMocks
    private CreateSchoolRubricResultBandsUseCase useCase;

    @Test
    void execute_throwsIllegalStateWhenTheRepositoryReportsADataConflict() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).status(UserStatus.ACTIVE).build()));
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder()
            .id(rubricId).ownerType(RubricOwnerType.SCHOOL).schoolId(schoolId).build()));
        when(schoolUserRepository.findByUserId(userId))
            .thenReturn(Optional.of(SchoolUser.builder().schoolId(schoolId).userId(userId).build()));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(School.builder().active(true).build()));
        when(rubricResultBandRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
        when(rubricResultBandRepository.saveAll(any())).thenThrow(new DataConflictException("unique violation", null));
        CreateSchoolRubricResultBandsCommand command = new CreateSchoolRubricResultBandsCommand(schoolId, versionId, List.of(new CreateSchoolRubricResultBandsCommand.ResultBandItemCommand("BAND", "Band", null, BigDecimal.ZERO, BigDecimal.TEN, 1)));

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Data save error: the result band code (Code) or order already exists in this Rubric version. Please check again.");
    }
}
