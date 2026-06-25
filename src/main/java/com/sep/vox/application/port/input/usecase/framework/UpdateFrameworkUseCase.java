package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

@Service
public class UpdateFrameworkUseCase implements IUseCase<UpdateFrameworkCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkUseCase(FrameworkRepository frameworkRepository, UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCommand input) {
        var framework = frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var code = StringNormalization.normalizeCode(input.code());
        var name = StringNormalization.trimAndCollapseSpaces(input.name());
        var description = StringNormalization.trimAndCollapseSpaces(input.description());

        frameworkRepository.findByCode(code)
            .filter(f -> !f.getId().equals(input.frameworkId()))
            .ifPresent(f -> { throw new DuplicatedException("Mã framework đã tồn tại"); });

        framework.setCode(new FrameworkCode(code));
        framework.setName(name);
        framework.setDescription(description);
        framework.setUpdatedAt(OffsetDateTime.now());
        framework.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());

        try {
            return frameworkRepository.save(framework).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Mã framework đã tồn tại");
        }
    }
}
