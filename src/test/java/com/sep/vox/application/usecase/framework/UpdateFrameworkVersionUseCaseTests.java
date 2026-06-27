package com.sep.vox.application.usecase.framework;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand.ResultBandInput;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class UpdateFrameworkVersionUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private FrameworkCriterionRepository frameworkCriterionRepository;
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private UserContextPort userContextPort;
    private UpdateFrameworkVersionUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private UUID versionId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        frameworkCriterionRepository = mock(FrameworkCriterionRepository.class);
        frameworkCriterionBandRepository = mock(FrameworkCriterionBandRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateFrameworkVersionUseCase(
            frameworkRepository,
            frameworkVersionRepository,
            frameworkCriterionRepository,
            frameworkCriterionBandRepository,
            frameworkResultBandRepository,
            userContextPort
        );
    }

    @Test
    void should_update_draft_version_with_new_criteria_and_result_bands() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated version",
            now, now.plusDays(365),
            List.of(),
            List.of()
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId))
            .thenReturn(List.of());
        when(frameworkResultBandRepository.findByFrameworkVersionId(versionId))
            .thenReturn(List.of());

        useCase.execute(command);

        verify(frameworkVersionRepository).save(any(FrameworkVersion.class));
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365), List.of(), List.of()
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365), List.of(), List.of()
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_belongs_to_framework() {
        var otherFrameworkId = UUID.randomUUID();
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365), List.of(), List.of()
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(otherFrameworkId);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_draft() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365), List.of(), List.of()
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_replace_result_bands_atomically() {
        var newBandInput = new ResultBandInput("A1", "Beginner", "Level A1", 1);
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365),
            List.of(),
            List.of(newBandInput)
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var oldBand = new FrameworkResultBand();
        oldBand.setId(UUID.randomUUID());

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId))
            .thenReturn(List.of());
        when(frameworkResultBandRepository.findByFrameworkVersionId(versionId))
            .thenReturn(List.of(oldBand));

        useCase.execute(command);

        verify(frameworkResultBandRepository).deleteByFrameworkVersionId(versionId);
        verify(frameworkResultBandRepository).saveAll(any());
    }
}
