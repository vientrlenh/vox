package com.sep.vox.application.port.input.usecase.framework;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.DeleteFrameworkResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;

@Service
@RequiredArgsConstructor
public class DeleteFrameworkResultBandUseCase
        implements IUseCase<DeleteFrameworkResultBandCommand, Void> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final GradeLevelBandScopeRepository gradeLevelBandScopeRepository;

    @Override
    @Transactional
    public Void execute(DeleteFrameworkResultBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkResultBand band = getBand(command);

        checkValidRequest(command, version, band);

        frameworkResultBandRepository.deleteById(command.bandId());
        return null;
    }

    private FrameworkVersion getVersion(DeleteFrameworkResultBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkResultBand getBand(DeleteFrameworkResultBandCommand command) {
        return frameworkResultBandRepository.findById(command.bandId())
            .orElseThrow(() -> new ResourceNotFoundException("Result band not found"));
    }

    private void checkValidRequest(DeleteFrameworkResultBandCommand command, FrameworkVersion version, FrameworkResultBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Only a version in DRAFT status can be edited");

        if (!band.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("The result band does not belong to this version");

        if (frameworkCriterionBandRepository.existsByFrameworkResultBandId(command.bandId()))
            throw new IllegalArgumentException("Cannot delete a result band that a criterion is using");

        // V42 adds a foreign key from grade_level_band_scopes to this table. The check lives in the application layer
        // so the user gets a readable message instead of Postgres' raw constraint error.
        if (gradeLevelBandScopeRepository.existsByBandId(command.bandId()))
            throw new IllegalArgumentException("Cannot delete a result band that is used as the tier ceiling of a grade level");
    }
}
