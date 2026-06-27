package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

@Service
public class CreateFrameworkUseCase implements IUseCase<CreateFrameworkCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkUseCase(FrameworkRepository frameworkRepository, UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateFrameworkCommand input) {
        var code = StringNormalization.normalizeCode(input.code());
        var name = StringNormalization.trimAndCollapseSpaces(input.name());
        var description = StringNormalization.trimAndCollapseSpaces(input.description());

        frameworkRepository.findByCode(code)
            .ifPresent(f -> { throw new DuplicatedException("Mã framework đã tồn tại"); });

        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var framework = new Framework(new FrameworkCode(code), name, description, true, now, now, userId, userId);
        return frameworkRepository.save(framework).getId();
    }
}
