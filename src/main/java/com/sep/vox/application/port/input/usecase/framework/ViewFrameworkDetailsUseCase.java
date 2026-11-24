package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.Framework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.framework.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.mapper.FrameworkDtoMapper;
import com.sep.vox.domain.repository.FrameworkRepository;

@Service
@RequiredArgsConstructor
public class ViewFrameworkDetailsUseCase implements IUseCase<ViewFrameworkDetailsQuery, FrameworkDto> {

    private final FrameworkRepository frameworkRepository;

    @Override
    @Transactional(readOnly = true)
    public FrameworkDto execute(ViewFrameworkDetailsQuery input) {
        Framework framework = frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy framework"));
        return FrameworkDtoMapper.toDto(framework);
    }
}
