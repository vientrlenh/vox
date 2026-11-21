package com.sep.vox.application.port.input.usecase.framework;


import com.sep.vox.application.exception.DataConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.framework.CreateFrameworkVersionResponseMapper;
import com.sep.vox.application.port.input.command.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkVersionUseCase implements IUseCase<CreateFrameworkVersionCommand, CreateFrameworkVersionResponse> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public CreateFrameworkVersionUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional
    public CreateFrameworkVersionResponse execute(CreateFrameworkVersionCommand input) {
        CreateFrameworkVersionCommand command = normalize(input);
        if (command.effectiveTo() != null && !command.effectiveTo().isAfter(command.effectiveFrom())) {
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày hiệu lực");
        }

        frameworkRepository.findById(command.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        FrameworkVersion version = FrameworkVersion.builder()
            .frameworkId(command.frameworkId())
            .code(command.code())
            .name(command.name())
            .description(command.description())
            .version(command.version())
            .effectiveFrom(command.effectiveFrom())
            .effectiveTo(command.effectiveTo())
            .status(FrameworkVersionStatus.DRAFT)
            .build();

        try {
            FrameworkVersion saved = frameworkVersionRepository.save(version);
            return CreateFrameworkVersionResponseMapper.toResponse(saved.getId());
        } catch (DataConflictException e) {
            throw new DuplicatedException("Phiên bản hoặc mã phiên bản này đã tồn tại trong framework");
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
