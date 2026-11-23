package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class ViewPublishedFrameworkVersionsUseCase implements IUseCase<ViewFrameworkVersionsQuery, PageResult<FrameworkVersionDto>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    public PageResult<FrameworkVersionDto> execute(ViewFrameworkVersionsQuery input) {
        Framework framework = frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy framework"));
        if (!framework.isActive()) {
            throw new ResourceNotFoundException("Không tìm thấy framework");
        }

        PageResult<FrameworkVersion> result = frameworkVersionRepository.findByFrameworkIdAndStatus(
            input.frameworkId(), FrameworkVersionStatus.PUBLISHED, input.page(), input.size());
        return FrameworkVersionDtoMapper.toDtoPage(result);
    }
}
