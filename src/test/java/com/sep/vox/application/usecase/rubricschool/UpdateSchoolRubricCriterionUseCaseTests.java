package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.rubricschool.UpdateSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSchoolRubricCriterionUseCaseTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();

    @Mock
    private RubricCriterionRepository rubricCriterionRepository;

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

    @Mock
    private RubricCriterionExamplesNormalizer examplesNormalizer;

    @InjectMocks
    private UpdateSchoolRubricCriterionUseCase useCase;

    @Test
    void execute_throwsIllegalArgumentWhenTheRepositoryReportsADataConflict() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(userId))
            .thenReturn(Optional.of(SchoolUser.builder().schoolId(schoolId).userId(userId).build()));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(School.builder().active(true).build()));
        when(rubricCriterionRepository.findById(criterionId)).thenReturn(Optional.of(RubricCriterion.builder()
            .id(criterionId).rubricVersionId(versionId).name("Criterion")
            .minScore(BigDecimal.ZERO).maxScore(BigDecimal.TEN).order(1).build()));
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder().id(rubricId).ownerType(RubricOwnerType.SCHOOL).schoolId(schoolId).build()));
        doThrow(new DataConflictException("unique violation", null)).when(rubricCriterionRepository)
            .updateCriterionAtomic(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        UpdateSchoolRubricCriterionCommand command = new UpdateSchoolRubricCriterionCommand(schoolId, criterionId, "Criterion", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Không thể cập nhật Tiêu chí do dữ liệu bị trùng hoặc không hợp lệ.");
    }
}
