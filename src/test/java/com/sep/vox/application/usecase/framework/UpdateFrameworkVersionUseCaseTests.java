package com.sep.vox.application.usecase.framework;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class UpdateFrameworkVersionUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private UserContextPort userContextPort;
    private UpdateFrameworkVersionUseCase useCase;

    private final UUID frameworkId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateFrameworkVersionUseCase(frameworkVersionRepository, userContextPort);
    }

    @Test
    void should_update_draft_version() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365)
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

        useCase.execute(command);

        verify(frameworkVersionRepository).save(any(FrameworkVersion.class));
    }

    @Test
    void should_throw_when_framework_not_found() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365)
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_found() {
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365)
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_belongs_to_framework() {
        var otherFrameworkId = UUID.randomUUID();
        var command = new UpdateFrameworkVersionCommand(
            frameworkId, versionId, "V2_0", "Version 2.0", "Updated",
            now, now.plusDays(365)
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
            now, now.plusDays(365)
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
}
