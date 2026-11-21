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
import com.sep.vox.application.port.input.command.UpdateFrameworkResultBandCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkResultBandUseCase;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@ExtendWith(MockitoExtension.class)
class UpdateFrameworkResultBandUseCaseTests {

    private final UUID frameworkId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID bandId = UUID.randomUUID();

    @Mock
    private FrameworkVersionRepository frameworkVersionRepository;

    @Mock
    private FrameworkResultBandRepository frameworkResultBandRepository;

    @InjectMocks
    private UpdateFrameworkResultBandUseCase useCase;

    @Test
    void execute_throwsIllegalStateWhenTheRepositoryReportsADataConflict() {
        FrameworkVersion draftVersion = FrameworkVersion.builder()
            .id(versionId)
            .frameworkId(frameworkId)
            .status(FrameworkVersionStatus.DRAFT)
            .build();
        FrameworkResultBand band = FrameworkResultBand.builder()
            .id(bandId)
            .frameworkVersionId(versionId)
            .code("B1")
            .label("Basic")
            .order(1)
            .build();
        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion));
        when(frameworkResultBandRepository.findById(bandId)).thenReturn(Optional.of(band));
        when(frameworkResultBandRepository.findByFrameworkVersionId(versionId)).thenReturn(List.of(band));
        when(frameworkResultBandRepository.save(any(FrameworkResultBand.class)))
            .thenThrow(new DataConflictException("unique violation", null));
        UpdateFrameworkResultBandCommand command = new UpdateFrameworkResultBandCommand(
            frameworkId, versionId, bandId, "B1", "Basic", "Basic level", 1);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Mã hoặc nhãn kết quả đã tồn tại cho phiên bản này");
    }
}
