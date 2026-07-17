package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.mapper.FrameworkDtoMapper;
import com.sep.vox.domain.repository.FrameworkRepository;

@Service
public class ViewFrameworksUseCase implements IUseCase<ViewFrameworksQuery, PageResult<FrameworkDto>> {

    private final FrameworkRepository frameworkRepository;

    public ViewFrameworksUseCase(FrameworkRepository frameworkRepository) {
        this.frameworkRepository = frameworkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FrameworkDto> execute(ViewFrameworksQuery input) {
        var result = frameworkRepository.findAll(input.page(), input.size(), input.search(), input.isActive());
        return FrameworkDtoMapper.toDtoPage(result);
    }
}
