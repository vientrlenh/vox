package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;

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
        Framework framework = getFramework(input);
        updateFramework(input, framework);
        return frameworkRepository.saveFramework(framework).getId();
    }

    private Framework getFramework(UpdateFrameworkCommand input) {
        return frameworkRepository.findFrameworkById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
    }

    private void updateFramework(UpdateFrameworkCommand input, Framework framework) {
        framework.setName(StringNormalization.trimAndCollapseSpaces(input.name()));
        framework.setDescription(StringNormalization.trimAndCollapseSpaces(input.description()));
        framework.setUpdatedAt(OffsetDateTime.now());
        framework.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }
}
