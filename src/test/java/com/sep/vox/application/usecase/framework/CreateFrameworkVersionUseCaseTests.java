package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class CreateFrameworkVersionUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private UserContextPort userContextPort;
    private CreateFrameworkVersionUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateFrameworkVersionUseCase(frameworkRepository, frameworkVersionRepository, userContextPort);
    }

    @Test
    void should_create_framework_version_when_framework_exists() {
        var command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plusDays(30)
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test Framework", "Description",
            true, now, now, null, null
        );
        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkIdAndVersion(frameworkId, 1))
            .thenReturn(Optional.empty());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(frameworkVersionRepository.save(any(FrameworkVersion.class)))
            .thenAnswer(invocation -> {
                FrameworkVersion version = invocation.getArgument(0);
                version.setId(UUID.randomUUID());
                return version;
            });

        var result = useCase.execute(command);

        assertThat(result.versionId()).isNotNull();
        verify(frameworkRepository).findById(frameworkId);
        verify(frameworkVersionRepository).findByFrameworkIdAndVersion(frameworkId, 1);
        verify(frameworkVersionRepository).save(any(FrameworkVersion.class));
    }

    @Test
    void should_throw_not_found_when_framework_does_not_exist() {
        var command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plusDays(30)
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_duplicated_when_version_number_exists() {
        var command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plusDays(30)
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test Framework", "Description",
            true, now, now, null, null
        );
        var existingVersion = new FrameworkVersion();
        existingVersion.setId(UUID.randomUUID());

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkIdAndVersion(frameworkId, 1))
            .thenReturn(Optional.of(existingVersion));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));
    }
}
