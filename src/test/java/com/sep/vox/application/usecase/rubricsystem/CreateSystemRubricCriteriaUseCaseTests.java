package com.sep.vox.application.usecase.rubricsystem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import com.sep.vox.application.port.input.command.CreateSystemRubricCriteriaCommand;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CreateSystemRubricCriteriaUseCaseTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();
    private final UUID frameworkCriterionId = UUID.randomUUID();

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
    private FrameworkCriterionRepository frameworkCriterionRepository;

    @InjectMocks
    private CreateSystemRubricCriteriaUseCase useCase;

    @Test
    void execute_throwsIllegalStateWhenTheRepositoryReportsADataConflict() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).status(UserStatus.ACTIVE).build()));
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder()
            .id(rubricId).ownerType(RubricOwnerType.SYSTEM).build()));
        when(frameworkCriterionRepository.findAllByIds(List.of(frameworkCriterionId)))
            .thenReturn(List.of(FrameworkCriterion.builder().id(frameworkCriterionId).build()));
        when(rubricCriterionRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
        doThrow(new DataConflictException("unique violation", null)).when(rubricCriterionRepository).saveAll(any());
        CreateSystemRubricCriteriaCommand command = new CreateSystemRubricCriteriaCommand(versionId, List.of(new CreateSystemRubricCriteriaCommand.CriterionItemCommand(frameworkCriterionId, "FLUENCY", "Fluency", null, null,
                null, BigDecimal.ZERO, BigDecimal.TEN, 1, true)));

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Lỗi lưu dữ liệu: Mã tiêu chí, Khung tiêu chuẩn (Framework) hoặc Thứ tự (order) đã tồn tại trong phiên bản Rubric hệ thống này từ trước.");
    }
}
