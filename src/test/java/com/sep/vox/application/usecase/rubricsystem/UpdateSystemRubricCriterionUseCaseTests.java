package com.sep.vox.application.usecase.rubricsystem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricCriterionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSystemRubricCriterionUseCaseTests {

    private final UUID userId = UUID.randomUUID();
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
    private RubricCriterionExamplesNormalizer examplesNormalizer;

    @InjectMocks
    private UpdateSystemRubricCriterionUseCase useCase;

    private void stubDraftCriterion() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(rubricCriterionRepository.findById(criterionId)).thenReturn(Optional.of(RubricCriterion.builder()
            .id(criterionId).rubricVersionId(versionId).name("Criterion")
            .minScore(BigDecimal.ZERO).maxScore(BigDecimal.TEN).order(1).build()));
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder().id(rubricId).ownerType(RubricOwnerType.SYSTEM).build()));
    }

    @Test
    void execute_clearsDescriptionAndExamplesOnAnExplicitNull() {
        stubDraftCriterion();

        useCase.execute(new UpdateSystemRubricCriterionCommand(criterionId, "Criterion", null, true, null, true, null, null, null, null, null));

        verify(rubricCriterionRepository).updateCriterionAtomic(eq(criterionId), any(), any(), eq(null), eq(true),
            eq(null), eq(true), any(), any(), any(), any(), any());
    }

    @Test
    void execute_keepsDescriptionAndExamplesWhenNotProvidedAndKeepsABlankDescription() {
        stubDraftCriterion();

        useCase.execute(new UpdateSystemRubricCriterionCommand(criterionId, "Criterion", null, false, null, false, null, null, null, null, null));
        useCase.execute(new UpdateSystemRubricCriterionCommand(criterionId, "Criterion", "  ", true, null, false, null, null, null, null, null));

        verify(rubricCriterionRepository, org.mockito.Mockito.times(2)).updateCriterionAtomic(eq(criterionId), any(), any(), eq(null), eq(false),
            eq(null), eq(false), any(), any(), any(), any(), any());
    }

    @Test
    void execute_throwsIllegalArgumentWhenTheRepositoryReportsADataConflict() {
        stubDraftCriterion();
        doThrow(new DataConflictException("unique violation", null)).when(rubricCriterionRepository)
            .updateCriterionAtomic(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any(), any(), any());
        UpdateSystemRubricCriterionCommand command = new UpdateSystemRubricCriterionCommand(criterionId, "Criterion", null, false, null, false, null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot update the criterion because the data is duplicated or invalid.");
    }
}
