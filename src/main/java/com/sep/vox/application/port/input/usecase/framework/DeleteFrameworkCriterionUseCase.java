package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.DeleteFrameworkCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class DeleteFrameworkCriterionUseCase
        implements IUseCase<DeleteFrameworkCriterionCommand, Void> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    @Override
    @Transactional
    public Void execute(DeleteFrameworkCriterionCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        checkValidRequest(command, version, criterion);

        // delete child bands explicitly before the criterion to avoid an FK violation.
        frameworkCriterionBandRepository.deleteByFrameworkCriterionIdIn(List.of(command.criterionId()));
        frameworkCriterionRepository.deleteById(command.criterionId());
        return null;
    }

    private FrameworkVersion getVersion(DeleteFrameworkCriterionCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkCriterion getCriterion(DeleteFrameworkCriterionCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
    }

    private void checkValidRequest(DeleteFrameworkCriterionCommand command, FrameworkVersion version,
            FrameworkCriterion criterion) {
        if (!version.getFrameworkId().equals(command.frameworkId())) {
            throw new IllegalArgumentException("The version does not belong to this framework");
        }
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be deleted while the version is in DRAFT status");
        }
        if (!criterion.getFrameworkVersionId().equals(command.versionId())) {
            throw new IllegalArgumentException("The criterion does not belong to this version");
        }
    }
}
