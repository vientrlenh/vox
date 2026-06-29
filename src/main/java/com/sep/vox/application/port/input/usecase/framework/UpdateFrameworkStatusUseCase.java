package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkActiveStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;

@Service
public class UpdateFrameworkStatusUseCase implements IUseCase<UpdateFrameworkActiveStatusCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkStatusUseCase(FrameworkRepository frameworkRepository, UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkActiveStatusCommand input) {
        var framework = frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
        applyChanges(input, framework);
        return frameworkRepository.save(framework).getId();
    }

    private void applyChanges(UpdateFrameworkActiveStatusCommand input, Framework framework) {
        framework.setActive(input.isActive());
        framework.setUpdatedAt(OffsetDateTime.now());
        framework.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }
}
