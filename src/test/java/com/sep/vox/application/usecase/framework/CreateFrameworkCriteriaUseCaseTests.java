package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.command.CreateFrameworkCriteriaCommand;
import com.sep.vox.application.port.input.command.CreateFrameworkCriteriaCommand.CriterionItemCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriteriaUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@ExtendWith(MockitoExtension.class)
class CreateFrameworkCriteriaUseCaseTests {

    private final UUID frameworkId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    @Mock
    private FrameworkVersionRepository frameworkVersionRepository;

    @Mock
    private FrameworkCriterionRepository frameworkCriterionRepository;

    @InjectMocks
    private CreateFrameworkCriteriaUseCase useCase;

    @Test
    void execute_throwsIllegalStateWhenTheRepositoryReportsADataConflict() {
        FrameworkVersion draftVersion = FrameworkVersion.builder()
            .id(versionId)
            .frameworkId(frameworkId)
            .status(FrameworkVersionStatus.DRAFT)
            .build();
        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion));
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId)).thenReturn(List.of());
        when(frameworkCriterionRepository.saveAll(any())).thenThrow(new DataConflictException("unique violation", null));
        CreateFrameworkCriteriaCommand command = new CreateFrameworkCriteriaCommand(frameworkId, versionId,
            List.of(new CriterionItemCommand("FLUENCY", "Fluency", "Speaks smoothly", 1)));

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Mã tiêu chí đã tồn tại trong cơ sở dữ liệu");
    }
}
