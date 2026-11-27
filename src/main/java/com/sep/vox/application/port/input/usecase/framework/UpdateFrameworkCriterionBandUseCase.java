package com.sep.vox.application.port.input.usecase.framework;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkCriterionBandUseCase
        implements IUseCase<UpdateFrameworkCriterionBandCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCriterionBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);
        FrameworkCriterionBand band = getBand(command);

        checkValidRequest(command, version, criterion, band);
        updateCriterionBand(command, band);

        frameworkCriterionBandRepository.save(band);
        return band.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkCriterionBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkCriterion getCriterion(UpdateFrameworkCriterionBandCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
    }

    private FrameworkCriterionBand getBand(UpdateFrameworkCriterionBandCommand command) {
        return frameworkCriterionBandRepository.findById(command.bandId())
            .orElseThrow(() -> new ResourceNotFoundException("Band not found"));
    }

    private void checkValidRequest(UpdateFrameworkCriterionBandCommand command, FrameworkVersion version,
            FrameworkCriterion criterion, FrameworkCriterionBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Bands can only be updated while the version is in DRAFT status");

        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("The criterion does not belong to this version");

        if (!band.getFrameworkCriterionId().equals(command.criterionId()))
            throw new IllegalArgumentException("The band does not belong to this criterion");
    }

    private void updateCriterionBand(UpdateFrameworkCriterionBandCommand command, FrameworkCriterionBand band) {
        if (shouldChangeDescriptor(command))
            band.setDescriptor(StringNormalization.trimAndCollapseSpaces(command.descriptor()));

        if (hasSignals(command.positiveSignals())) 
            band.setPositiveSignals(command.positiveSignals());

        if (hasSignals(command.negativeSignals())) 
            band.setNegativeSignals(command.negativeSignals());

    }

    private boolean shouldChangeDescriptor(UpdateFrameworkCriterionBandCommand command) {
        return command.descriptorProvided() && (command.descriptor() == null || !command.descriptor().isEmpty());
    }

    private boolean hasSignals(FrameworkCriterionSignals signals) {
        return signals != null && !signals.values().isEmpty();
    }
}
