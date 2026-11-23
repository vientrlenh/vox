package com.sep.vox.application.port.input.usecase.framework;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;

@Service
@RequiredArgsConstructor
public class UpdateFrameworkUseCase implements IUseCase<UpdateFrameworkCommand, UUID> {

    private final FrameworkRepository frameworkRepository;

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCommand input) {
        Framework framework = getFramework(input);
        updateFramework(input, framework);
        return frameworkRepository.save(framework).getId();
    }

    private Framework getFramework(UpdateFrameworkCommand input) {
        return frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
    }

    private void updateFramework(UpdateFrameworkCommand input, Framework framework) {
        framework.setName(StringNormalization.trimAndCollapseSpaces(input.name()));
        framework.setDescription(StringNormalization.trimAndCollapseSpaces(input.description()));
    }
}
