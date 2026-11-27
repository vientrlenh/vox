package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkVersionUseCase implements IUseCase<UpdateFrameworkVersionCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionCommand input) {
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        frameworkVersionRepository.save(updateVersion(input, version));

        return input.versionId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkVersionCommand input) {
        return frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework version not found"));
    }

    private FrameworkVersion updateVersion(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        return version.withEditableFields(
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            input.descriptionProvided()
                ? StringNormalization.trimAndCollapseSpaces(input.description())
                : version.getDescription(),
            input.effectiveFrom(),
            effectiveTo(input, version));
    }

    private void checkValidRequest(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(input.frameworkId()))
            throw new IllegalArgumentException("The version does not belong to this framework");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Only a framework version in DRAFT status can be updated");

        Instant effectiveTo = effectiveTo(input, version);
        if (effectiveTo != null && !effectiveTo.isAfter(input.effectiveFrom()))
            throw new IllegalArgumentException("The expiry date must be after the effective date");
    }

    private Instant effectiveTo(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        return input.effectiveToProvided() ? input.effectiveTo() : version.getEffectiveTo();
    }
}
