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
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkCriterionUseCase
        implements IUseCase<UpdateFrameworkCriterionCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCriterionCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        checkValidRequest(command, version, criterion);
        updateCriterion(command, criterion);

        try {
            frameworkCriterionRepository.save(criterion);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Criterion code already exists", e);
        }

        return criterion.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkCriterionCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkCriterion getCriterion(UpdateFrameworkCriterionCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
    }

    private void checkValidRequest(UpdateFrameworkCriterionCommand command, FrameworkVersion version,
            FrameworkCriterion criterion) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Criteria can only be updated while the version is in DRAFT status");

        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("The criterion does not belong to this version");

        requireValidUniqueCode(command);
        requireOrderKeepsSequenceContinuous(command);
    }

    private void requireValidUniqueCode(UpdateFrameworkCriterionCommand command) {
        String safeCode = StringNormalization.normalizeCode(command.code());
        if (!FrameworkCriterionCode.ALLOWED_CODES.contains(safeCode)) {
            throw new IllegalArgumentException("Invalid criterion code: " + safeCode);
        }
        if (frameworkCriterionRepository.existsByFrameworkVersionIdAndCodeAndIdNot(command.versionId(), safeCode, command.criterionId())) {
            throw new IllegalArgumentException("Criterion code already exists");
        }
    }

    private void requireOrderKeepsSequenceContinuous(UpdateFrameworkCriterionCommand command) {
        Set<Integer> otherOrders = frameworkCriterionRepository.findByFrameworkVersionId(command.versionId())
                .stream()
                .filter(fc -> !fc.getId().equals(command.criterionId()))
                .map(fc -> fc.getOrder())
                .collect(Collectors.toSet());
        if (!otherOrders.add(command.order())) {
            throw new IllegalArgumentException("Duplicate criterion order: " + command.order());
        }
        int expectedOrder = otherOrders.size();
        for (int i = 1; i <= expectedOrder; i++) {
            if (!otherOrders.contains(i)) {
                throw new IllegalArgumentException("Criterion order must increase continuously from 1 without skipping a number");
            }
        }
    }

    private void updateCriterion(UpdateFrameworkCriterionCommand command, FrameworkCriterion criterion) {
        criterion.setCode(StringNormalization.normalizeCode(command.code()));
        criterion.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        if (command.descriptionProvided())
            criterion.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        criterion.setOrder(command.order());
    }
}
