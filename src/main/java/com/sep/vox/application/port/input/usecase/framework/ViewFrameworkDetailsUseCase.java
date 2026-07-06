package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.mapper.FrameworkDtoMapper;
import com.sep.vox.domain.repository.FrameworkRepository;

@Service
public class ViewFrameworkDetailsUseCase implements IUseCase<ViewFrameworkDetailsQuery, FrameworkDto> {

    private final FrameworkRepository frameworkRepository;

    public ViewFrameworkDetailsUseCase(FrameworkRepository frameworkRepository) {
        this.frameworkRepository = frameworkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FrameworkDto execute(ViewFrameworkDetailsQuery input) {
        var framework = frameworkRepository.findFrameworkById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
        return FrameworkDtoMapper.toDto(framework);
    }
}
