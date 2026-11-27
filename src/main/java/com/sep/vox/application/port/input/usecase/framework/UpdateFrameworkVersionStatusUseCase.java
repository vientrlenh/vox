package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkVersionStatusUseCase implements IUseCase<UpdateFrameworkVersionStatusCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionStatusCommand input) {
        getFramework(input);
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        int updated = frameworkVersionRepository.updateStatus(input.versionId(), input.status());
        if (updated == 0) {
            throw new ResourceNotFoundException("Framework version to update the status of not found");
        }
        return input.versionId();
    }

    private void getFramework(UpdateFrameworkVersionStatusCommand input) {
        frameworkRepository.findFrameworkByIdForUpdate(input.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework not found"));
    }

    private FrameworkVersion getVersion(UpdateFrameworkVersionStatusCommand input) {
        return frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private void checkValidRequest(UpdateFrameworkVersionStatusCommand input, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(input.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (input.status() == FrameworkVersionStatus.PUBLISHED) {
            validatePublishable(input, version);
        } else if (input.status() == FrameworkVersionStatus.ARCHIVED) {
            if (version.getStatus() != FrameworkVersionStatus.PUBLISHED)
                throw new IllegalArgumentException("Only a version in PUBLISHED status can be archived");
        } else {
            throw new IllegalArgumentException("Invalid status for update");
        }
    }

    private void validatePublishable(UpdateFrameworkVersionStatusCommand input, FrameworkVersion version) {
        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Only a version in DRAFT status can be published");
        if (!frameworkCriterionRepository.existsByFrameworkVersionId(input.versionId()))
            throw new IllegalArgumentException("A framework version must have at least one criterion before it is published");
        if (!frameworkResultBandRepository.existsByFrameworkVersionId(input.versionId()))
            throw new IllegalArgumentException("A framework version must have at least one result band before it is published");
        validateEveryCriterionHasBandsWithSignals(input.versionId());
        validateNoConflictingPublished(input.frameworkId(), input.versionId(), version.getEffectiveFrom(), version.getEffectiveTo());
    }

    private void validateEveryCriterionHasBandsWithSignals(UUID versionId) {
        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);

        Set<String> presentCodes = criteria.stream().map(fc -> fc.getCode()).collect(Collectors.toSet());
        if (!presentCodes.equals(FrameworkCriterionCode.ALLOWED_CODES)) {
            throw new IllegalArgumentException(
                    "A framework version must have exactly the 5 criteria: " + FrameworkCriterionCode.ALLOWED_CODES);
        }

        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());
        List<FrameworkCriterionBand> bands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds);

        Map<UUID, List<FrameworkCriterionBand>> bandsByCriterion = bands.stream().collect(Collectors.groupingBy(fcb -> fcb.getFrameworkCriterionId()));

        for (FrameworkCriterion criterion : criteria) {
            validateCriterionBandsHaveSignals(criterion, bandsByCriterion.getOrDefault(criterion.getId(), List.of()));
        }
    }

    private void validateCriterionBandsHaveSignals(FrameworkCriterion criterion, List<FrameworkCriterionBand> criterionBands) {
        if (criterionBands.isEmpty()) {
            throw new IllegalArgumentException(
                    "Criterion \"" + criterion.getName() + "\" must have at least one result scale before it is published");
        }
        for (FrameworkCriterionBand band : criterionBands) {
            if (band.getPositiveSignals() == null || band.getPositiveSignals().values().isEmpty()
                    || band.getNegativeSignals() == null || band.getNegativeSignals().values().isEmpty()) {
                throw new IllegalArgumentException(
                        "Criterion \"" + criterion.getName() + "\" must have at least one positive and one negative indicator in every result scale before it is published");
            }
        }
    }

    private void validateNoConflictingPublished(UUID frameworkId, UUID versionId, Instant effectiveFrom, Instant effectiveTo) {
        List<FrameworkVersion> published = frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED);
        boolean hasConflict = published.stream()
            .filter(v -> !v.getId().equals(versionId))
            .anyMatch(v -> rangesOverlap(effectiveFrom, effectiveTo, v.getEffectiveFrom(), v.getEffectiveTo()));
        if (hasConflict) {
            throw new IllegalArgumentException("A published version that is effective in this period already exists");
        }
    }

    private boolean rangesOverlap(Instant from1, Instant to1, Instant from2, Instant to2) {
        Instant start1 = from1 != null ? from1 : Instant.MIN;
        Instant start2 = from2 != null ? from2 : Instant.MIN;
        Instant end1 = to1 != null ? to1 : Instant.MAX;
        Instant end2 = to2 != null ? to2 : Instant.MAX;
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
