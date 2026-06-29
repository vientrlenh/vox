package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteFrameworkCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class DeleteFrameworkUseCase implements IUseCase<DeleteFrameworkCommand, Void> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public DeleteFrameworkUseCase(FrameworkRepository frameworkRepository, FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteFrameworkCommand input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        if (frameworkVersionRepository.existsByFrameworkId(input.frameworkId())) {
            throw new IllegalStateException("Không thể xóa framework đang có phiên bản");
        }

        frameworkRepository.deleteById(input.frameworkId());
        return null;
    }
}
