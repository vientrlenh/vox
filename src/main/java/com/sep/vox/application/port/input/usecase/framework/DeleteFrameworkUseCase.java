package com.sep.vox.application.port.input.usecase.framework;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.DeleteFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class DeleteFrameworkUseCase implements IUseCase<DeleteFrameworkCommand, Void> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    @Transactional
    public Void execute(DeleteFrameworkCommand input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework not found"));

        if (frameworkVersionRepository.existsByFrameworkId(input.frameworkId())) {
            throw new IllegalArgumentException("Cannot delete a framework that has versions");
        }

        frameworkRepository.deleteById(input.frameworkId());
        return null;
    }
}
