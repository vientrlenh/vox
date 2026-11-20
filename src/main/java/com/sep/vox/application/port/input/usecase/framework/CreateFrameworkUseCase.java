package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

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
        String code = StringNormalization.normalizeCode(input.code());
        String name = StringNormalization.trimAndCollapseSpaces(input.name());
        String description = StringNormalization.trimAndCollapseSpaces(input.description());
        Instant now = Instant.now();
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        Framework framework = new Framework(
            new FrameworkCode(code), 
            name, 
            description, 
            false,
            now, now, 
            userId, userId);
        
        try {
            return frameworkRepository.save(framework).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Mã framework đã tồn tại");
        }
    }
}
