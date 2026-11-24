package com.sep.vox.application.usecase.rubricsystem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricResultBandUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSystemRubricResultBandUseCaseTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID resultBandId = UUID.randomUUID();
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

    @InjectMocks
    private UpdateSystemRubricResultBandUseCase useCase;

    private void stubDraftResultBand() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(rubricResultBandRepository.findById(resultBandId)).thenReturn(Optional.of(RubricResultBand.builder()
            .id(resultBandId).rubricVersionId(versionId).name("Band")
            .scoreMin(BigDecimal.ZERO).scoreMax(BigDecimal.TEN).order(1).build()));
        when(rubricResultBandRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder().id(rubricId).ownerType(RubricOwnerType.SYSTEM).build()));
    }

    @Test
    void execute_clearsDescriptionOnAnExplicitNull() {
        stubDraftResultBand();

        useCase.execute(new UpdateSystemRubricResultBandCommand(resultBandId, "Band", null, true, null, null, null));

        verify(rubricResultBandRepository).updateResultBandAtomic(eq(resultBandId), any(), any(), eq(null), eq(true),
            any(), any(), any(), any(), any());
    }

    @Test
    void execute_keepsDescriptionWhenNotProvidedOrBlank() {
        stubDraftResultBand();

        useCase.execute(new UpdateSystemRubricResultBandCommand(resultBandId, "Band", null, false, null, null, null));
        useCase.execute(new UpdateSystemRubricResultBandCommand(resultBandId, "Band", "  ", true, null, null, null));

        verify(rubricResultBandRepository, times(2)).updateResultBandAtomic(eq(resultBandId), any(), any(), eq(null), eq(false),
            any(), any(), any(), any(), any());
    }

    @Test
    void execute_throwsIllegalArgumentWhenTheRepositoryReportsADataConflict() {
        stubDraftResultBand();
        doThrow(new DataConflictException("unique violation", null)).when(rubricResultBandRepository)
            .updateResultBandAtomic(any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(), any());
        UpdateSystemRubricResultBandCommand command = new UpdateSystemRubricResultBandCommand(resultBandId, "Band", null, false, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Không thể cập nhật Dải điểm Kết quả do dữ liệu bị trùng hoặc không hợp lệ.");
    }
}
