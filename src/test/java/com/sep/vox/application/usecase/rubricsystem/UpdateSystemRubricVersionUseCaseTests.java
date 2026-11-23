package com.sep.vox.application.usecase.rubricsystem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricVersionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSystemRubricVersionUseCaseTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();

    @Mock
    private RubricVersionRepository rubricVersionRepository;

    @Mock
    private RubricRepository rubricRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContextPort userContextPort;

    @Mock
    private RubricResultBandRepository rubricResultBandRepository;

    @Mock
    private RubricCriterionRepository rubricCriterionRepository;

    @InjectMocks
    private UpdateSystemRubricVersionUseCase useCase;

    @Test
    void execute_throwsIllegalArgumentWhenTheRepositoryReportsADataConflict() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(rubricResultBandRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
        when(rubricCriterionRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder().id(rubricId).ownerType(RubricOwnerType.SYSTEM).build()));
        doThrow(new DataConflictException("unique violation", null)).when(rubricVersionRepository)
            .updateRubricVersionAtomic(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        UpdateSystemRubricVersionCommand command = new UpdateSystemRubricVersionCommand(versionId, "Version", null, null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Không thể cập nhật Rubric Version do dữ liệu bị trùng hoặc không hợp lệ.");
    }
}
