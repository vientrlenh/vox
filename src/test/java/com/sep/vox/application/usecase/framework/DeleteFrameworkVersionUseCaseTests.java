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
import com.sep.vox.application.port.input.command.DeleteFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkVersionUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class DeleteFrameworkVersionUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private FrameworkCriterionRepository frameworkCriterionRepository;
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private DeleteFrameworkVersionUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private UUID versionId = UUID.randomUUID();
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        frameworkCriterionRepository = mock(FrameworkCriterionRepository.class);
        frameworkCriterionBandRepository = mock(FrameworkCriterionBandRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        useCase = new DeleteFrameworkVersionUseCase(
            frameworkVersionRepository,
            frameworkCriterionRepository,
            frameworkCriterionBandRepository,
            frameworkResultBandRepository,
            assessmentPolicyRepository
        );
    }

    @Test
    void should_hard_delete_draft_version_with_children() {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var criterion1 = new FrameworkCriterion();
        criterion1.setId(UUID.randomUUID());
        var criterion2 = new FrameworkCriterion();
        criterion2.setId(UUID.randomUUID());

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findFrameworkVersionByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId))
            .thenReturn(List.of(criterion1, criterion2));

        useCase.execute(command);

        verify(frameworkCriterionBandRepository).deleteByFrameworkCriterionIdIn(
            List.of(criterion1.getId(), criterion2.getId()));
        verify(frameworkCriterionRepository).deleteByFrameworkVersionId(versionId);
        verify(frameworkResultBandRepository).deleteByFrameworkVersionId(versionId);
        verify(frameworkVersionRepository).deleteFrameworkVersionById(versionId);
    }

    @Test
    void should_soft_delete_published_version() {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findFrameworkVersionByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        useCase.execute(command);

        verify(frameworkVersionRepository).updateFrameworkVersionStatus(versionId, FrameworkVersionStatus.ARCHIVED);
    }

    @Test
    void should_soft_delete_archived_version() {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.ARCHIVED);

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findFrameworkVersionByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        useCase.execute(command);

        verify(frameworkVersionRepository).updateFrameworkVersionStatus(versionId, FrameworkVersionStatus.ARCHIVED);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findFrameworkVersionByIdForUpdate(versionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_belongs_to_framework() {
        var otherFrameworkId = UUID.randomUUID();
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(otherFrameworkId);

        when(frameworkRepository.findFrameworkById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findFrameworkVersionByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }
}
