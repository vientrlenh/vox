package com.sep.vox.application.port.input.usecase.framework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class CreateFrameworkResultBandsUseCase
        implements IUseCase<CreateFrameworkResultBandsCommand, List<UUID>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    @Override
    @Transactional
    public List<UUID> execute(CreateFrameworkResultBandsCommand command) {

        FrameworkVersion version = getVersion(command);
        validateRequest(command, version);

        List<FrameworkResultBand> bandsToSave = new ArrayList<>();
        for (CreateFrameworkResultBandsCommand.ResultBandItemCommand bandCmd : command.bands()) {
            String safeCode = StringNormalization.normalizeCode(bandCmd.code());
            String safeLabel = StringNormalization.trimAndCollapseSpaces(bandCmd.label());
            bandsToSave.add(FrameworkResultBand.forVersion(
                command.versionId(),
                safeCode,
                safeLabel,
                StringNormalization.trimAndCollapseSpaces(bandCmd.description()),
                bandCmd.order()));
        }

        try {
            List<FrameworkResultBand> savedBands = frameworkResultBandRepository.saveAll(bandsToSave);
            return savedBands.stream().map(frb -> frb.getId()).collect(Collectors.toList());
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("The result code or label already exists for this version", e);
        }
    }

    private FrameworkVersion getVersion(CreateFrameworkResultBandsCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private void validateRequest(CreateFrameworkResultBandsCommand command, FrameworkVersion version) {
        requireDraftVersionOfFramework(command, version);

        Set<String> requestCodes = new HashSet<>();
        Set<String> requestLabels = new HashSet<>();
        Set<Integer> allOrders = frameworkResultBandRepository.findByFrameworkVersionId(command.versionId())
                .stream().map(frb -> frb.getOrder()).collect(Collectors.toCollection(HashSet::new));
        for (CreateFrameworkResultBandsCommand.ResultBandItemCommand bandCmd : command.bands()) {
            requireUniqueBand(bandCmd, requestCodes, requestLabels, allOrders);
        }
        requireOrdersContinuousFromOne(allOrders);

        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndCodeIn(command.versionId(), requestCodes))
            throw new IllegalArgumentException("Result code already exists");

        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndLabelIn(command.versionId(), requestLabels))
            throw new IllegalArgumentException("Result label already exists");
    }

    private void requireDraftVersionOfFramework(CreateFrameworkResultBandsCommand command, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Result bands can only be added while the version is in DRAFT status");
    }

    private void requireUniqueBand(CreateFrameworkResultBandsCommand.ResultBandItemCommand bandCmd,
                                   Set<String> requestCodes, Set<String> requestLabels, Set<Integer> allOrders) {
        String safeCode = StringNormalization.normalizeCode(bandCmd.code());
        String safeLabel = StringNormalization.trimAndCollapseSpaces(bandCmd.label());

        if (!requestCodes.add(safeCode)) {
            throw new IllegalArgumentException("The submitted data repeats the result code: " + safeCode);
        }
        if (!requestLabels.add(safeLabel)) {
            throw new IllegalArgumentException("The submitted data repeats the result label: " + safeLabel);
        }
        if (!allOrders.add(bandCmd.order())) {
            throw new IllegalArgumentException("Duplicate result band order: " + bandCmd.order());
        }
    }

    private void requireOrdersContinuousFromOne(Set<Integer> allOrders) {
        for (int i = 1; i <= allOrders.size(); i++) {
            if (!allOrders.contains(i)) {
                throw new IllegalArgumentException("Result band order must increase continuously from 1 without skipping a number");
            }
        }
    }
}
