package com.sep.vox.application.port.input.usecase.framework;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkResultBandUseCase
        implements IUseCase<UpdateFrameworkResultBandCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkResultBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkResultBand band = getBand(command);

        checkValidRequest(command, version, band);
        updateResultBand(command, band);

        try {
            frameworkResultBandRepository.save(band);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("The result code or label already exists for this version", e);
        }

        return band.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkResultBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkResultBand getBand(UpdateFrameworkResultBandCommand command) {
        return frameworkResultBandRepository.findById(command.bandId())
            .orElseThrow(() -> new ResourceNotFoundException("Result band not found"));
    }

    private void checkValidRequest(UpdateFrameworkResultBandCommand command, FrameworkVersion version, FrameworkResultBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Result bands can only be updated while the version is in DRAFT status");

        if (!band.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("The result band does not belong to this version");

        requireUniqueCodeAndLabel(command);
        requireOrderKeepsSequenceContinuous(command);
    }

    private void requireUniqueCodeAndLabel(UpdateFrameworkResultBandCommand command) {
        String safeCode = StringNormalization.normalizeCode(command.code());
        String safeLabel = StringNormalization.trimAndCollapseSpaces(command.label());
        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndCodeAndIdNot(command.versionId(), safeCode, command.bandId())) {
            throw new IllegalArgumentException("Result code already exists");
        }
        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndLabelAndIdNot(command.versionId(), safeLabel, command.bandId())) {
            throw new IllegalArgumentException("Result label already exists");
        }
    }

    private void requireOrderKeepsSequenceContinuous(UpdateFrameworkResultBandCommand command) {
        Set<Integer> otherOrders = frameworkResultBandRepository.findByFrameworkVersionId(command.versionId())
                .stream()
                .filter(frb -> !frb.getId().equals(command.bandId()))
                .map(frb -> frb.getOrder())
                .collect(Collectors.toSet());
        if (!otherOrders.add(command.order())) {
            throw new IllegalArgumentException("Duplicate result band order: " + command.order());
        }
        int expectedOrder = otherOrders.size();
        for (int i = 1; i <= expectedOrder; i++) {
            if (!otherOrders.contains(i)) {
                throw new IllegalArgumentException("Result band order must increase continuously from 1 without skipping a number");
            }
        }
    }

    private void updateResultBand(UpdateFrameworkResultBandCommand command, FrameworkResultBand band) {
        band.setCode(StringNormalization.normalizeCode(command.code()));
        band.setLabel(StringNormalization.trimAndCollapseSpaces(command.label()));
        if (command.descriptionProvided())
            band.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        band.setOrder(command.order());
    }
}
