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
import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;

@Service
@RequiredArgsConstructor
public class CreateFrameworkCriteriaUseCase
        implements IUseCase<CreateFrameworkCriteriaCommand, List<UUID>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    @Override
    @Transactional
    public List<UUID> execute(CreateFrameworkCriteriaCommand command) {

        FrameworkVersion version = getVersion(command);
        validateRequest(command, version);

        List<FrameworkCriterion> criteriaToSave = new ArrayList<>(command.criteria().size());
        for (CreateFrameworkCriteriaCommand.CriterionItemCommand criterionCmd : command.criteria()) {
            criteriaToSave.add(FrameworkCriterion.forVersion(
                command.versionId(),
                StringNormalization.normalizeCode(criterionCmd.code()),
                StringNormalization.trimAndCollapseSpaces(criterionCmd.name()),
                StringNormalization.trimAndCollapseSpaces(criterionCmd.description()),
                criterionCmd.order()));
        }

        try {
            return frameworkCriterionRepository.saveAll(criteriaToSave)
                    .stream()
                    .map(fc -> fc.getId())
                    .collect(Collectors.toList());
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Criterion code already exists in the database", e);
        }
    }

    private FrameworkVersion getVersion(CreateFrameworkCriteriaCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private void validateRequest(CreateFrameworkCriteriaCommand command, FrameworkVersion version) {
        requireDraftVersionOfFramework(command, version);

        Set<String> requestCodes = new HashSet<>();
        Set<Integer> allOrders = frameworkCriterionRepository.findByFrameworkVersionId(command.versionId())
                .stream().map(fc ->fc.getOrder()).collect(Collectors.toCollection(HashSet::new));
        for (CreateFrameworkCriteriaCommand.CriterionItemCommand criterionCmd : command.criteria()) {
            requireUniqueValidCriterion(criterionCmd, requestCodes, allOrders);
        }
        requireOrdersContinuousFromOne(allOrders);

        if (frameworkCriterionRepository.existsByFrameworkVersionIdAndCodeIn(command.versionId(), requestCodes))
            throw new IllegalArgumentException("Criterion code already exists");
    }

    private void requireDraftVersionOfFramework(CreateFrameworkCriteriaCommand command, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Criteria can only be added while the version is in DRAFT status");
    }

    private void requireUniqueValidCriterion(CreateFrameworkCriteriaCommand.CriterionItemCommand criterionCmd,
                                             Set<String> requestCodes, Set<Integer> allOrders) {
        String safeCode = StringNormalization.normalizeCode(criterionCmd.code());
        if (!FrameworkCriterionCode.ALLOWED_CODES.contains(safeCode)) {
            throw new IllegalArgumentException("Invalid criterion code: " + safeCode);
        }
        if (!requestCodes.add(safeCode)) {
            throw new IllegalArgumentException("The submitted data repeats the criterion code: " + safeCode);
        }
        if (!allOrders.add(criterionCmd.order())) {
            throw new IllegalArgumentException("Duplicate criterion order: " + criterionCmd.order());
        }
    }

    private void requireOrdersContinuousFromOne(Set<Integer> allOrders) {
        for (int i = 1; i <= allOrders.size(); i++) {
            if (!allOrders.contains(i)) {
                throw new IllegalArgumentException("Criterion order must increase continuously from 1 without skipping a number");
            }
        }
    }
}
