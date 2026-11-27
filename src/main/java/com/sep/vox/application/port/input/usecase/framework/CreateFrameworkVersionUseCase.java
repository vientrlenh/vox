package com.sep.vox.application.port.input.usecase.framework;


import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.framework.CreateFrameworkVersionResponseMapper;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class CreateFrameworkVersionUseCase implements IUseCase<CreateFrameworkVersionCommand, CreateFrameworkVersionResponse> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    @Transactional
    public CreateFrameworkVersionResponse execute(CreateFrameworkVersionCommand input) {
        CreateFrameworkVersionCommand command = normalize(input);
        if (command.effectiveTo() != null && !command.effectiveTo().isAfter(command.effectiveFrom())) {
            throw new IllegalArgumentException("The expiry date must be after the effective date");
        }

        frameworkRepository.findById(command.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework not found"));

        FrameworkVersion version = FrameworkVersion.draft(
            command.frameworkId(),
            command.code(),
            command.name(),
            command.description(),
            command.version(),
            command.effectiveFrom(),
            command.effectiveTo());

        try {
            FrameworkVersion saved = frameworkVersionRepository.save(version);
            return CreateFrameworkVersionResponseMapper.toResponse(saved.getId());
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("The version or version code already exists in the framework");
        }
    }

    private CreateFrameworkVersionCommand normalize(CreateFrameworkVersionCommand input) {
        return new CreateFrameworkVersionCommand(
            input.frameworkId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.version(),
            input.effectiveFrom(),
            input.effectiveTo()
        );
    }
}
