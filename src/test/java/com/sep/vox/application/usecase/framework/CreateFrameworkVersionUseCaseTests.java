package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sep.vox.application.exception.DataConflictException;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class CreateFrameworkVersionUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private CreateFrameworkVersionUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        useCase = new CreateFrameworkVersionUseCase(frameworkRepository, frameworkVersionRepository);
    }

    @Test
    void should_create_framework_version_when_framework_exists() {
        CreateFrameworkVersionCommand command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plus(30, ChronoUnit.DAYS)
        );

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("Test Framework")
            .description("Description")
            .active(true)
            .build();
        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.save(any(FrameworkVersion.class)))
            .thenAnswer(invocation -> {
                FrameworkVersion version = invocation.getArgument(0);
                version.setId(UUID.randomUUID());
                return version;
            });

        CreateFrameworkVersionResponse result = useCase.execute(command);

        assertThat(result.versionId()).isNotNull();
        verify(frameworkRepository).findById(frameworkId);
        verify(frameworkVersionRepository).save(any(FrameworkVersion.class));
    }

    @Test
    void should_throw_not_found_when_framework_does_not_exist() {
        CreateFrameworkVersionCommand command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plus(30, ChronoUnit.DAYS)
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_duplicated_when_version_number_exists() {
        CreateFrameworkVersionCommand command = new CreateFrameworkVersionCommand(
            frameworkId, "V1_0", "Version 1.0", "Initial version", 1, now, now.plus(30, ChronoUnit.DAYS)
        );

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("Test Framework")
            .description("Description")
            .active(true)
            .build();
        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.save(any(FrameworkVersion.class)))
            .thenThrow(new DataConflictException("duplicate", null));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));
    }
}
