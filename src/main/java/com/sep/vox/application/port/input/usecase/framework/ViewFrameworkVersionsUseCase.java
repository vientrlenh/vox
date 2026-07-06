package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class ViewFrameworkVersionsUseCase implements IUseCase<ViewFrameworkVersionsQuery, PageResult<FrameworkVersionDto>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewFrameworkVersionsUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FrameworkVersionDto> execute(ViewFrameworkVersionsQuery input) {
        frameworkRepository.findFrameworkById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var result = frameworkVersionRepository.findByFrameworkId(
            input.frameworkId(), input.page(), input.size());
        return FrameworkVersionDtoMapper.toDtoPage(result);
    }
}
