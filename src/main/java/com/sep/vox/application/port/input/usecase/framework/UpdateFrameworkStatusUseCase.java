package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkActiveStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkStatusUseCase implements IUseCase<UpdateFrameworkActiveStatusCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkStatusUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkActiveStatusCommand input) {
        Framework framework = getFramework(input);
        validateRequest(input);
        updateFrameworkStatus(input, framework);
        return frameworkRepository.save(framework).getId();
    }

    private Framework getFramework(UpdateFrameworkActiveStatusCommand input) {
        return frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
    }

    private void validateRequest(UpdateFrameworkActiveStatusCommand input) {
        if (input.isActive()
                && frameworkVersionRepository.findByFrameworkIdAndStatus(input.frameworkId(), FrameworkVersionStatus.PUBLISHED).isEmpty()) {
            throw new IllegalStateException("Framework phải có ít nhất một phiên bản đã xuất bản trước khi kích hoạt");
        }
    }

    private void updateFrameworkStatus(UpdateFrameworkActiveStatusCommand input, Framework framework) {
        framework.setActive(input.isActive());
        framework.setUpdatedAt(Instant.now());
        framework.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }
}
