package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class CreateFrameworkCriterionBandsUseCase
        implements IUseCase<CreateFrameworkCriterionBandsCommand, List<UUID>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    @Override
    @Transactional
    public List<UUID> execute(CreateFrameworkCriterionBandsCommand command) {

        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        Map<String, UUID> resultBandCodeToId = resolveResultBandIdsByCode(command);

        checkValidRequest(command, version, criterion, resultBandCodeToId);

        List<FrameworkCriterionBand> bandsToCreate = buildBandsToCreate(command, resultBandCodeToId);

        try {
            return frameworkCriterionBandRepository.saveAll(bandsToCreate)
                    .stream()
                    .map(fcb -> fcb.getId())
                    .collect(Collectors.toList());
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("The result code already has a band assigned for this criterion", e);
        }
    }

    private Map<String, UUID> resolveResultBandIdsByCode(CreateFrameworkCriterionBandsCommand command) {
        Set<String> requestedCodes = command.bands().stream()
                .map(b -> StringNormalization.normalizeCode(b.resultBandCode()))
                .collect(Collectors.toSet());

        if (requestedCodes.size() != command.bands().size())
            throw new IllegalArgumentException("The submitted data repeats a result code");

        List<FrameworkResultBand> existingBands = frameworkResultBandRepository.findByFrameworkVersionIdAndCodeIn(command.versionId(), requestedCodes);

        if (existingBands.size() != requestedCodes.size())
            throw new IllegalArgumentException("One or more result codes do not exist in this version");

        return existingBands.stream()
                        .collect(Collectors.toMap(frb -> frb.getCode(), frb -> frb.getId()));
    }

    private List<FrameworkCriterionBand> buildBandsToCreate(CreateFrameworkCriterionBandsCommand command,
                                                            Map<String, UUID> resultBandCodeToId) {
        return command.bands().stream().map(bandCmd -> {
            String safeCode = StringNormalization.normalizeCode(bandCmd.resultBandCode());
            return FrameworkCriterionBand.forCriterion(
                command.criterionId(),
                resultBandCodeToId.get(safeCode),
                StringNormalization.trimAndCollapseSpaces(bandCmd.descriptor()),
                bandCmd.positiveSignals(),
                bandCmd.negativeSignals());
            }).collect(Collectors.toList());
    }

    private FrameworkVersion getVersion(CreateFrameworkCriterionBandsCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkCriterion getCriterion(CreateFrameworkCriterionBandsCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
    }

    private void checkValidRequest(CreateFrameworkCriterionBandsCommand command, FrameworkVersion version, FrameworkCriterion criterion, Map<String, UUID> resultBandCodeToId) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Bands can only be added while the version is in DRAFT status");
        
        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("The criterion does not belong to this version");
    }
}
