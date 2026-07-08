package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.application.port.input.command.CreateFrameworkCriterionBandsCommand.CriterionBandItemCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriterionBandsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public class CreateFrameworkCriterionBandsUseCaseTests {

    private FrameworkVersionRepository frameworkVersionRepository;
    private FrameworkCriterionRepository frameworkCriterionRepository;
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private UserContextPort userContextPort;
    private CreateFrameworkCriterionBandsUseCase useCase;

    private final UUID frameworkId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        frameworkCriterionRepository = mock(FrameworkCriterionRepository.class);
        frameworkCriterionBandRepository = mock(FrameworkCriterionBandRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateFrameworkCriterionBandsUseCase(
                frameworkVersionRepository, frameworkCriterionRepository,
                frameworkCriterionBandRepository, frameworkResultBandRepository, userContextPort);

        var version = new FrameworkVersion(versionId, frameworkId, "V1_0", "Version 1.0", "Desc", 1, now, now.plusDays(30),
                FrameworkVersionStatus.DRAFT, now, now, userId, userId);
        when(frameworkVersionRepository.findFrameworkVersionById(versionId)).thenReturn(Optional.of(version));

        var criterion = new FrameworkCriterion(criterionId, versionId, "C1", "Criterion 1", "Desc", 1, now, now, userId, userId);
        when(frameworkCriterionRepository.findById(criterionId)).thenReturn(Optional.of(criterion));

        var resultBand = new FrameworkResultBand(UUID.randomUUID(), versionId, "RB1", "Label", "Desc", 1, now, now, userId, userId);
        when(frameworkResultBandRepository.findByFrameworkVersionIdAndCodeIn(any(), any())).thenAnswer(invocation -> {
            Set<String> codes = invocation.getArgument(1);
            return List.of(resultBand).stream().filter(rb -> codes.contains(rb.getCode())).collect(Collectors.toList());
        });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
    }

    private CriterionBandItemCommand bandItem(String resultBandCode) {
        return new CriterionBandItemCommand(resultBandCode, "Descriptor",
                new FrameworkCriterionSignals(List.of()), new FrameworkCriterionSignals(List.of()));
    }

    @Test
    void should_create_bands_when_result_band_code_matches() {
        when(frameworkCriterionBandRepository.saveAll(any())).thenAnswer(invocation -> {
            List<FrameworkCriterionBand> bands = invocation.getArgument(0);
            bands.forEach(b -> b.setId(UUID.randomUUID()));
            return bands;
        });

        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, List.of(bandItem("rb1")));
        var ids = useCase.execute(command);

        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isNotNull();
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        when(frameworkVersionRepository.findFrameworkVersionById(versionId)).thenReturn(Optional.empty());
        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, List.of(bandItem("rb1")));

        assertThrows(NotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_version_not_draft() {
        var activeVersion = new FrameworkVersion(versionId, frameworkId, "V1_0", "Version 1.0", "Desc", 1, now, now.plusDays(30),
                FrameworkVersionStatus.PUBLISHED, now, now, userId, userId);
        when(frameworkVersionRepository.findFrameworkVersionById(versionId)).thenReturn(Optional.of(activeVersion));
        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, List.of(bandItem("rb1")));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_criterion_belongs_to_different_version() {
        var otherCriterion = new FrameworkCriterion(criterionId, UUID.randomUUID(), "C1", "Criterion 1", "Desc", 1, now, now, userId, userId);
        when(frameworkCriterionRepository.findById(criterionId)).thenReturn(Optional.of(otherCriterion));
        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, List.of(bandItem("rb1")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_result_band_code_unknown() {
        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, List.of(bandItem("unknown")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_duplicate_result_band_code_in_batch() {
        var command = new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId,
                List.of(bandItem("rb1"), bandItem("RB1")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }
}
