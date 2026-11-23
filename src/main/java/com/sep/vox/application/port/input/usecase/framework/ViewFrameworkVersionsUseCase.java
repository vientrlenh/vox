package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class ViewFrameworkVersionsUseCase implements IUseCase<ViewFrameworkVersionsQuery, PageResult<FrameworkVersionDto>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    public PageResult<FrameworkVersionDto> execute(ViewFrameworkVersionsQuery input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        PageResult<FrameworkVersion> result = input.status() != null
            ? frameworkVersionRepository.findByFrameworkIdAndStatus(
                input.frameworkId(), input.status(), input.page(), input.size())
            : frameworkVersionRepository.findByFrameworkId(input.frameworkId(), input.page(), input.size());
        return FrameworkVersionDtoMapper.toDtoPage(result);
    }
}
