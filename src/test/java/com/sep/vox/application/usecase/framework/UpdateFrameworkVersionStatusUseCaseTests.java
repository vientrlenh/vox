package com.sep.vox.application.usecase.framework;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public class UpdateFrameworkVersionStatusUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private FrameworkCriterionRepository frameworkCriterionRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private UpdateFrameworkVersionStatusUseCase useCase;

    private UUID frameworkId = UUID.randomUUID();
    private UUID versionId = UUID.randomUUID();
    private Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        frameworkCriterionRepository = mock(FrameworkCriterionRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        frameworkCriterionBandRepository = mock(FrameworkCriterionBandRepository.class);
        useCase = new UpdateFrameworkVersionStatusUseCase(
            frameworkRepository, frameworkVersionRepository,
            frameworkCriterionRepository, frameworkResultBandRepository,
            frameworkCriterionBandRepository);
    }

    private List<FrameworkCriterion> buildValidCriteria() {
        List<String> codes = List.of("PRONUNCIATION", "FLUENCY", "GRAMMAR", "VOCABULARY", "COHERENCE");
        List<FrameworkCriterion> criteria = new ArrayList<>();
        int order = 1;
        for (String code : codes) {
            criteria.add(new FrameworkCriterion(
                    UUID.randomUUID(), versionId, code, code, "Description", order++, now, now, null, null));
        }
        return criteria;
    }

    private List<FrameworkCriterionBand> buildValidBands(List<FrameworkCriterion> criteria) {
        var signals = new FrameworkCriterionSignals(List.of(
                new FrameworkCriterionSignal("S1", "desc", FrameworkCriterionSignalImportance.HIGH, null)));
        return criteria.stream()
                .map(criterion -> new FrameworkCriterionBand(
                        UUID.randomUUID(), criterion.getId(), UUID.randomUUID(), "descriptor",
                        signals, signals, now, now, null, null))
                .collect(Collectors.toList());
    }

    @Test
    void should_publish_version_when_no_conflict() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        List<FrameworkCriterion> criteria = buildValidCriteria();
        List<FrameworkCriterionBand> bands = buildValidBands(criteria);
        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());

        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId)).thenReturn(criteria);
        when(frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)).thenReturn(bands);
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of());
        when(frameworkVersionRepository.updateStatus(versionId, FrameworkVersionStatus.PUBLISHED)).thenReturn(1);

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.PUBLISHED);
    }

    @Test
    void should_throw_when_criterion_has_no_bands() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        List<FrameworkCriterion> criteria = buildValidCriteria();
        List<FrameworkCriterionBand> bands = buildValidBands(criteria).stream()
            .filter(band -> !band.getFrameworkCriterionId().equals(criteria.get(0).getId()))
            .collect(Collectors.toList());
        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId)).thenReturn(criteria);
        when(frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)).thenReturn(bands);

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_criterion_band_missing_signals() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        List<FrameworkCriterion> criteria = buildValidCriteria();
        var emptySignals = new FrameworkCriterionSignals(List.of());
        List<FrameworkCriterionBand> bands = buildValidBands(criteria).stream()
            .map(band -> band.getFrameworkCriterionId().equals(criteria.get(0).getId())
                ? new FrameworkCriterionBand(band.getId(), band.getFrameworkCriterionId(), band.getFrameworkResultBandId(),
                    band.getDescriptor(), emptySignals, emptySignals, now, now, null, null)
                : band)
            .collect(Collectors.toList());
        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId)).thenReturn(criteria);
        when(frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)).thenReturn(bands);

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_publishing_without_criteria() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_publishing_without_result_bands() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_archive_version() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.ARCHIVED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkVersionRepository.updateStatus(versionId, FrameworkVersionStatus.ARCHIVED)).thenReturn(1);

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.ARCHIVED);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
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
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(otherFrameworkId);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
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
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(30, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var publishedVersion = new FrameworkVersion();
        publishedVersion.setId(UUID.randomUUID());
        publishedVersion.setFrameworkId(frameworkId);
        publishedVersion.setEffectiveFrom(now.plus(15, ChronoUnit.DAYS));
        publishedVersion.setEffectiveTo(now.plus(45, ChronoUnit.DAYS));
        publishedVersion.setStatus(FrameworkVersionStatus.PUBLISHED);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of(publishedVersion));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_archiving_draft_version() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.ARCHIVED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_allow_publish_when_date_ranges_do_not_overlap() {
        var command = new UpdateFrameworkVersionStatusCommand(
            frameworkId, versionId, FrameworkVersionStatus.PUBLISHED
        );

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "Test", "Description",
            true, now, now, null, null
        );
        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plus(10, ChronoUnit.DAYS));
        version.setStatus(FrameworkVersionStatus.DRAFT);

        var publishedVersion = new FrameworkVersion();
        publishedVersion.setId(UUID.randomUUID());
        publishedVersion.setFrameworkId(frameworkId);
        publishedVersion.setEffectiveFrom(now.plus(20, ChronoUnit.DAYS));
        publishedVersion.setEffectiveTo(now.plus(40, ChronoUnit.DAYS));
        publishedVersion.setStatus(FrameworkVersionStatus.PUBLISHED);

        List<FrameworkCriterion> criteria = buildValidCriteria();
        List<FrameworkCriterionBand> bands = buildValidBands(criteria);
        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());

        when(frameworkRepository.findFrameworkByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));
        when(frameworkCriterionRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkResultBandRepository.existsByFrameworkVersionId(versionId)).thenReturn(true);
        when(frameworkCriterionRepository.findByFrameworkVersionId(versionId)).thenReturn(criteria);
        when(frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)).thenReturn(bands);
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of(publishedVersion));
        when(frameworkVersionRepository.updateStatus(versionId, FrameworkVersionStatus.PUBLISHED)).thenReturn(1);

        useCase.execute(command);

        verify(frameworkVersionRepository).updateStatus(versionId, FrameworkVersionStatus.PUBLISHED);
    }
}
