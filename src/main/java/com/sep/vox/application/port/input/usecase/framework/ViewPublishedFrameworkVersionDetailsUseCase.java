package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class ViewPublishedFrameworkVersionDetailsUseCase implements IUseCase<ViewFrameworkVersionDetailsQuery, FrameworkVersionDto> {

    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewPublishedFrameworkVersionDetailsUseCase(FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FrameworkVersionDto execute(ViewFrameworkVersionDetailsQuery input) {
        var version = frameworkVersionRepository.findFrameworkVersionById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));
        if (version.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new NotFoundException("Không tìm thấy phiên bản framework");
        }
        return FrameworkVersionDtoMapper.toDto(version);
    }
}
