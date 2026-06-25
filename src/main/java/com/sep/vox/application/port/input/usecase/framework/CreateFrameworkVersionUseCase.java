package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.framework.CreateFrameworkVersionResponseMapper;
import com.sep.vox.application.port.input.command.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkVersionUseCase implements IUseCase<CreateFrameworkVersionCommand, CreateFrameworkVersionResponse> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkVersionUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateFrameworkVersionResponse execute(CreateFrameworkVersionCommand input) {
        var command = normalize(input);
        if (command.effectiveTo() != null && command.effectiveTo().isBefore(command.effectiveFrom())) {
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày hiệu lực");
        }
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        frameworkRepository.findById(command.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        frameworkVersionRepository.findByFrameworkIdAndVersion(command.frameworkId(), command.version())
            .ifPresent(v -> { throw new DuplicatedException("Phiên bản này đã tồn tại trong framework"); });

        var version = new FrameworkVersion(
            command.frameworkId(),
            command.code(),
            command.name(),
            command.description(),
            command.version(),
            command.effectiveFrom(),
            command.effectiveTo(),
            FrameworkVersionStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        );

        var saved = frameworkVersionRepository.save(version);
        return CreateFrameworkVersionResponseMapper.toResponse(saved.getId());
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
