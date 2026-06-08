package com.sep.vox.application.usecase.framework;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class UpdateFrameworkVersionStatusUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private UpdateFrameworkVersionStatusUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private UUID versionId = UUID.randomUUID();
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        useCase = new UpdateFrameworkVersionStatusUseCase(frameworkRepository, frameworkVersionRepository);
    }

    @Test
    void should_publish_version_when_no_conflict() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plusDays(30));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of());

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.PUBLISHED);
        verify(frameworkRepository).updateCurrentVersionId(frameworkId, versionId);
    }

    @Test
    void should_archive_version() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.ARCHIVED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.ARCHIVED);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_belongs_to_framework() {
        var otherFrameworkId = UUID.randomUUID();
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(otherFrameworkId);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_date_range_overlaps_with_published() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plusDays(30));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var publishedVersion = new FrameworkVersion();
        publishedVersion.setId(UUID.randomUUID());
        publishedVersion.setFrameworkId(frameworkId);
        publishedVersion.setEffectiveFrom(now.plusDays(15));
        publishedVersion.setEffectiveTo(now.plusDays(45));
        publishedVersion.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of(publishedVersion));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_allow_publish_when_date_ranges_do_not_overlap() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, null, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plusDays(10));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var publishedVersion = new FrameworkVersion();
        publishedVersion.setId(UUID.randomUUID());
        publishedVersion.setFrameworkId(frameworkId);
        publishedVersion.setEffectiveFrom(now.plusDays(20));
        publishedVersion.setEffectiveTo(now.plusDays(40));
        publishedVersion.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of(publishedVersion));

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.PUBLISHED);
    }
}
